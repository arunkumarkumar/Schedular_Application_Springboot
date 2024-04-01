package com.teleapps.schedulemate.service;

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
import com.teleapps.schedulemate.util.StackTrace;

public class SyncDatabase {
	
	private static final Logger logger = LogManager.getLogger(SyncDatabase.class);
	
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
			JsonObject actionParameters = jobDetails.get(Constants.ACTION_PARAMETERS).getAsJsonObject();
			SortedMap<String, String> procedureMetaData = new DataBaseUtility().getStoredProcedureDetails(actionParameters.get("connectionString").getAsString(),
					actionParameters.get(Constants.CREDENTIALS).getAsJsonObject().get(Constants.USER_NAME).getAsString(),
					actionParameters.get(Constants.CREDENTIALS).getAsJsonObject().get(Constants.PASSWORD).getAsString(),
					actionParameters.get("procedureName").getAsString());
			if(procedureMetaData.isEmpty()) {
				logMessages.add(String.format("Unable to retive the meta data of the procedure. Exiting the execution"));
				logger.warn(logMessages.get(logMessages.size()-1));
				report.setResult(Constants.FAILURE);
				return;
			}
			String query = new DataBaseUtility().buildProcedureCallString(actionParameters.get("procedureName").getAsString(), actionParameters.get("parameters").getAsJsonObject().keySet().size());
			logger.info("Query build {}", query);
			if(!new DataBaseUtility().executeStoreProcedure(actionParameters.get("connectionString").getAsString(),
					actionParameters.get(Constants.CREDENTIALS).getAsJsonObject().get(Constants.USER_NAME).getAsString(),
					actionParameters.get(Constants.CREDENTIALS).getAsJsonObject().get(Constants.PASSWORD).getAsString(), query, actionParameters.get("parameters").getAsJsonObject(), procedureMetaData)) {
				logMessages.add(String.format("Unable to sync the records to database. Will try on next run"));
				logger.error(logMessages.get(logMessages.size()-1));
				report.setResult(Constants.FAILURE);
				return;
			}
			logMessages.add(String.format("All records are synced."));
			logger.info(logMessages.get(logMessages.size()-1));
			report.setResult(Constants.SUCCESS);
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
}