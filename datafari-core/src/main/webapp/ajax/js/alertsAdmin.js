//@ sourceURL=alertsAdmin.js


$(document).ready(function() {
		doGet();
		var icons = {
			header : "ui-icon-circle-arrow-e",
			activeHeader : "ui-icon-circle-arrow-s"
		};
		var totalHeight =  document.getElementById("box").scrollHeight;
		document.getElementById("hints").setAttribute("style","height:"+totalHeight+"px;");
		document.getElementById("hint1").setAttribute("style","border:1px solid #ccc; margin-top : "+totalHeight/6+"px;");
		document.getElementById("labelHint1").innerHTML = window.i18n.msgStore['hint1'];
		document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
		document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
		document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-AlertAdmin'];
		document.getElementById("boxname").innerHTML = "\r"+ window.i18n.msgStore['alertAdmin'];
		document.getElementById("HourlyLabel").innerHTML = window.i18n.msgStore['hourly'];
		document.getElementById("DailyLabel").innerHTML = window.i18n.msgStore['daily'];
		document.getElementById("WeeklyLabel").innerHTML = window.i18n.msgStore['weekly'];
		document.getElementById("HostLabel").innerHTML = window.i18n.msgStore['host'];
		document.getElementById("PortLabel").innerHTML = window.i18n.msgStore['port'];
		document.getElementById("delayLegend").innerHTML = window.i18n.msgStore['alertsDelay'];
		document.getElementById("connLegend").innerHTML = window.i18n.msgStore['connConf'];
		document.getElementById("DatabaseLabel").innerHTML = window.i18n.msgStore['database'];
		document.getElementById("CollectionLabel").innerHTML = window.i18n.msgStore['collection'];
		document.getElementById("paramRegtext").innerHTML = window.i18n.msgStore['paramReg'];

		//Creates date pickers et get current dates
		$('#HourlyDelay').datetimepicker({
			dateFormat : 'dd/mm/yy/ '
		});
		$('#DailyDelay').datetimepicker({
			dateFormat : 'dd/mm/yy/'
		});
		$('#WeeklyDelay').datetimepicker({
			dateFormat : 'dd/mm/yy/'
		});
		//Remove the div of selection, useful on second load of date pickers
		$("a").click(function(){
			if(document.getElementById("ui-datepicker-div")!==null){
				var element = document.getElementById("ui-datepicker-div");
				element.parentNode.removeChild(element);
			}
		 });
		});
	
	function doGet() {
		$("#errorPrint").empty();
		$.ajax({			//Ajax request to the doGet of the AlertsAdmin servlet
			type : "GET",
			url : "./../admin/alertsAdmin",
			//if received a response from the server
			success : function(data, textStatus, jqXHR) {
				$("#prevNext").empty();
				if(data.toString().indexOf("Error code : ")!==-1){
					$("#errorPrint").append("<label>"+data.toString()+"</label>");
					document.getElementById("activated").disabled = true;
					document.getElementById("paramReg").disabled = true;
				}else{
					$("#prevNext").append("<fieldset id=\"field1\" class=\"col-sm-12\">");
					$("#field1").append("<legend id=\"prevLegend\">"+window.i18n.msgStore["previousExecution"]+"</legend><div class=\"form-group\"><label id=prevHourlyLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["hourly"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"prevHourly\"  style=\"min-width : 150px;\"disabled value="+"\""+data.hourly+"\""+"></div>");
					$("#field1").append("<div class=\"form-group\"><label id=prevDailyLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["daily"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"prevDaily\" style=\"min-width : 150px;\" disabled value="+"\""+data.daily+"\""+"></div>");
					$("#field1").append("<div class=\"form-group\"><label id=prevWeeklyLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["weekly"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"prevWeekly\" style=\"min-width : 150px;\" disabled value="+"\""+data.weekly+"\""+"></div>");
					$("#prevNext").append("</fieldset><fieldset id=\"field2\" class=\"col-sm-12\">");
					//$("#field2").append("<legend id=\"nextLegend\">"+window.i18n.msgStore["nextExecution"]+"<span class=\"fa fa-asterisk \" style=\"color : red\"></span></legend><div class=\"form-group\"><label id=nextHourlyLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["hourly"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"nextHourly\" style=\"min-width : 150px;\" disabled value="+"\""+data.nextHourly+"\""+"></div>");
					$("#field2").append("<legend id=\"nextLegend\">"+window.i18n.msgStore["nextExecution"]+"</legend><div class=\"form-group\"><label id=nextHourlyLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["hourly"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"nextHourly\" style=\"min-width : 150px;\" disabled value="+"\""+data.nextHourly+"\""+"></div>");
					$("#field2").append("<div class=\"form-group\"><label id=nextDailyLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["daily"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"nextDaily\" style=\"min-width : 150px;\" disabled value="+"\""+data.nextDaily+"\""+"></div>");
					$("#field2").append("<div class=\"form-group\"><label id=nextWeeklyLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["weekly"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"nextWeekly\" style=\"min-width : 150px;\" disabled value="+"\""+data.nextWeekly+"\""+"></div>");
					//$("#field2").append("<div class=\"control-label\"><i class=\"fa fa-asterisk\" style=\"color : red\"></i><label class=\"control-label\">"+window.i18n.msgStore['takingAccount']+"</label></div>");
					
					if (data.on === "on") //Set the button
						document.getElementById("activated").checked = true;
					else 
						document.getElementById("activated").checked = false;
					$("#prevNext").append("</fieldset>");
					
					$("#prevNext").append
					//Set the parameters
					document.getElementById("HourlyDelay").value = format(data.hourlyDate);
					document.getElementById("DailyDelay").value = format(data.dailyDate);
					document.getElementById("WeeklyDelay").value = format(data.weeklyDate);
					document.getElementById("Host").value = data.host;
					document.getElementById("Port").value = data.port;
					document.getElementById("Database").value = data.database;
					document.getElementById("Collection").value = data.collection;
					
					// SMTP conf
					if(document.getElementById("fieldForm")!==null){
						$("#fieldForm").empty();
					}
					$("#mailForm").append("<fieldset id=\"fieldForm\" class=\"col-sm-12\">");
					$("#fieldForm").append("<legend id=\"mailLegend\">"+window.i18n.msgStore["mailConf"]+"</legend><div class=\"form-group\"><label id=SMPTLabel class=\"col-sm-5 control-label\">SMTP : </label><input type=\"text\" class=\"col-sm-2\" id=\"SMTP\"  style=\"min-width : 150px;\" value="+"\""+data.smtp+"\""+"></div>");
					$("#fieldForm").append("<div class=\"form-group\"><label id=AddressLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["address"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"Address\" style=\"min-width : 150px;\"  value="+"\""+data.from+"\""+"></div>");
					$("#fieldForm").append("<div class=\"form-group\"><label id=UserLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["user"]+"</label><input type=\"text\" class=\"col-sm-2\" id=\"UserName\" style=\"min-width : 150px;\"  value="+"\""+data.user+"\""+"></div>");
					$("#fieldForm").append("<div class=\"form-group\"><label id=PassLabel class=\"col-sm-5 control-label\">"+window.i18n.msgStore["pass"]+"</label><input type=\"password\" class=\"col-sm-2\" id=\"Pass\" style=\"min-width : 150px;\"  value="+"\""+data.pass+"\""+"></div>");
					$("#mailForm").append("</fieldset>");
				}
				var totalHeight =  document.getElementById("box").scrollHeight;
				var margin = (totalHeight/2);
				document.getElementById("hint2").setAttribute("style","border:1px solid #ccc; margin-top : "+margin+"px;");
				document.getElementById("labelHint2").innerHTML = window.i18n.msgStore['hint2'];
			}
		});
	}
	
	function format(Date) {
		return Date.substring(0, 10)+"/  "+Date.substring(11, 16);
	}
	
	function onOff() {
		var activated;
		if($("#activated").is(':checked')) {
			activated = "on";
		} else {
			activated = "off";
		}
		//var data = "activated=" + document.getElementById("activated").innerHTML
		$.post("./../admin/alertsAdmin",{
			activated : activated
		},function(data, textStatus, jqXHR){
			doGet();
		});
		
	}
	
	function parameters() {
		var data = "HOURLYDELAY=" + document.getElementById("HourlyDelay").value 
				+ "&DAILYDELAY=" + document.getElementById("DailyDelay").value 
				+ "&WEEKLYDELAY=" + document.getElementById("WeeklyDelay").value
				+ "&HOST=" + document.getElementById("Host").value
				+ "&PORT=" + document.getElementById("Port").value
				+ "&DATABASE=" + document.getElementById("Database").value
				+ "&COLLECTION=" + document.getElementById("Collection").value
				+ "&smtp=" + document.getElementById("SMTP").value 
				+ "&from=" + document.getElementById("Address").value 
				+ "&user=" + document.getElementById("UserName").value
				+ "&pass=" + document.getElementById("Pass").value;
		if (data.indexOf("=&") != -1) {
			alert(window.i18n.msgStore['missingParameter']);
		}
		$.ajax({ //Ajax request to the doGet of the Alerts servlet
			type : "POST",
			url : "./../admin/alertsAdmin",
			data : data,
			//if received a response from the server
			success : function(data, textStatus, jqXHR) {
				if(data.toString().indexOf("Error code : ")!==-1){
					$("#errorPrint").append("<label>"+data.toString()+"</label>");
					document.getElementById("activated").disabled = true;
					document.getElementById("paramReg").disabled = true;
				}
				document.getElementById("parameterSaved").innerHTML = window.i18n.msgStore["parameterSaved"];
				doGet();
			},
			error : function(jqXHR, textStatus, errorThrown) {
				console.log("Something really bad happened " + textStatus);
				$("#ajaxResponse").html(jqXHR.responseText);
			},
			//capture the request before it was sent to server
			beforeSend : function(jqXHR, settings) {
				//disable the button until we get the response
				$('#add').attr("disabled", true);
			},
			//this is called after the response or error functions are finished
			complete : function(jqXHR, textStatus) {
				//enable the button 
				$('#add').attr("disabled", false);
			}
		});
	}
	