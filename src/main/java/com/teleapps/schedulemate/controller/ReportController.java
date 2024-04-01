package com.teleapps.schedulemate.controller;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.teleapps.schedulemate.dao.ReportDao;
import com.teleapps.schedulemate.domain.Report;
import com.teleapps.schedulemate.repository.ReportRepository;
import com.teleapps.schedulemate.util.Bootstrap;
import com.teleapps.schedulemate.util.Constants;

@Controller
public class ReportController {
	
	private static final Logger logger = LogManager.getLogger(ReportController.class);

	@Autowired
	ReportRepository reportRepository;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	ReportDao reportDao;
	
	@GetMapping(value = "/")
	public ModelAndView generateReport() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("index");
		modelAndView.setStatus(HttpStatus.OK);
		List<JsonObject> jobs = new Bootstrap().getJobList();
		System.out.println("Size : " + jobs.size());
		for(int jobCount = 0; jobCount < jobs.size();jobCount++) {
			if("UpdateJobs".equalsIgnoreCase(jobs.get(jobCount).get("jobName").getAsString())) {
				System.out.println("HI");
				jobs.remove(jobCount);
			} else {
				jobs.get(jobCount).add("lastTenRuns", JsonNull.INSTANCE);
			}
		}
		for(int jobCount = 0; jobCount < jobs.size();jobCount++) {
			List<Report> reports = reportDao.getReportList(jobs.get(jobCount).get(Constants.JOB_NAME).getAsString(), 10, Constants.DESC);
			JsonArray lastTenRuns = new JsonArray();
			if(reports != null) {
				for(int reportCount = 0; reportCount < reports.size(); reportCount++) {
					lastTenRuns.add(reports.get(reportCount).getResult());
				}
			}
			jobs.get(jobCount).add("lastTenRuns", lastTenRuns);
		}
		System.out.println("jobs : "  + jobs);
		modelAndView.addObject("jobs", jobs);
		return modelAndView;
	}

	@GetMapping(value = "/getDetails")
	public ModelAndView getReprotDetails(@RequestParam String jobName) {
		ModelAndView modelAndView = new ModelAndView();
		logger.info("Report data requested for {}", jobName);
		if(jobName.trim().length() == 0) {
			logger.error("Invalid request data.");
			modelAndView.setStatus(HttpStatus.BAD_REQUEST);
			return modelAndView;
		}
		List<JsonObject> jobs = new Bootstrap().getJobList();
		JsonObject job = null;
		for(int jobCount = 0; jobCount < jobs.size();jobCount++) {
			if(jobName.equalsIgnoreCase(jobs.get(jobCount).get(Constants.JOB_NAME).getAsString())) {
				job = jobs.get(jobCount);
				break;
			}
		}
		if(job == null) {
			logger.warn("Unable to find the job {}", jobName);
			modelAndView.setStatus(HttpStatus.NOT_FOUND);
			return modelAndView;
		}
		List<Report> reports = reportDao.getReportList(job.get(Constants.JOB_NAME).getAsString(), 200, Constants.DESC);
		JsonObject reportDetails = new JsonObject();
		reportDetails.addProperty("jobName", job.get(Constants.JOB_NAME).getAsString());
		reportDetails.addProperty("jobDescription", job.get(Constants.JOB_DESCRIPTION).getAsString());
		reportDetails.addProperty("isActive", job.get(Constants.IS_ACTIVE).getAsString());
		reportDetails.addProperty("cronExpression", job.get("cronExpression").getAsString());
		reportDetails.addProperty("cronDescription", job.get("cronDescription").getAsString());
		reportDetails.addProperty("lastExecutedAt", !reports.isEmpty() ? reports.get(0).getJobExecutedTime() : "");
		reportDetails.addProperty("executedDuration", !reports.isEmpty() ? reports.get(0).getDuration() : "");
		modelAndView.addObject("reportDetails", reportDetails);
		modelAndView.addObject("reports", reports);
		modelAndView.setViewName("pages/reports");
		modelAndView.setStatus(HttpStatus.OK);
		return modelAndView;
	}
	
	@GetMapping(value = "/getLastTenStatus", produces = "application/json; charset=UTF-8")
	public ResponseEntity<String> getLastTenStatus() {
		List<JsonObject> jobs = new Bootstrap().getJobList();
		jobs = reportDao.getReportData(jobs, 10, Constants.DESC);
		if (jobs.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		} else {
			return ResponseEntity.status(HttpStatus.OK).body(jobs.toString());
		}
	}

}