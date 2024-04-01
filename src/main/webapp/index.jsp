<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri = "http://java.sun.com/jsp/jstl/functions" prefix = "fn" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="ISO-8859-1">
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>TaskX</title>
<link rel="icon" href="images/favicon-w.ico" type="image/x-icon">
<link rel="stylesheet" type="text/css" href="css/style.css">
<link rel="stylesheet" type="text/css" href="css/jquery.growl.css">
<script type="text/javascript" src="js/jquery-3.4.1.min.js"></script>
<script type="text/javascript" src="js/bootstrap.min.js"></script>
<script type="text/javascript" src="js/inline-script.js"></script>
<script type="text/javascript" src="js/jquery.growl.js"></script>
</head>
<body>
	<header>
		<div class="header-container">
			<div class="logo-container">
				<img class="logo" src="images/taskX_logo_w.png" alt="TaskX" title="TaskX" />
			</div>
			<div class="header-right-container">
				<div class="header-left"></div>
				<div class="header-right">
					<ul class="header-options">
					<li class="lock-btn-li">
							<button class="lock-btn" onclick="getCiphertextpopup();" title="Encryption">
								<i class="fa-thin fa-lock"></i>
							</button>
						</li>
						<li class="menu-btn-li">
							<button class="menu-btn">
								<i class="fa-light fa-bars"></i>
							</button>
						</li>
					</ul>
				</div>
			</div>
		</div>
	</header>
	<section class="section-container">
		<aside class="aside-container">
			<nav class="nav-container">
				<ul class="nav-list">
					<c:forEach begin="0" end="${jobs.size() -1}" var="index">
						<li class="success">
							<div class="nav-list-option" data-jobName='${jobs.get(index).get("jobName").getAsString()}' onClick="getDetails(this);">
								<p class="schedule-name">${jobs.get(index).get("jobName").getAsString()}</p>
								<span class="schedule-action">${jobs.get(index).get("action").getAsString()}</span>
								<ul class="schedule-progressbar" id="${fn:replace(jobs.get(index).get('jobName').getAsString(), ' ', '')}">
									<c:set var="iteration" scope="page" value="1" />
									<c:if test="${jobs.get(index).get('lastTenRuns').getAsJsonArray() != '[]'}">
										<c:forEach begin="0" end="${jobs.get(index).get('lastTenRuns').getAsJsonArray().size() -1}" var="lastRuns">
											<li class=${jobs.get(index).get('lastTenRuns').getAsJsonArray().get(lastRuns)}><span>${lastRuns}</span></li>
											<c:set var="iteration" scope="page" value="${iteration + 1}" />
										</c:forEach>
									</c:if>
									<c:if test="${iteration < 10}">
										<c:forEach begin="${iteration}" end="10" var="remaining">
											<li class=""><span></span></li>
										</c:forEach>
									</c:if>
								</ul>
							</div>
						</li>
					</c:forEach>
				</ul>
			</nav>
		</aside>
		<div class="body-container">
			<div class="body-inner-container" id="reportContent">
			
			</div>
		</div>
	</section>
