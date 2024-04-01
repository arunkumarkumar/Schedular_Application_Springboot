package com.teleapps.schedulemate.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teleapps.schedulemate.domain.Report;
import com.teleapps.schedulemate.repository.ReportRepository;
import com.teleapps.schedulemate.util.Constants;
import com.teleapps.schedulemate.util.DataBaseUtility;
import com.teleapps.schedulemate.util.FileOperation;
import com.teleapps.schedulemate.util.StackTrace;

public class SyncTable {

	private static final Logger logger = LogManager.getLogger(SyncTable.class);

	public void init(JsonObject jobDetails, ReportRepository reportRepository) {
		List<String> logMessages = new ArrayList<>();
		Report report = new Report();
		report.setResult(Constants.NA);
		Date startTime = new Date();
		try {
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
				logMessages.add("Job is configured as in-active. Exiting the operation");
				logger.warn(logMessages.get(logMessages.size()-1));
				report.setResult("");
				return;
			}
			String inputFileName = jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get("sourceFileName").getAsString();
			logMessages.add(String.format("Searching for the %s in %s", inputFileName, jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get(Constants.SOURCE_DIRECTORY).getAsString()));
			logger.info(logMessages.get(logMessages.size()-1));
			File sourceDirectory = new File(jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get(Constants.SOURCE_DIRECTORY).getAsString());
			if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
				logMessages.add(String.format("Unable to find the source directory. Exiting the execution"));
				logger.error(logMessages.get(logMessages.size()-1));
				return;
			}
			List<String> inputFiles = new FileOperation().findFilesWithKeyword(sourceDirectory, inputFileName);
			if(inputFiles.isEmpty()) {
				logMessages.add(String.format("No files found in %s directory", sourceDirectory));
				logger.info(logMessages.get(logMessages.size()-1));
				return;
			}
			String listOfFiles = String.join(", ", inputFiles);
			logMessages.add(String.format("%s files found in %s. and the files are %s", inputFiles.size(),  inputFileName, listOfFiles));
			logger.info(logMessages.get(logMessages.size()-1));
			for(int filesCount = 0; filesCount < inputFiles.size(); filesCount++) {
				JsonObject actionParameters = jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject();
				String currentFile = actionParameters.get(Constants.SOURCE_DIRECTORY).getAsString().concat(inputFiles.get(filesCount));
				List<JsonObject> recordsList = new FileOperation().readFlatFilesAsJsonArray(currentFile);
				if (recordsList.isEmpty()) {
					logMessages.add(String.format("The source file %s does not contain any records to proceed with", inputFiles.get(filesCount)));
					logger.warn(logMessages.get(logMessages.size()-1));
					report.setResult(Constants.NA);
				} else {
					logMessages.add(String.format("%s records obtained from %s", recordsList.size(), inputFiles.get(filesCount)));
					logger.info(logMessages.get(logMessages.size()-1));
					SortedMap<String, List<String>> tableMetaData = new DataBaseUtility().getTableDetails(actionParameters.get("connectionString").getAsString(),
							actionParameters.get(Constants.CREDENTIALS).getAsJsonObject().get(Constants.USER_NAME).getAsString(),
							actionParameters.get(Constants.CREDENTIALS).getAsJsonObject().get(Constants.PASSWORD).getAsString(),
							actionParameters.get("tableName").getAsString());
					if(tableMetaData.isEmpty()) {
						logMessages.add(String.format("Unable to retive the meta data of the table. Exiting the execution"));
						logger.warn(logMessages.get(logMessages.size()-1));
						report.setResult(Constants.FAILURE);
						return;
					}
					String query = new DataBaseUtility().buildInsertQuery(tableMetaData, actionParameters.get("tableName").getAsString());
					logger.info("Query build {}", query);
					if(!new DataBaseUtility().insertRecordsToTable(actionParameters.get("connectionString").getAsString(),
							actionParameters.get(Constants.CREDENTIALS).getAsJsonObject().get(Constants.USER_NAME).getAsString(),
							actionParameters.get(Constants.CREDENTIALS).getAsJsonObject().get(Constants.PASSWORD).getAsString(), query, recordsList, tableMetaData)) {
						logMessages.add(String.format("Unable to sync the records to table. Will try on next run"));
						logger.error(logMessages.get(logMessages.size()-1));
						report.setResult(Constants.FAILURE);
						return;
					}
					logMessages.add(String.format("All records are synced. Deleting the processed file"));
					logger.info(logMessages.get(logMessages.size()-1));
					if(deleteTheProcessedFile(currentFile)) {
						logMessages.add(String.format("Source file %s is deleted permanently", currentFile));
						logger.info(logMessages.get(logMessages.size()-1));
						report.setResult(Constants.SUCCESS);
					} else {
						logMessages.add(String.format("Unable to delete the processed file %s", currentFile));
						logger.error(logMessages.get(logMessages.size()-1));
					}
				}
			}
		}  catch (Exception ex) {
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
	
	private boolean deleteTheProcessedFile(String processedFile) {
		try {
			Files.deleteIfExists(Paths.get(processedFile));
			return true;
		} catch (IOException ex) {
			logger.error("Exception while deleting the source file: {}", ex.getMessage());
			return false;
		}
	}
}