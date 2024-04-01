package com.teleapps.schedulemate.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teleapps.schedulemate.domain.Report;
import com.teleapps.schedulemate.repository.ReportRepository;
import com.teleapps.schedulemate.util.Constants;
import com.teleapps.schedulemate.util.ExpressionResolver;
import com.teleapps.schedulemate.util.FileOperation;
import com.teleapps.schedulemate.util.StackTrace;

public class SyncEndpoint {

	private static final Logger logger = LogManager.getLogger(SyncEndpoint.class);

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
			String listOfFiles = String.join(",", inputFiles);
			logMessages.add(String.format("%s files found %s", listOfFiles,  inputFileName));
			logger.info(logMessages.get(logMessages.size()-1));
			for(int filesCount = 0; filesCount < inputFiles.size(); filesCount++) {
				String currentFile = jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get(Constants.SOURCE_DIRECTORY).getAsString().concat(inputFiles.get(filesCount));
				List<JsonObject> recordsList = new FileOperation().readFlatFilesAsJsonArray(currentFile);
				if (recordsList.isEmpty()) {
					logMessages.add(String.format("The source file %s does not contain any records to proceed with", inputFiles.get(filesCount)));
					logger.warn(logMessages.get(logMessages.size()-1));
					report.setResult(Constants.NA);
				} else {
					logMessages.add(String.format("%s records obtained from %s", recordsList.size(), inputFiles.get(filesCount)));
					logger.info(logMessages.get(logMessages.size()-1));
					List<JsonObject> failedRecords = new ArrayList<>();
					for (int recordsCount = 0; recordsCount < recordsList.size(); recordsCount++) {
						if (!sendRequest(jobDetails, recordsList.get(recordsCount))) {
							failedRecords.add(recordsList.get(recordsCount));
						}
					}
					if(failedRecords.isEmpty()) {
						logMessages.add(String.format("All the records are synced."));
						logger.info(logMessages.get(logMessages.size()-1));
						if(deleteTheProcessedFile(currentFile)) {
							logMessages.add(String.format("Source file %s is deleted permanently", currentFile));
							logger.info(logMessages.get(logMessages.size()-1));
							report.setResult(Constants.SUCCESS);
						} else {
							logMessages.add(String.format("Unable to delete the processed file %s", currentFile));
							logger.error(logMessages.get(logMessages.size()-1));
						}
					} else {
						logMessages.add(String.format("Unable to sync all the records. %s records are failed", failedRecords.size()));
						logger.warn(logMessages.get(logMessages.size()-1));
						if(removeSyncedRecordsFromTheSourceFile(failedRecords, currentFile)) {
							logMessages.add(String.format("Removed synced records from the existing file"));
							logger.info(logMessages.get(logMessages.size()-1));
						} else {
							logMessages.add(String.format("Unable to update the failed records"));
							logger.error(logMessages.get(logMessages.size()-1));
						}
					}
				}
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

	private boolean sendRequest(JsonObject jobDetails, JsonObject requestBody) {
		CloseableHttpClient  httpclient = null;
		try {
			JsonObject apiDetails = jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject().get("api").getAsJsonObject();
			httpclient = apiDetails.get("url").getAsString().startsWith("https://") ? configureHttpClientWithSsl(httpclient, apiDetails.get("ssl").getAsJsonObject()) : configureHttpClient(httpclient);
			if (httpclient == null) {
				logger.error("Unable to create http client. Exiting the operation");
				return false;
			}
			CloseableHttpResponse response;
			if("POST".equalsIgnoreCase(apiDetails.get(Constants.METHOD).getAsString())) {
				response = httpclient.execute(makePost(apiDetails, requestBody));
			} else if("PUT".equalsIgnoreCase(apiDetails.get(Constants.METHOD).getAsString())) {
				response = httpclient.execute(makePut(apiDetails, requestBody));
			} else if("GET".equalsIgnoreCase(apiDetails.get(Constants.METHOD).getAsString())) {
				response = httpclient.execute(makeGet(apiDetails));
			} else {
				logger.error("Unsupported HTTP method {}", apiDetails.get(Constants.METHOD).getAsString());
				return false;
			}
			HttpEntity entity = response.getEntity();
			String responseString = EntityUtils.toString(entity);
			if((apiDetails.get("httpSuccessCode").getAsInt()) == response.getStatusLine().getStatusCode()) {
				logger.info("Response : {}" , responseString);
				return true;
			}else {
				logger.info("Response : {}",  responseString);
				return false;
			}
		} catch (Exception ex) {
			
			logger.error("API request failed @ {} for the requestbody {} \n {}", new SimpleDateFormat(Constants.DATE_FORMAT_WITH_TIME).format(new Date()), requestBody, StackTrace.getMessage(ex));
			return false;
		}
	}
	
	private HttpPost makePost(JsonObject apiDetails, JsonObject requestBody) {
		HttpPost httpPost = new HttpPost(apiDetails.get("url").getAsString());
		if(apiDetails.get(Constants.HEADERS).getAsJsonObject() != null) {
			for (String headers : apiDetails.get(Constants.HEADERS).getAsJsonObject().keySet()) {
				httpPost.setHeader(headers, new ExpressionResolver().resolve(apiDetails.get(Constants.HEADERS).getAsJsonObject().get(headers).getAsString()));
			}
		}
		httpPost.setEntity(new ByteArrayEntity(requestBody.toString().getBytes(StandardCharsets.UTF_8)));
		return httpPost;
	}
	
	private HttpPut makePut(JsonObject apiDetails, JsonObject requestBody) {
		HttpPut httpPut = new HttpPut(apiDetails.get("url").getAsString());
		if(apiDetails.get(Constants.HEADERS).getAsJsonObject() != null) {
			for (String headers : apiDetails.get(Constants.HEADERS).getAsJsonObject().keySet()) {
				httpPut.setHeader(headers, new ExpressionResolver().resolve(apiDetails.get(Constants.HEADERS).getAsJsonObject().get(headers).getAsString()));
			}
		}
		httpPut.setEntity(new ByteArrayEntity(requestBody.toString().getBytes(StandardCharsets.UTF_8)));
		return httpPut;
	}
	
	private HttpGet makeGet(JsonObject apiDetails) {
		HttpGet httpGet = new HttpGet(apiDetails.get("url").getAsString());
		if(apiDetails.get(Constants.HEADERS).getAsJsonObject() != null) {
			for (String headers : apiDetails.get(Constants.HEADERS).getAsJsonObject().keySet()) {
				httpGet.setHeader(headers, new ExpressionResolver().resolve(apiDetails.get(Constants.HEADERS).getAsJsonObject().get(headers).getAsString()));
			}
		}
		return httpGet;
	}

	private CloseableHttpClient configureHttpClientWithSsl(CloseableHttpClient httpClient, JsonObject ssl) {
		int timeOut = 10;
		try {
			RequestConfig config = RequestConfig.custom()
			  .setConnectTimeout(timeOut * 1000)
			  .setConnectionRequestTimeout(timeOut * 1000)
			  .setSocketTimeout(timeOut * 1000).build();
			SSLContextBuilder sslContextBuilder = SSLContexts.custom();
			File file = new File(ssl.get("keyStorePath").getAsString());
			sslContextBuilder = sslContextBuilder.loadTrustMaterial(file, ssl.get("password").getAsString().toCharArray());
			SSLContext sslcontext = sslContextBuilder.build();
			SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslcontext, new NoopHostnameVerifier());
			HttpClientBuilder clientbuilder = HttpClients.custom();
			clientbuilder = clientbuilder.setSSLSocketFactory(sslConSocFactory);
			httpClient = clientbuilder.setDefaultRequestConfig(config).build();
		} catch (KeyManagementException ex) {
			logger.error("KeyManagementException : " + StackTrace.getMessage(ex));
		} catch (NoSuchAlgorithmException ex) {
			logger.error("NoSuchAlgorithmException : " + StackTrace.getMessage(ex));
		} catch (KeyStoreException ex) {
			logger.error("KeyStoreException : " + StackTrace.getMessage(ex));
		} catch (CertificateException ex) {
			logger.error("CertificateException : " + StackTrace.getMessage(ex));
		} catch (IOException ex) {
			logger.error("IOException : " + StackTrace.getMessage(ex));
		} catch (Exception ex) {
			logger.error("Exception while creating http client with ssl : {} ", StackTrace.getMessage(ex));
		}
		return httpClient;
	}
	
