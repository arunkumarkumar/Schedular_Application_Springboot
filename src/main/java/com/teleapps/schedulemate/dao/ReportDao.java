package com.teleapps.schedulemate.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teleapps.schedulemate.domain.Report;
import com.teleapps.schedulemate.util.Constants;
import com.teleapps.schedulemate.util.StackTrace;

@Component
public class ReportDao {

	private static final Logger logger = LogManager.getLogger(ReportDao.class);

	@PersistenceContext
	private EntityManager entityManager;

	public List<Report> getReportList(String jobName, int count, String order) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		List<Report> reports = null;
		try {
			CriteriaQuery<Report> criteriaQuery = criteriaBuilder.createQuery(Report.class);
			Root<Report> reportRoot = criteriaQuery.from(Report.class);
			criteriaQuery.select(reportRoot);
			criteriaQuery.where((criteriaBuilder.equal(reportRoot.get(Constants.JOB_NAME), jobName)));
			if (Constants.ASC.equalsIgnoreCase(order)) {
				criteriaQuery.orderBy(criteriaBuilder.asc(reportRoot.get("id")));
			} else {
				criteriaQuery.orderBy(criteriaBuilder.desc(reportRoot.get("id")));
			}
			reports = count == 0 ? entityManager.createQuery(criteriaQuery).getResultList() : entityManager.createQuery(criteriaQuery).setMaxResults(count).getResultList();
			logger.info("{} records found", reports.size());
			return reports;
		} catch (Exception ex) {
			logger.error("Exception while fetching records in getReportList for reports {}", StackTrace.getMessage(ex));
			return reports;
		}
	}
	
	public List<JsonObject> getReportData(List<JsonObject> jobs, int count, String order) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		List<Report> reports = new ArrayList<>();
		JsonArray jobNames = null;
		JsonArray lastTenRuns = null;
		try {
			jobNames = getJobNameArrays(jobs);
			if (jobNames == null) {
				logger.error("Exception while fetching jobs for reports ");
				return Collections.emptyList();
			}
			for (int nameCount = 0; nameCount < jobNames.size(); nameCount++) {
				CriteriaQuery<Report> criteriaQuery = criteriaBuilder.createQuery(Report.class);
				Root<Report> reportRoot = criteriaQuery.from(Report.class);
				criteriaQuery
						.where(criteriaBuilder.equal(reportRoot.get(Constants.JOB_NAME), jobNames.get(nameCount).getAsString()));
				criteriaQuery.select(reportRoot);
				if (Constants.ASC.equalsIgnoreCase(order)) {
					criteriaQuery.orderBy(criteriaBuilder.asc(reportRoot.get("id")));
				} else {
					criteriaQuery.orderBy(criteriaBuilder.desc(reportRoot.get("id")));
				}
				reports = count == 0 ? entityManager.createQuery(criteriaQuery).getResultList() : entityManager.createQuery(criteriaQuery).setMaxResults(count).getResultList();
				lastTenRuns = new JsonArray();
				for (int reportCount = 0; reportCount < reports.size(); reportCount++) {
					lastTenRuns.add(new Gson().toJson(reports.get(reportCount)));
				}
				jobs.get(nameCount).add("lastTenRuns", lastTenRuns);
			}
			logger.info("{} records found", reports.size());
		} catch (Exception ex) {
			logger.error("Exception while fetching records for reports {}", StackTrace.getMessage(ex));
		}
		return jobs;
	}
	
	public JsonArray getJobNameArrays(List<JsonObject> jobs) {
		JsonArray jobNameArray = null;
		JsonArray jobNameList = null;
		try {
			jobNameArray = new Gson().toJsonTree(jobs).getAsJsonArray();
			jobNameList = new JsonArray();
			for (int jobCount = 0; jobCount < jobNameArray.size(); jobCount++) {
				JsonObject jsonObject = jobNameArray.get(jobCount).getAsJsonObject();
				String jobNames = jsonObject.get(Constants.JOB_NAME).getAsString();
				jobNameList.add(jobNames);
			}
			return jobNameList;
		} catch (Exception ex) {
			logger.error("Exception while extracting jobs name {}", StackTrace.getMessage(ex));
			return jobNameList;
		}
	}
}