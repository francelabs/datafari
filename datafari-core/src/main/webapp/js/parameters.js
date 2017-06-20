$(document).ready(function() {
	
	$('#parameters').click(function() {
		initParametersUI();
		changeContent("lang");
	});
	
	
	//Internationalize content
	$("#lang-label").text(window.i18n.msgStore['facetlanguage']);
	$("#alert-label").text(window.i18n.msgStore['alerts']);
	$("#savedsearch-label").text(window.i18n.msgStore['savedsearch']);
	$("#param-label").text(window.i18n.msgStore['param']);
	
	var param = retrieveParamValue();
	
	changeContent(param);
	
	$("#lang-link").click(function() {changeContent("lang")});
	$("#alert-link").click(function() {changeContent("alert")});
	$("#savedsearch-link").click(function() {changeContent("savedsearch")});
	
});
	var d;
	var alertsTable;
	var searchesTable;
	
	function initParametersUI() {
		$("#parametersUi").show();
		$("#results_div").hide();
		$("#search_information").hide();
		$("#sortMode").hide();
		$("#advancedSearch").hide();
		$("#favoritesUi").hide();
		$("#searchBar").show();
		clearActiveLinks();
		$("#parameters").addClass("active");
	}
	
	function clearActiveLinks() {
		$("#loginDatafariLinks").find(".active").removeClass("active");
	}
	
	function retrieveParamValue() {
		var query = window.location.search.substring(1);
		var vars = query.split("&");
		for (var i=0;i<vars.length;i++) {
			var pair = vars[i].split("=");
			if(pair[0] == "param") {
				return pair[1];
				break;
			}
		}
	}
	
	function changeContent(param) {
		clear();
		if(param == "lang") {
			$("#param-content-title").text(window.i18n.msgStore['param-lang']);
			$("#lang-link .link-icon").addClass("selected");
			$("#lang-link .link-label").addClass("selected");
			createLangContent();
		} else if(param == "alert") {
			$("#param-content-title").text(window.i18n.msgStore['param-alert']);
			$("#alert-link .link-icon").addClass("selected");
			$("#alert-link .link-label").addClass("selected");
			createAlertContent();
		}
	}
	
	function clear() {
		$("#lang-link .link-icon").removeClass("selected");
		$("#alert-link .link-icon").removeClass("selected");
		$("#lang-link .link-label").removeClass("selected");
		$("#alert-link .link-label").removeClass("selected");
	}
	
	function createLangContent() {
		var languages = ['en', 'fr', 'it', 'pt_br', 'de'];
		
		$("#param-content").html("<div id='lang-choice'><span id='lang-choice-label'>" + window.i18n.msgStore['lang-choice'] + "</span></div>");
		var inputs = $("<div id='lang-inputs'></div>")
		$.each(languages, function( index, value ) {
			inputs.append("<input type='radio' name='lang' id='" + value + "' value='" + value + "'><label for='" + value + "'> " + window.i18n.msgStore[value+'_locale'] + "</label><br>");
		});
		
		$("#param-content").append(inputs);
		
		// Select the language for languageSelectorWidget, based on the window.i18n language detected from the browser/system
		inputs.find('input[value="'+ window.i18n.language + '"]').prop('checked', true); 
		inputs.find('input').show();
		
		$("#param-content").append("<div class='separator'></div><div id='button-div'><input type='button' name='validate-lang' id='validate-lang' value='" + window.i18n.msgStore['validate'] + "'/></div>");
		$("#validate-lang").click(function() {
			var selectedLang = $('input[name=lang]:checked').val();

			// Save user language in the 'lang' table of Cassandra
			$.post('./applyLang',{"lang":selectedLang}, function() {
				// Function executed every time the user changes the language of
				// Datafari		
				window.i18n.userSelected(selectedLang);
			});
		});
	}
	
	function createAlertContent() {
		var dataString = "keyword=";
		$("#param-content").html("<div id='addAlertDiv'><button onclick='javascript:addAlert();' id='addAlertButton'>"+window.i18n.msgStore['addAlert']+"</button></div>");
		$("#param-content").append("<div id='alertsListDiv'></div>");
		$.ajax({			//Ajax request to the doGet of the Alerts servlet
	        type: "GET",
	        url: "./admin/Alerts",
	        data: dataString,
	    	beforeSend: function(jqXHR, settings){
	    		$("#alertsListDiv").html("<center><div class=\"bar-loader\" style=\"display : block; height : 32px; width : 32px;\"></div></center>");
	    	},
	        //if received a response from the server
	        success: function( data, textStatus, jqXHR) {
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		console.log(data);
	        	} else if(data.alerts!=undefined){
	        		$("#alertsListDiv").html("<table id='alerts_table'><thead><tr><th>"+window.i18n.msgStore['search']+"</th><th>"+window.i18n.msgStore['subject']+"</th><th>"+window.i18n.msgStore['mail']+"</th><th>"+window.i18n.msgStore['send-frequency']+"</th><th>"+window.i18n.msgStore['delete']+"</th></tr></thead><tbody></tbody></table>");
	        		//get the data in a global var so it can be used in edit() or remove() 
	        		d=data;
	        		var numb = data.alerts.length;
	        		var i = 0;          
					while (i<numb){	//While they are still alerts to print
						var doc = data.alerts[i];
						//Print the alert with an href of the keyword towards edit()
						$("#alerts_table tbody").append("<tr id=\"alert-"+i+"\"><td><span class='alert_search_term'>"+doc.keyword+"</span></td><td>"+doc.subject+"</td><td>"+doc.mail+"</td><td id='frequency-"+i+"' class='frequency'>"+window.i18n.msgStore[doc.frequency]+" <span class='modify-link'><button onclick='javascript: modify("+i+")'>"+window.i18n.msgStore['modify']+"</button></span></td><td><a href=\"javascript: remove("+i+")\" class='delete-button'>x</a></td></tr>");
						//Print a button with an href towards remove()
						i++;
					}
					alertsTable = $("#alerts_table").DataTable(
	        				{
	        					"info":false, 
	        					"lengthChange":false, 
	        					"searching":false,
	        					"columns": [
	        						null,
	        						null,
	        						null,
	        						null,
	        						{ "orderable": false }
	        					]
	        				}
	        		);
	        	}else{
	        		$("#alertsListDiv").html("<div><b>"+window.i18n.msgStore['noAlerts']+"</b></div>");
	        	}
	        },
	       
	        //If there was no response from the server
	        error: function(jqXHR, textStatus, errorThrown){
	            if(jqXHR.responseText==="error connecting to the database"){
	            	console.log(window.i18n.msgStore['dbError'])
	          	}else {
	                console.log("Something really bad happened " + textStatus);
	          	}
	        }
	    });  
	}
	
	function addAlert() {
		$("#addAlertDiv").html("");
		$("#addAlertDiv").append("<form id=\"add\" role=\"form\">");
		$("#add").append("<table id=\"addAlertTable\" ></table>");
		var tr = $("<tr>");
		tr.append("<td><label>"+window.i18n.msgStore['keyword']+"</label></td>");
		tr.append("<td><input required type=\"text\" id=\"keyword\" name=\"keyword\" placeholder="+window.i18n.msgStore['keyword']+"/></td>");
		$("#addAlertTable").append(tr);
		tr = $("<tr>");
		tr.append("<td><label>"+window.i18n.msgStore['subject']+"</label></td>");
		tr.append("<td><input required type=\"text\" id=\"subject\" name=\"subject\" placeholder="+window.i18n.msgStore['subject']+"/></td>");
		$("#addAlertTable").append(tr);
		tr = $("<tr>");
		tr.append("<td><label>"+window.i18n.msgStore['mail']+"</label></td>");
		tr.append("<td><input required type=\"text\" id=\"mail\" name=\"mail\" placeholder="+window.i18n.msgStore['mail']+"/></td>");
		$("#addAlertTable").append(tr);
		tr = $("<tr style='display: none;'>");
		tr.append("<td><label>"+window.i18n.msgStore['core']+"</label></td>");
		tr.append("<td><input required type=\"text\" id=\"core\" name=\"core\" placeholder=\"Core\" value=\"FileShare\"/></td>");
		$("#addAlertTable").append(tr);
		tr = $("<tr>");
		tr.append("<td><label>"+window.i18n.msgStore['frequency']+"</label></td>");
		tr.append("<td><select required id=\"frequency\" name=\"frequency\" class=\"col-sm-4\">	<OPTION value='hourly'>"+window.i18n.msgStore['hourly']+"</OPTION><OPTION value='daily'>"+window.i18n.msgStore['daily']+"</OPTION><OPTION value='weekly'>"+window.i18n.msgStore['weekly']+"</OPTION></select></td>");
		$("#addAlertTable").append(tr);
		tr = $("<tr>");
		tr.append("<td colspan=2 id='addAlertSubmit'><input type=\"Submit\" id=\"newAlerts\" name=\"AddAlert\" value=\""+window.i18n.msgStore['confirm']+"\"/><button id='addAlertCancel'>"+window.i18n.msgStore['cancel']+"</button></td>");
		$("#addAlertTable").append(tr);
		$("#addAlertDiv").append("</form>");
		$("#addAlertDiv").append("<div id='addAlertMessage'></div>");
		
		$("#addAlertCancel").click(function() {initCreateAlertButton();});
		
		$("#add").submit(function(e){
	        e.preventDefault();
	 	});
		
		$("#add").submit(function(e){
			 var datastring = $("#add").serialize();	
		 		$.ajax({ 				//Ajax request to the doPost of the Alerts servlet
		       		type: "POST",
		        	url: "./admin/Alerts",
		       	 	data: datastring,
		        	//if received a response from the server
		        	success: function(data, textStatus, jqXHR) {
		        		if(data.toString().indexOf("Error code : ")!==-1){
		        			$("#addAlertMessage").addClass("fail");
		    				$("#addAlertMessage").html(data);
		            	}else{
		            		$("#addAlertMessage").addClass("success");
		            		$("#addAlertMessage").html(window.i18n.msgStore['success']);
		            		$("#addAlertMessage").fadeOut(1500,function(){
		            			destroyAlertsTable();
		            			createAlertContent();});
		            	}
		        	},
		        	error: function(jqXHR, textStatus, errorThrown){
		        		 if(jqXHR.responseText==="error connecting to the database"){
		               		$("#addAlertsForm").append(window.i18n.msgStore['dbError'])
		               	}else {
		                     console.log("Something really bad happened " + textStatus);
		                     $("#addAlertsForm").html(jqXHR.responseText);
		               	}
		        	},
		        	//capture the request before it was sent to server
		        	beforeSend: function(jqXHR, settings){
		            	//disable the button until we get the response
		            	$('#newAlerts').attr("disabled", true);
		        	},
		        	//called after the response or error functions are finsihed
		        	complete: function(jqXHR, textStatus){
		            	//enable the button 
		            	$('#add').attr("disabled", false);
		        	}  
		 		});
		 });
	}
	
	function initCreateAlertButton() {
		$("#addAlertDiv").html("<button onclick='javascript:addAlert();' id='addAlertButton'>"+window.i18n.msgStore['addAlert']+"</button>");
	}
	
	function modify(i) {
		$("#frequency-"+i).html("<select required id=\"select-frequency-" + i + "\" name=\"frequency\" class=\"col-sm-4\">	<OPTION value='hourly'>"+window.i18n.msgStore['hourly']+"</OPTION><OPTION value='daily'>"+window.i18n.msgStore['daily']+"</OPTION><OPTION value='weekly'>"+window.i18n.msgStore['weekly']+"</OPTION></select> <button onclick='javascript: validate("+i+")' >"+window.i18n.msgStore['validate']+"</button>")
		$("#select-frequency-" + i).val(d.alerts[i].frequency);
	}
	
	function validate(i) {
		var id = "_id="+d.alerts[i]._id;
		var datastring = "";
		for (var key in d.alerts[i]) {
		    if (datastring != "") {
		    	datastring += "&";
		    }
		    var value = "";
		    if(key==="frequency") {
		    	value = $("#select-frequency-" + i).val();
		    } else {
		    	value = d.alerts[i][key];
		    }
		    datastring += key + "=" + value;
		}
		$.ajax({			 	//Ajax request to the doPost of the Alerts servlet
	        type: "POST",
	        url: "./admin/Alerts",
	        data: datastring,
	        //if received a response from the server
	        success: function(data, textStatus, jqXHR) {
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		console.log(data);
	        	}else{
	        		d.alerts[i].frequency = $("#select-frequency-" + i).val();
	        		d.alerts[i]._id = JSON.parse(data).uuid;
	        		alertsTable.cell("#frequency-"+i).data(window.i18n.msgStore[d.alerts[i].frequency]+" <span class='modify-link'><button onclick='javascript: modify("+i+")'>"+window.i18n.msgStore['modify']+"</button></span>").draw();
	        	}
	        },
	        error: function(jqXHR, textStatus, errorThrown){
	             console.log("Something really bad happened " + textStatus);
	        }
		});
	}
	
	function remove(i){
		// get the id of the alert to remove and serialize it
		var id = "_id="+d.alerts[i]._id;
		$.ajax({		//Ajax request to the doPost of the Alerts servlet
	        type: "POST",
	        url: "./admin/Alerts",
	        data: id,
	        //if received a response from the server
	        success: function(data, textStatus, jqXHR) {
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		console.log(data);
	        	}else{
	        		//Suppress the row of the tab that was show the now removed alert
//	        		var row = document.getElementById(i);
//	            	row.parentNode.removeChild(row);
	            	alertsTable
	                .row( "#alert-" + i) 
	                .remove()
	                .draw();
	            	
	            	var nbData = alertsTable.column(0).data().length
					if(nbData < 1 ) {
						$("#alertsListDiv").html("<div><b>"+window.i18n.msgStore['noAlerts']+"</b></div>");
						destroyAlertsTable();
					}
	        	}        
	        },
	        //If there was no resonse from the server
	        error: function(jqXHR, textStatus, errorThrown){
	             console.log("Something really bad happened " + textStatus);
	        }
		});
	}
	
	function destroyDatatables() {
		destroyAlertsTable();
	}
	
	function destroyAlertsTable() {
		if(alertsTable !== undefined) {
			alertsTable.clear();
			alertsTable.destroy(true);
			alertsTable = undefined;
		}
	}