	private CloseableHttpClient configureHttpClient(CloseableHttpClient httpClient) {
		int timeOut = 10;
		try {
			RequestConfig config = RequestConfig.custom()
			  .setConnectTimeout(timeOut * 1000)
			  .setConnectionRequestTimeout(timeOut * 1000)
			  .setSocketTimeout(timeOut * 1000).build();
			HttpClientBuilder clientbuilder = HttpClients.custom();
			httpClient = clientbuilder.setDefaultRequestConfig(config).build();
		} catch (Exception ex) {
			logger.error("Exception while configuring http client {} ",  StackTrace.getMessage(ex));
		}
		return httpClient;
	}
	
	private boolean removeSyncedRecordsFromTheSourceFile(List<JsonObject> failedRecords, String failedFilePath) {
        try {
        	List<String> jsonStrings = failedRecords.stream().map(new Gson()::toJson).collect(Collectors.toList());
            Files.write(Paths.get(failedFilePath), jsonStrings, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException ex) {
            logger.error("Exception while removing the synced records from the file {}\n{}", StackTrace.getMessage(ex), new Gson().toJson(failedRecords));
            return false;
        }
	}
	
	private boolean deleteTheProcessedFile(String processedFile) {
		try {
			Files.deleteIfExists(Paths.get(processedFile));
			return true;
		} catch (IOException ex) {
			logger.error("Exception while deleting the source file: {}", StackTrace.getMessage(ex));
			return false;
		}
	}
	
}