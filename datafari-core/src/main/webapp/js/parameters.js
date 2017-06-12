$(document).ready(function() {
	
	$('#parameters').click(function() {
		$("#parametersUi").show();
		$("#results_div").hide();
		$("#search_information").hide();
		$("#sortMode").hide();
		$("#advancedSearchLink").hide();
	});
	
	
	//Internationalize content
	$("#lang-label").text(window.i18n.msgStore['language']);
	$("#alert-label").text(window.i18n.msgStore['alerts']);
	$("#savedsearch-label").text(window.i18n.msgStore['savedsearch']);
	$("#param-label").text(window.i18n.msgStore['param']);
	
	// Add active class to the parameters button
	$("#param").addClass("active");
	
	
	// Reload the entire page to return to the search view when click on the search button
	$('.searchBar .search').click(function() {
		location.reload();
	});
	
			// Reload the entire page to return to the search view when keypressed
	$('.searchBar input[type=text]').keypress(function(event) {
		if (event.keyCode === 13) {
			location.reload();
		}
	});
	
	// Hide advanced search
	/*
	var a = document.createElement('a');
    a.setAttribute("class", "linkAdvancedSearch");
    var linkText = document.createTextNode(window.i18n.msgStore['advancedSearchLink']);
    a.appendChild(linkText);

    a.href = "AdvancedSearch";
    var element = document.getElementById("searchBarContent");
    element.appendChild(a);
	*/
	// Hide the advanced search link
	$("#showAdvancedSearch").hide();
	
	var param = retrieveParamValue();
	
	changeContent(param);
	
	$("#lang-link").click(function() {changeContent("lang")});
	$("#alert-link").click(function() {changeContent("alert")});
	$("#savedsearch-link").click(function() {changeContent("savedsearch")});
	
});
	var d;
	var table;
	
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
			createLangContent();
		} else if(param == "alert") {
			$("#param-content-title").text(window.i18n.msgStore['param-alert']);
			$("#alert-link .link-icon").addClass("selected");
			createAlertContent();
		} else if (param == "savedsearch") {
			$("#param-content-title").text(window.i18n.msgStore['param-savedsearch']);
			$("#savedsearch-link .link-icon").addClass("selected");
			createSavedSearchContent();
		}
	}
	
	function clear() {
		$("#lang-link .link-icon").removeClass("selected");
		$("#alert-link .link-icon").removeClass("selected");
		$("#savedsearch-link .link-icon").removeClass("selected");
	}
	
	function createSavedSearchContent() {
		$.ajax({			//Ajax request to the doGet of the Alerts servlet
	        type: "GET",
	        url: "./GetSearches",
	    	beforeSend: function(jqXHR, settings){
	    		$("#param-content").html("<center><div class=\"bar-loader\" style=\"display : block; height : 32px; width : 32px;\"></div></center>");
	    	},
	        //if received a response from the server
	    	success: function( data, textStatus, jqXHR) {
	        	if (data.code == 0){
					if (data.searchesList!==undefined && data.searchesList.length!=0){
						$("#param-content").html("<table id='tableResult'><thead><tr><th>"+window.i18n.msgStore['search']+"</th><th>"+window.i18n.msgStore['link']+"</th><th>"+window.i18n.msgStore['delete']+"</th></tr></thead><tbody></tbody></table>");
						$.each(data.searchesList,function(name,search){
							var line = $('<tr class="tr">'+
										'<td>' + name + '</td>'+
										'<td><a href="/Datafari/Search?lang=' + window.i18n.language + '&request=\''+search+'\'">' + window.i18n.msgStore['exec-search'] + '</a></td>'+
										"<td><a class='delete-button'>x</a></td>"+
										'</tr>'
							);
							line.data("id",search);
							line.data("name",name);
							$("#tableResult tbody").append(line);
						});
						var tableResult = $("#tableResult").DataTable(
        				{
        					"info":false, 
        					"lengthChange":false, 
        					"searching":false,
        					"columns": [
        						null,
        						{ "orderable": false },
        						{ "orderable": false }
        					]
        				});
						$('.delete-button').click(function(e){
							var element = $(e.target);
							while (!element.hasClass('tr')){
								element = element.parent();
								console.log(element);
							}
							$.post("./deleteSearch",{name:element.data('name'),request:element.data('id')},function(data){
								if (data.code==0){
									tableResult
					                .row(element) 
					                .remove()
					                .draw();
								}else{
									console.log(data.status);
								}
							}).fail(function(){
								console.log(window.i18n.msgStore['dbError']);
							});
						});
					}else{
						$("#param-content").html("<div><b>"+window.i18n.msgStore["NOSEARCHESFOUND"]+"</b></div>");
					}
				} else{
					console.log(window.i18n.msgStore['dbError']);
	        	}
	        },
	       
	        //If there was no response from the server
	        error: function(jqXHR, textStatus, errorThrown){
	            console.log("Something really bad happened " + textStatus);
	        }
	    });
	}
	
	function createLangContent() {
		$("#param-content").html("<div id='lang-choice'><span id='lang-choice-label'>" + window.i18n.msgStore['lang-choice'] + "</span></div>");
		var inputs = $("<div id='lang-inputs'></div>").append("<input type='radio' name='lang' id='en' value='en'><label for='en'> " + window.i18n.msgStore['english_locale'] + "</label><br>");
		inputs.append("<input type='radio' name='lang' value='fr' id='fr'><label for='fr'> " + window.i18n.msgStore['french_locale'] + "</label><br>");
		
		$("#param-content").append(inputs);
		
		// Select the language for languageSelectorWidget, based on the window.i18n language detected from the browser/system
		inputs.find('input[value="'+ window.i18n.language + '"]').prop('checked', true); 
		inputs.find('input').show();
		
		$("#param-content").append("<div class='separator'></div><div id='button-div'><input type='button' name='validate-lang' id='validate-lang' value='" + window.i18n.msgStore['validate'] + "'/></div>");
		$("#validate-lang").click(function() {
			window.i18n.userSelected(inputs.find('input:checked').val());
		});
	}
	
	function createAlertContent() {
		var dataString = "keyword=";
		$.ajax({			//Ajax request to the doGet of the Alerts servlet
	        type: "GET",
	        url: "./admin/Alerts",
	        data: dataString,
	    	beforeSend: function(jqXHR, settings){
	    		$("#param-content").html("<center><div class=\"bar-loader\" style=\"display : block; height : 32px; width : 32px;\"></div></center>");
	    	},
	        //if received a response from the server
	        success: function( data, textStatus, jqXHR) {
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		console.log(data);
	        	} else if(data.alerts!=undefined){
	        		$("#param-content").html("<table id='favorites_table'><thead><tr><th>"+window.i18n.msgStore['date']+"</th><th>"+window.i18n.msgStore['search']+"</th><th>"+window.i18n.msgStore['send-frequency']+"</th><th>"+window.i18n.msgStore['delete']+"</th></tr></thead><tbody></tbody></table>");
	        		//get the data in a global var so it can be used in edit() or remove() 
	        		d=data;
	        		var numb = data.alerts.length;
	        		var i = 0;          
					while (i<numb){	//While they are still alerts to print
						var doc = data.alerts[i];
						//Print the alert with an href of the keyword towards edit()
						$("#favorites_table tbody").append("<tr id=\"alert-"+i+"\"><td></td><td><span class='alert_search_term'>"+doc.keyword+"</span></td><td id='frequency-"+i+"' class='frequency'>"+window.i18n.msgStore[doc.frequency]+" <span class='modify-link'><a href='javascript: modify("+i+")'>"+window.i18n.msgStore['modify']+"</a></span></td><td><a href=\"javascript: remove("+i+")\" class='delete-button'>x</a></td></tr>");
						//Print a button with an href towards remove()
						i++;
					}
					table = $("#favorites_table").DataTable(
	        				{
	        					"info":false, 
	        					"lengthChange":false, 
	        					"searching":false,
	        					"columns": [
	        						null,
	        						null,
	        						null,
	        						{ "orderable": false }
	        					]
	        				}
	        		);
	        	}else{
	        		$("#param-content").html("<div><b>"+window.i18n.msgStore['noAlerts']+"</b></div>");
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
	
	function modify(i) {
		$("#frequency-"+i).html("<select required id=\"frequency\" name=\"frequency\" class=\"col-sm-4\">	<OPTION value='hourly'>"+window.i18n.msgStore['hourly']+"</OPTION><OPTION value='daily'>"+window.i18n.msgStore['daily']+"</OPTION><OPTION value='weekly'>"+window.i18n.msgStore['weekly']+"</OPTION></select> <button onclick='javascript: validate("+i+")' >"+window.i18n.msgStore['validate']+"</button>")
		$("#frequency").val(d.alerts[i].frequency);
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
		    	value = $("#frequency").val();
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
	        		d.alerts[i].frequency = $("#frequency").val();
	        		d.alerts[i]._id = JSON.parse(data).uuid;
	        		table.cell("#frequency-"+i).data(window.i18n.msgStore[d.alerts[i].frequency]+" <span class='modify-link'><a href='javascript: modify("+i+")'>"+window.i18n.msgStore['modify']+"</a></span>").draw();
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
	            	table
	                .row( "#alert-" + i) 
	                .remove()
	                .draw();
	        	}        
	        },
	        //If there was no resonse from the server
	        error: function(jqXHR, textStatus, errorThrown){
	             console.log("Something really bad happened " + textStatus);
	        }
		});
	}
