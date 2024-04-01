package com.teleapps.schedulemate.util;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Order;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.teleapps.schedulemate.repository.ReportRepository;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class Bootstrap {

	private static Logger logger = LogManager.getLogger(Bootstrap.class);

	private static String jobsChecksum = "";
	
	static List<JsonObject> jobsList = new ArrayList<>();

	ApplicationContext applicationContext;

	@Autowired
	public Bootstrap(ApplicationContext context) {
		applicationContext = context;
	}

	public Bootstrap() {
	}

	@Autowired
	ReportRepository reportRepository;

	@EventListener(ApplicationReadyEvent.class)
	private void init() {
		if (build(Constants.JOBS_FILE_PATH)) {
			logger.info("Jobs imported into the programme successfully");
			jobsUpdater();
			processJobs(jobsList);
		} else {
			logger.fatal("Unable to load the jobs, Terminating the application");
			if (applicationContext != null) {
				SpringApplication.exit(applicationContext, () -> 0);
			}
		}
	}

	public void processJobs(List<JsonObject> jobsList) {
		Scheduler scheduler = null;
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
			Constants.scheduler = scheduler;
			for (JsonObject jobDetails : jobsList) {
				if (scheduleJobs(scheduler, jobDetails.get(Constants.JOB_NAME).getAsString(),
						jobDetails.get(Constants.ACTION).getAsString(), jobDetails,
						jobDetails.get(Constants.CRON_EXPRESSION).getAsString())) {
					logger.info("Scheduler created for [{}]. {}", jobDetails.get(Constants.JOB_NAME).getAsString(),
							jobDetails.get(Constants.CRON_DESCRIPTION).getAsString());

				} else {
					logger.error("Failed to create scheduler for [{}]",
							jobDetails.get(Constants.JOB_NAME).getAsString());
				}
			}
		} catch (Exception ex) {
			logger.error("Failed to process the configured jobs {}", StackTrace.getMessage(ex));
			try {
				if (scheduler != null && scheduler.isStarted()) {
					scheduler.clear();
				}
			} catch (SchedulerException e) {
				logger.error("Unable to clear the scheduler {}", StackTrace.getMessage(e));
			}
		}
	}

	public boolean scheduleJobs(Scheduler scheduler, String jobName, String action, JsonObject jobDetails,
			String cronExpression) throws SchedulerException {
		try {
			JobKey job = new JobKey(jobName);
			if (scheduler.checkExists(job)) {
				JobDetail existingJobDetail = scheduler.getJobDetail(job);
				JobDataMap jobDataMap = existingJobDetail.getJobDataMap();
				jobDataMap.put(Constants.ACTION, action);
				jobDataMap.put("jobDetails", jobDetails);
				JobDetail updatedJobDetail = JobBuilder.newJob(existingJobDetail.getJobClass()).withIdentity(jobName)
						.usingJobData(jobDataMap).storeDurably().build();
				scheduler.addJob(updatedJobDetail, true);
				return true;
			} else {
				JobDataMap jobDataMap = new JobDataMap();
				jobDataMap.put(Constants.ACTION, action);
				jobDataMap.put("jobDetails", jobDetails);
				jobDataMap.put("reportRepository", reportRepository);
				JobDetail jobDetail = JobBuilder.newJob(DynamicJobExecuter.class).withIdentity(jobName)
						.usingJobData(jobDataMap).build();
				Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName + "Trigger")
						.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
				scheduler.scheduleJob(jobDetail, trigger);
				return true;
			}
		} catch (Exception ex) {
			logger.error("Exception scheduling {}", StackTrace.getMessage(ex));
			return false;
		}
	}

	private boolean build(String filePath) {
		File file = new File(filePath);
		jobsChecksum = getChecksumOfAFile(filePath);
		try (FileReader reader = new FileReader(file)) {
			Gson gson = new Gson();
			Type listType = new TypeToken<ArrayList<JsonObject>>() {
			}.getType();
			jobsList = gson.fromJson(reader, listType);
			return isAllJobsAreUnique(jobsList);
		} catch (Exception ex) {
			logger.error("Unable to build the jobs " + ex.getMessage());
			return false;
		}
	}

	private boolean isAllJobsAreUnique(List<JsonObject> jobList) {
		Set<String> jobNames = new HashSet<>();
		for (JsonObject jsonObject : jobList) {
			if (jsonObject.has(Constants.JOB_NAME)) {
				String jobName = jsonObject.get(Constants.JOB_NAME).getAsString();
				if (jobNames.contains(jobName)) {
					logger.warn("Duplicate job found in the Jobs.json.");
					return false;
				}
				jobNames.add(jobName);
			}
		}
		return true;
	}

	public List<JsonObject> getJobList() {
		return jobsList;
	}

	public boolean reloadAndProcessJobs() {
		try {
			if(jobsChecksum.equalsIgnoreCase(getChecksumOfAFile(Constants.JOBS_FILE_PATH))) {
				return false;
			}
			if (build(Constants.JOBS_FILE_PATH)) {
				for (JsonObject jobDetails : jobsList) {
					if (new Bootstrap().scheduleJobs(Constants.scheduler,
							jobDetails.get(Constants.JOB_NAME).getAsString(),
							jobDetails.get(Constants.ACTION).getAsString(), jobDetails,
							jobDetails.get(Constants.CRON_EXPRESSION).getAsString())) {
					}
				}
				jobsChecksum = getChecksumOfAFile(Constants.JOBS_FILE_PATH);
				return true;
			} else {
				logger.error("Unble to build the job from Jobs.json");
				return false;
			}
		} catch (Exception ex) {
			logger.error("Exception while rebuilding the job {}", StackTrace.getMessage(ex));
			return false;
		}
	}
	
	private void jobsUpdater() {
		JsonObject jobReader = new JsonObject();
		jobReader.addProperty("jobName", "UpdateJobs");
		jobReader.addProperty("jobDescription", "To dynamically update the jobs configured in the Jobs.json file");
		jobReader.addProperty("cronExpression", "0 */2 * ? * *");
		jobReader.addProperty("cronDescription", "Runs at every two minute");
		jobReader.addProperty("action", "UpdateJobs");
		jobsList.add(jobReader);
	}
	
	private String getChecksumOfAFile(String filePath) {
		try {
			byte[] data = Files.readAllBytes(Paths.get(filePath));
			byte[] hash = MessageDigest.getInstance("MD5").digest(data);
			return new BigInteger(1, hash).toString(16);
		} catch (Exception ex) {
			logger.error("Exception while calculating the checksum of the file {} : {}", filePath, StackTrace.getMessage(ex));
			return null;
		}
	}
	
}