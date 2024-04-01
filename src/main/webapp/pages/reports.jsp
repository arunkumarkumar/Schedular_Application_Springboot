<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<div class="schedule-status-container">
	<ul class="schedule-status" id="caption">
		<li><div><p>Job Name :</p><span>${reportDetails.get("jobName").getAsString()}</span></div></li>
		<li><div><p>Description</p><span>${reportDetails.get("jobDescription").getAsString()}</span></div></li>
	    <li><div><p>Last Execution :</p><span id="lastExecutionData">${reportDetails.get("lastExecutedAt").getAsString()}&nbsp;(${reportDetails.get("executedDuration").getAsString()}&nbsp;ms)</span></div></li>
	    <li><div><p>CRON Expression :</p><span>${reportDetails.get("cronExpression").getAsString()}&nbsp;(${reportDetails.get("cronDescription").getAsString()})</span></div></li>
	    <c:if test="${reportDetails.get('isActive').getAsBoolean() == 'true'}">
	    	<li><div><p>Status :</p><span class="success">Running</span></div></li>
	    </c:if>
	    <c:if test="${reportDetails.get('isActive').getAsBoolean() == 'false'}">
	    	<li><div><p>Status :</p><span class="failure">Halt</span></div></li>
	    </c:if>
    </ul>
</div>
<div class="schedule-list-container">
	<ul class="schedule-list" id="report">
		<c:forEach items="${reports}" var="report">
        	<li class="${report.getResult()} reportContent" id="${report.getId()}">
	        	<div class="schedule-list-content">
		        	<div class="schedule-list-head">
		            	<p>Executed at : ${report.getJobExecutedTime()}</p>
						<span>Duration: ${report.getDuration()}&nbsp;ms</span>
					</div>
					<div class="schedule-list-body">
						<ul class="schedule-list-logs">
		            		<c:forEach items="${report.getMessage()}" var="message">
		            			<li class="logMessage"><p class="log-txt">${message}</p></li>
		            		</c:forEach>
						</ul>
					</div>
				</div>
	        </li> 
		</c:forEach>
    </ul>
</div>