package com.teleapps.schedulemate.util;

import org.quartz.Scheduler;

public class Constants {
	
	private Constants() {
		
	}
	
	public static final String JOBS_FILE_PATH = "Jobs.json";
	
	public static final String JOB_NAME = "jobName";
	public static final String JOB_DESCRIPTION = "jobDescription";
	public static final String CRON_EXPRESSION = "cronExpression";
	public static final String CRON_DESCRIPTION = "cronDescription";
	public static final String IS_ACTIVE = "isActive";
	public static final String ACTION = "action";
	public static final String ACTION_PARAMETERS = "actionParameters";
	public static final String COUNT = "count";
	public static final String ORDER = "order";
	public static final String DESC = "za";
	public static final String ASC = "az";
	
	public static final String SOURCE_DIRECTORY = "sourceDirectory";
	public static final String DESTINATION_DIRECTORY = "destinationDirectory";
	public static final String GRXML_TEMPLATE_NAME = "grxmlTemplateName";
	public static final String CREATED_ON = "createdOn";
	
	public static final String SHEET_NUMBER = "sheetNumber";
	public static final String IGNORE_CAPTION = "ignoreCaption";
	
	public static final String METHOD = "method";
	public static final String HEADERS = "headers";
	
	public static final String SUCCESS = "success";
	public static final String FAILURE = "failure";
	public static final String YES = "yes";
	public static final String NO = "no";
	public static final String NA = "na";
	
	public static final String COLUMN_NAME = "COLUMN_NAME";
	public static final String TYPE_NAME = "TYPE_NAME";
	public static final String COLUMN_SIZE = "COLUMN_SIZE";
	
	public static final String CREDENTIALS = "credentials";
	public static final String USER_NAME = "userName";
	public static final String PASSWORD = "password";
	
	public static final String DATE_FORMAT_WITH_TIME = "dd-MMM-yyyy hh:mm:ss";
	public static Scheduler scheduler=null;
}