<div class="popup-container hide" id="encryption_popup">
		<div id="modelContent" class="popup-body-container" style="width: 450px; height: auto;">
			<button class="close-popup-btn">X</button>
			<div class="popup-body">
				<div class="popup-head-content">
					<h3 class="h3-head">Encryption</h3>
				</div>
				<div class="popup-body-content">
				<ul class="form-container">
                    <li>
                        <div class="input-group">
                            <div class="label-field">
                                <label>Enter your plain text here</label>
                            </div>
                            <div class="input-field input-icon">
                                <input class="input-password" type="password" maxlength="100" id="plainText">
                                <i class="fa-solid fa-eye input-field-icon show-password"></i>
                            </div>
                        </div>
                    </li>
                    <li id="getCipherPW_field" class="hide">
                        <div class="input-group">
                            <div class="input-field input-icon">
                            <textarea class="input-textarea" style="height: 90px; padding-right: 30px; resize:none;" id="getCipherPW" readonly></textarea>
                                <i class="fa-solid fa-lock-hashtag input-field-icon copy-password"></i>
                            </div>
                        </div>
                    </li>
                    </ul>
				</div>	
				<div class="popup-footer-content">
				 <ul class="popbtn-container">
				 	<li><button class="input-btn green-btn" onclick="getCiphertext();">Submit</button></li>
				 </ul>
				</div>			
			</div>
		</div>
	</div>
	<script>
		var lastJobRequested = "NA";
		$(document).ready(function() {
			$(document).on("click", ".refresh-btn", function() {
				$(this).toggleClass("active");
			});
			$(document).on("click", ".menu-btn", function() {
				$(this).toggleClass("active");
				$(this).closest("body").toggleClass("menu-active");
			});
			$(document).on("click", ".schedule-list-container .schedule-list > li > div.schedule-list-content > .schedule-list-head", function() {
				$(this).closest("li").toggleClass("active");
				$(this).closest("li").siblings("li").removeClass("active");
			});
		});
	
		function getDetails(element) {
			$.ajax({
				type: "GET",
				url: "/getDetails?jobName=" + $(element).attr('data-jobName'),
				contentType: 'application/json;charset=UTF-8',
				success: function(data) {
					$('#reportContent').html(data);
					lastJobRequested = $(element).attr('data-jobName');
					$(".highlight").removeClass("highlight");
					$(element).addClass('highlight');
					updateResults();
				},
				error: function(data) {
					$.growl.error({ title:"Task X", message:"Unable to perform the action" });
				}
			});
		}
		
		function updateResults() {
		    $.ajax({
		        type: "GET",
		        url: "/getLastTenStatus",
		        contentType: "application/json;charset=UTF-8",
		        success: function(data) {
		            let blocks = "";
		            for (let rowCount = 0; rowCount < data.length; rowCount++) {
		            	blocks = "";
		                for (let resultCount = 0; resultCount < data[rowCount].lastTenRuns.length; resultCount++) {
		                	let jobDetails = JSON.parse(data[rowCount].lastTenRuns[resultCount]);
		                    blocks = blocks + '<li class="' + jobDetails.result + '"><span></span></li>';
		                }
		                for (let decoyCount = data[rowCount].lastTenRuns.length; decoyCount < 10; decoyCount++) {
	                        blocks = blocks + '<li class=""><span></span></li>';
	                    }
		                $('#' + data[rowCount].jobName.replace(/ /g, "")).html(blocks);
		                if (lastJobRequested != "NA" && lastJobRequested == data[rowCount].jobName && data[rowCount].lastTenRuns.length > 0) {
							let jobDetails = JSON.parse(data[rowCount].lastTenRuns[0]);
							$('#lastExecutionData').html(jobDetails.jobExecutedTime + ' ' + '(' + jobDetails.duration + ' ms)');
							if($('#report li').filter('.reportContent').first().attr('id') != jobDetails.id){
								let latestReport = '<li class=" '+jobDetails.result+' reportContent" id="'+jobDetails.id+'">'+
					        	'<div class="schedule-list-content">'+
					        	'<div class="schedule-list-head"> <p>Executed at : '+jobDetails.jobExecutedTime+'</p>'+
									'<span>Duration: '+jobDetails.duration+' ms</span> </div> <div class="schedule-list-body">'+
									'<ul class="schedule-list-logs"> {{messageli}}'+
									'</ul></div></div></li>';
									let messageli = "";
									let message = JSON.parse(jobDetails.message);
									for(let messageCount =0 ; messageCount < message.length; messageCount++){
										messageli = messageli + '<li class="logMessage"><p class="log-txt">'+message[messageCount]+'</p></li>';
									}
								$('#report').prepend(latestReport.replace('{{messageli}}', messageli));
							}
		                }
		            }
		        }
		    });
		}
	
	
		$(document).ready(function() { 
			$(".show-password").click(function() {
				$(this).closest(".input-field.input-icon").toggleClass("active");
				if ($(this).closest(".input-field.input-icon").hasClass("active")) {
					$(this).siblings(".input-password").attr("type", "text");
				} else {
					$(this).siblings(".input-password").attr("type", "password");
				}
			});
		});
	
		$(document).ready(
				function() {
					$(".close-popup-btn, .popup-cancel-btn").click(function() {
						$('.popup-body-container').removeClass("popup-active");
						setTimeout(function() {
							$(".popup-container").addClass("hide");
						}, 200);
					});
	
					$("#encryption_popup .close-popup-btn").click(
							function() {
								$("#getCiphertext").val("");
								$("#getCipherPW_field").addClass("hide").find("#getCipherPW").val("");
							});
	
				});
	
		function getCiphertextpopup() {
			$(".popup-container#encryption_popup").removeClass("hide");
			setTimeout(function() {
				$('#encryption_popup .popup-body-container').addClass(
						"popup-active");
			}, 100);
		}
	
		function getCiphertext() {
			var plainText = $("#plainText").val();
			if (plainText != null && plainText.length > 0) {
				   $.ajax({
				        type: "GET",
				        url: "/crypto?plainText="+plainText,
				        contentType: "text/plain",
				        success: function(data) {
				        	$("#plainText").val("");
							if (data != 'NA') {
								$("#getCipherPW").val(data);
								$("#getCipherPW_field").removeClass("hide");
								$.growl.notice({ title:"Task X", message: "Encryption completed. Please copy the secret" });
							} else {
								$("#getCipherPW_field").addClass("hide").find("#getCipherPW").val("");
								$.growl.warn({ title:"Task X", message: "Encryption failed" });
							}
						},
						error : function(data) {
							$("#plainText").val("");
							$("#getCipherPW_field").addClass("hide").find("#getCipherPW").val("");
							$.growl.error({ title:"Task X", message: "Unable to encrypt the plain text" });
						}
					});
			} else {
				$("#getCipherPW_field").addClass("hide").find("#getCipherPW").val("");
				$.growl.warning({ title:"Task X", message: "Invalid input" });
			}
		} 
		
		//Set refresh interval in millsec
		setInterval(updateResults, 60000);
	</script>
</body>
</html>