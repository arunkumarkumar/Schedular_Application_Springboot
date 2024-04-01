package com.teleapps.schedulemate.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.teleapps.schedulemate.repository.ReportRepository;
import com.teleapps.schedulemate.service.SyncDatabase;
import com.teleapps.schedulemate.service.SyncEndpoint;
import com.teleapps.schedulemate.service.SyncGrammar;
import com.teleapps.schedulemate.service.SyncTable;

@Component
public class DynamicJobExecuter implements Job {
	
	private static final Logger logger = LogManager.getLogger(DynamicJobExecuter.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			boolean isUpdated = new Bootstrap().reloadAndProcessJobs();
			String action = (String) context.getJobDetail().getJobDataMap().get(Constants.ACTION);
			JsonObject parameters = (JsonObject) context.getJobDetail().getJobDataMap().get("jobDetails");
			ReportRepository reportRepository = (ReportRepository) context.getJobDetail().getJobDataMap().get("reportRepository");
			switch(action) {
			case "SyncGrammar":
				new SyncGrammar().init(parameters, reportRepository);
				break;
			case "SyncAPIFromFile":
				new SyncEndpoint().init(parameters, reportRepository);
				break;
			case "SyncTable":
				new SyncTable().init(parameters, reportRepository);
				break;
			case "SyncDatabase":
				new SyncDatabase().init(parameters,  reportRepository);
				break;
			case "UpdateJobs":
				if(isUpdated) {
					logger.info("Jobs.Json File loaded Successfully.");
				}
				break;
			default:
				throw new UnsupportedOperationException(String.format("Unknown function %s", action));
			}
		} catch (Exception ex) {
			logger.fatal("Exception while executing the job {} ", StackTrace.getMessage(ex));
		}
	}
}