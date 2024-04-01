package com.teleapps.schedulemate.domain;


import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.google.gson.Gson;
import com.google.gson.JsonArray;


@Entity
@Table(name="REPORT")
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)	
	private Long id;
	@Column(name = "jobName", length = 255)
	private String jobName;
	@Column(name = "jobDescription", length = 255)
	private String jobDescription;
	@Column(name = "cronExpression", length = 255)
	private String cronExpression; 
	@Column(name = "cronDescription", length = 255)
	private String cronDescription;
	@Column(name = "isActive", length = 255)
	private String isActive;
	@Column(name = "action", length = 255)
	private String action;
	@Column(name = "jobExecutedTime", length = 255)
	private String jobExecutedTime;
	@Column(name = "message", columnDefinition = "NVARCHAR(MAX)")
	private String message;
	@Column(name = "jobShutdownTime", length = 255)
	private String jobShutdownTime;
	@Column(name = "result", length = 255)
	private String result;
	@Column(name = "createdOn")
	private String createdOn;
	@Column(name = "duration")
	private String duration;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public String getJobDescription() {
		return jobDescription;
	}
	public void setJobDescription(String jobDescription) {
		this.jobDescription = jobDescription;
	}
	public String getCronExpression() {
		return cronExpression;
	}
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}
	public String getCronDescription() {
		return cronDescription;
	}
	public void setCronDescription(String cronDescription) {
		this.cronDescription = cronDescription;
	}
	public String getIsActive() {
		return isActive;
	}
	public void setIsActive(String isActive) {
		this.isActive = isActive;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getJobExecutedTime() {
		return jobExecutedTime;
	}
	public void setJobExecutedTime(String jobExecutedTime) {
		this.jobExecutedTime = jobExecutedTime;
	}
	public List<String> getMessage() {
		List<String> messageList = new ArrayList<>();
		new Gson().fromJson(message, JsonArray.class).forEach(element -> messageList.add(element.getAsString()));
		return messageList;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getJobShutdownTime() {
		return jobShutdownTime;
	}
	public void setJobShutdownTime(String jobShutdownTime) {
		this.jobShutdownTime = jobShutdownTime;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	
	@Override
	public String toString() {
		return "Report [id=" + id + ", jobName=" + jobName + ", jobDescription=" + jobDescription + ", cronExpression="
				+ cronExpression + ", cronDescription=" + cronDescription + ", isActive=" + isActive + ", action="
				+ action + ", jobExecutedTime=" + jobExecutedTime + ", message=" + message + ", jobShutdownTime="
				+ jobShutdownTime + ", result=" + result + ", createdOn=" + createdOn + ", duration=" + duration + "]";
	}
}