package com.teleapps.schedulemate.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teleapps.schedulemate.domain.Report;
import com.teleapps.schedulemate.repository.ReportRepository;
import com.teleapps.schedulemate.util.Constants;
import com.teleapps.schedulemate.util.StackTrace;


public class SyncGrammar {
	   
	private static final Logger logger = LogManager.getLogger(SyncGrammar.class);
	
	
	public void init(JsonObject jobDetails, ReportRepository reportRepository) {
		
		List<String> logMessages = new ArrayList<>();
		Report report = new Report();
		report.setResult(Constants.NA);
		Date startTime = new Date();
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DAY_OF_YEAR, -1);
			report.setJobExecutedTime(new SimpleDateFormat(Constants.DATE_FORMAT_WITH_TIME).format(startTime));
			logMessages.add(String.format("Job [%s] started on %s", jobDetails.get(Constants.JOB_NAME).getAsString(), new SimpleDateFormat(Constants.DATE_FORMAT_WITH_TIME).format(startTime)));
			logger.info(logMessages.get(logMessages.size()-1));
			report.setJobName(jobDetails.get(Constants.JOB_NAME).getAsString());
			report.setJobDescription(jobDetails.get(Constants.JOB_DESCRIPTION).getAsString());
			report.setCronExpression(jobDetails.get(Constants.CRON_EXPRESSION).getAsString());
			report.setCronDescription(jobDetails.get(Constants.CRON_DESCRIPTION).getAsString());
			report.setIsActive(jobDetails.get(Constants.IS_ACTIVE).getAsString());
			report.setAction(jobDetails.get(Constants.ACTION).getAsString());
			if (!jobDetails.get(Constants.IS_ACTIVE).getAsBoolean()) {
				logMessages.add(String.format("Job is configured as in-active. Exiting the operation"));
				logger.warn(logMessages.get(logMessages.size()-1));
				report.setResult("");
				return;
			}
			String placeholder = new SimpleDateFormat("yyyy_MM_dd").format(calendar.getTime());
			String inputFileName = jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get("inputFileName").getAsString().replace("*", placeholder);
			logMessages.add(String.format("Searching for the file %s%s", jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get(Constants.SOURCE_DIRECTORY).getAsString(), inputFileName));
			logger.info(logMessages.get(logMessages.size()-1));
			File sourceFile = new File(jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get(Constants.SOURCE_DIRECTORY).getAsString().concat(inputFileName));
			if (!sourceFile.exists() || sourceFile.isDirectory()) {
				logMessages.add(String.format("Unable to find the source file. Exiting the execution"));
				logger.error(logMessages.get(logMessages.size()-1));
				report.setResult(Constants.FAILURE);
				return;
			}
			List<String> userList = processExcelFile(jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get(Constants.SOURCE_DIRECTORY).getAsString().concat(inputFileName), jobDetails);
			if (userList.isEmpty()) {
				logMessages.add(String.format("The source file does not contain any records to proceed with"));
				logger.warn(logMessages.get(logMessages.size()-1));
				report.setResult(Constants.FAILURE);
				return;
			}
			logMessages.add(String.format("Total records obtained from the source file %s", userList.size()));
			logger.info(logMessages.get(logMessages.size()-1));
			userList.removeIf(userId -> !userId.matches("[a-zA-Z0-9]+"));
			logMessages.add(String.format("Total number of records remaining after removing non-alphanumeric records are %s", userList.size()));
			logger.info(logMessages.get(logMessages.size()-1));
			File grxmlTemplate = new File(jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get(Constants.GRXML_TEMPLATE_NAME).getAsString());
			if (!grxmlTemplate.exists() || grxmlTemplate.isDirectory()) {
				logMessages.add(String.format("GRXML template not found [%s]", jobDetails.get(Constants.GRXML_TEMPLATE_NAME).getAsString()));
				logger.warn(logMessages.get(logMessages.size()-1));
				report.setResult(Constants.FAILURE);
				return;
			}
			String grammar = buildGrxml(grxmlTemplate, userList, jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get("ignoreTitle").getAsString());
			if (grammar != null && grammar.trim().length() > 0) {
				String outputFileName = jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get("outputFileName").getAsString().replace("*", placeholder);
				if (createGrxmlFile(grammar, jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get(Constants.DESTINATION_DIRECTORY).getAsString(), outputFileName)) {
					logMessages.add(String.format("GRXML file created successfully"));
					logger.info(logMessages.get(logMessages.size()-1));
					if (moveSourceFile(sourceFile.getCanonicalPath(), jobDetails.get(Constants.ACTION_PARAMETERS) .getAsJsonObject().get(Constants.SOURCE_DIRECTORY).getAsString().concat("Processed"))) {
						logMessages.add(String.format("Source file moved to processed directory"));
						logger.info(logMessages.get(logMessages.size()-1));
						report.setResult(Constants.SUCCESS);
					}
				} else {
					logMessages.add(String.format("Unable to create GRXML file"));
					logger.info(logMessages.get(logMessages.size()-1));
					
					report.setResult(Constants.FAILURE);
				}
			} else {
				logMessages.add(String.format("Grammer is null or empty"));
				logger.error(logMessages.get(logMessages.size()-1));
			}
		} catch (Exception ex) {
			report.setResult(Constants.FAILURE);
			logMessages.add(String.format("Exception while executing the job [%s] %s", jobDetails.get(Constants.JOB_NAME).getAsString(), StackTrace.getMessage(ex)));
			logger.error(logMessages.get(logMessages.size()-1));
		} finally {
			Date endTime =  new Date();
			report.setJobShutdownTime(new SimpleDateFormat(Constants.DATE_FORMAT_WITH_TIME).format(endTime));
			report.setCreatedOn(new SimpleDateFormat(Constants.DATE_FORMAT_WITH_TIME).format(endTime));
			report.setDuration(String.valueOf(endTime.getTime() - startTime.getTime()));
			logMessages.add(String.format("Job [%s] ended on %s", jobDetails.get(Constants.JOB_NAME).getAsString(), new SimpleDateFormat(Constants.DATE_FORMAT_WITH_TIME).format(endTime)));
			logger.info(logMessages.get(logMessages.size()-1));
			JsonArray messages = new JsonArray();
			logMessages.stream().forEach(messages::add);
			report.setMessage(messages.toString());
			if("TRUE".equalsIgnoreCase(report.getIsActive())) {
				reportRepository.save(report);
			}
		}
	}

	private List<String> processExcelFile(String inputFileName, JsonObject jobDetails) {
		List<String> userList = new ArrayList<>();
		try (InputStream inputStream = new FileInputStream(inputFileName);
				Workbook workbook = WorkbookFactory.create(inputStream)) {
			Sheet sheet = workbook.getSheet(jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get("sheetName").getAsString());
			for (Row row : sheet) {
				Cell cell = row.getCell(0);
				if (cell != null) {
					userList.add(cell.getStringCellValue());
				}
			}
			return userList;
		} catch (IOException ex) {
			logger.error("Unable to process the source excel file. " + StackTrace.getMessage(ex));
			return Collections.emptyList();
		}
	}

	private String buildGrxml(File template, List<String> userList, String ignoreCaption) {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(template);
			NodeList oneOfNodes = document.getElementsByTagName("one-of");
			Node oneOfNode = oneOfNodes.item(0);
			int skipNoOfRow = Constants.YES.equalsIgnoreCase(ignoreCaption) ? 1 : 0;
			userList.stream().skip(skipNoOfRow).forEach(userId -> {
				Element mainItem = document.createElement("item");
				userId.chars().forEach(character -> {
					Element innerItem = document.createElement("item");
					innerItem.setTextContent(String.valueOf((char) character));
					mainItem.appendChild(innerItem);
				});
				Element tag = document.createElement("tag");
				tag.setTextContent("out = \"*\";".replace("*", userId));
				mainItem.appendChild(tag);
				oneOfNode.appendChild(mainItem);
			});
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(new StringWriter());
			transformer.transform(source, result);
			return result.getWriter().toString();
		} catch (ParserConfigurationException | SAXException | IOException | TransformerException ex) {
			logger.error("Exception while generating grammar {}", StackTrace.getMessage(ex));
			return null;
		}
	}

	private boolean createGrxmlFile(String grammar, String destinationDirectory, String fileName) {
	    try {
	        if (grammar == null || destinationDirectory == null || fileName == null) {
	            logger.error("Invalid data to create GRXML file");
	        	return false;
	        }

	        Path destinationPath = Paths.get(destinationDirectory.concat(fileName));
	        if (Files.notExists(destinationPath.getParent(), LinkOption.NOFOLLOW_LINKS)) {
	            logger.warn("The destination directory {} is not found, and an automated attempt is being made to create it", destinationDirectory);
	            if (!new File(destinationDirectory).mkdirs()) {
	                logger.error("An attempt to create the destination directory {} has failed", destinationDirectory);
	                return false;
	            }
	        }

	        byte[] bytes = grammar.getBytes(StandardCharsets.UTF_8);
	        Files.write(destinationPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	        return true;
	    } catch (IOException ex) {
	        logger.fatal("Exception while creating GRXML file {}", StackTrace.getMessage(ex));
	        return false;
	    }
	}

	private boolean moveSourceFile(String source, String destination) {
		try {
			Path sourceFile = Paths.get(source);
			Path targetDirectory = Paths.get(destination);
			if (!Files.exists(targetDirectory)) {
				Files.createDirectories(targetDirectory);
			}
			Files.move(sourceFile, targetDirectory.resolve(sourceFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException ex) {
			logger.error("Exception while moving the source file: {}", ex.getMessage());
			return false;
		}
	}
}