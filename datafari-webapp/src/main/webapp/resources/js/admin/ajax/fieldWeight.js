//# sourceURL=/Datafari/resources/js/admin/ajax//fieldWeight.js

var list;
var timeouts = [];

var clearTimeouts = function() {
	for (var i = 0; i < timeouts.length; i++) {
	    clearTimeout(timeouts[i]);
	}

	//quick reset of the timer array you just cleared
	timeouts = [];

	$('#main').unbind('DOMNodeRemoved');
}

$(document).ready(function() {
	//Get the fields from the schema
	getFields();
	//Disabled the input and the confirm buttons while no field has been selected
	$('#weightpf').attr("disabled", true);
	$('#weightqf').attr("disabled", true);
	$('#submitpf').attr("disabled", true);
	$('#submitqf').attr("disabled", true);
	//Modify the bahviour of the submit buttons
	$("#submitpf").click(function(e){
		e.preventDefault();
	    setPF();
	});
	$("#submitqf").click(function(e){
		e.preventDefault();
	    setQF();
	});
	
	$("#uprel").click(function(e){
		e.preventDefault();
		$("#uprel").loading("loading");
		$.get("../SearchExpert/zookeeperConf?action=upload_and_reload",function(data){
			$("#uprel").loading("reset");
			if(data.code == 0) {
				$("#answerqf").hide();
				$("#answerpf").hide();
				$("#answeruprel").text(window.i18n.msgStore['zkOK']);
				$("#answeruprel").addClass("success").removeClass("fail animated fadeOut").show();
				timeouts.push(setTimeout(function(){
		          $("#answeruprel").addClass("animated fadeOut");
		        },1500));
			} else {
				$("#answeruprel").text(window.i18n.msgStore['zkDown']);
				$("#answeruprel").addClass("fail").removeClass("success animated fadeOut").show();
			}
		});
	});
	//If the user refresh the page  
	$(window).bind('beforeunload', function(){
		clearTimeouts();
		if(document.getElementById("weightpf")!==null){
			if(!document.getElementById("weightpf").getAttribute('disabled')){
				  cleanSem("pf");
			}
		}
		if(document.getElementById("weightqf")!==null){
			if(!document.getElementById("weightqf").getAttribute('disabled')){
				  cleanSem("qf");
			}
		}
	 });
	//If the user loads an other page
	$("a").click(function(e){
		clearTimeouts();
		if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
			if(document.getElementById("weightpf")!==null){
				if(!document.getElementById("weightpf").getAttribute('disabled')){
					  cleanSem("pf");
				}
			}
			if(document.getElementById("weightqf")!==null){
				if(!document.getElementById("weightqf").getAttribute('disabled')){
					  cleanSem("qf");
				}
			}
		}
	});
	//Internationalize the text
	document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-FieldWeight'];
	document.getElementById("documentation-fieldweightapi").innerHTML = window.i18n.msgStore['documentation-fieldweightapi'];
	document.getElementById("submitpf").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("submitqf").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("pfname").innerHTML = window.i18n.msgStore['pf'];
	document.getElementById("qfname").innerHTML = window.i18n.msgStore['qf'];
	document.getElementById("uprel").innerHTML = window.i18n.msgStore['save-config'];
	$("#uprel").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['saving']);
});
function getFields(){										//Get the fields from the schema.xml
	 $.ajax({			//Ajax request to the doGet of the FieldWeight servlet
	        type: "GET",
	        url: "./../admin/FieldWeight",
	        //if received a response from the server
	        success: function( data, textStatus, jqXHR) {	
	        	//If they're was an error print the error
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		//print it and disable the selection
	        		document.getElementById("globalAnswer").innerHTML = data.toString();
	        		$('#selectpf').attr("disabled", true);
	        		$('#selectqf').attr("disabled", true);
	        	}else{		//else add the options to the select
	        		list = data;
	        		for(var i = 0 ; i < data.field.length ; i++){
	        			$("#selectpf").append("<OPTION>"+data.field[i].name+"</OPTION>");
	        			$("#selectqf").append("<OPTION>"+data.field[i].name+"</OPTION>");
	        		}
	        	}
	        }
	 });
}
//functions to check if a semaphore has to be cleaned and start getValue 
function selectQF(){
	if(!document.getElementById("weightqf").getAttribute('disabled')){
		cleanSem("qf");
	}
	getValue("qf");
}
function selectPF(){
	if(!document.getElementById("weightpf").getAttribute('disabled')){
		cleanSem("pf");
	}
	getValue("pf");
}
//Get the value of a field according to his type
function getValue(type){
	//Clean the response area
	document.getElementById("answer"+type).innerHTML = "";
	document.getElementById("weight"+type).value= "";
	//Get the field that has been selected
	var field = document.getElementById("select"+type).value;
	//If it's not empty
	if(field!=""){
		$.ajax({			//Ajax request to the doGet of the FieldWeight servlet
	        type: "GET",
	        url: "./../admin/FieldWeight",
	        data : "field="+field+"&type="+type,
	        //if received a response from the server
	        success: function( data, textStatus, jqXHR) {
	        	//If the file was already in use
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		//Print it and disable the input field
	        		document.getElementById("answer"+type).innerHTML = data.toString();
	        		document.getElementById("weight"+type).value = "";
	        		$('#weight'+type).attr("disabled", true);
	        	}//If they're was an error
	        	else if(data === "Error while opening the configuration files" || data === "Something bad happened, please retry, if the problem persists contact your system administrator" ){
	        		//print it and disable the selection
	        		document.getElementById("globalAnswer").innerHTML = data;
	        		$('#selectpf').attr("disabled", true);
	        		$('#selectqf').attr("disabled", true);
	        	}else if(data.toString() === "File already in use"){
	        		document.getElementById("answer"+type).innerHTML = data.toString();
	        		$('#weight'+type).attr("disabled", true);
	        	}
	        	else{//print the value of the field 
	        		document.getElementById("weight"+type).value=data;
	        		$('#weight'+type).attr("disabled", false);
	        		$('#submit'+type).attr("disabled", false);
	        	}
	        }
		 });
	}
}
//Functions to modify the value
function setQF(){
	setValue("qf");
}
function setPF(){
	setValue("pf");
}
//Modify the value according to the type
function setValue(type){
	//Gets the field to modify and the value to put
	var field = document.getElementById("select"+type).value;
	var value = document.getElementById("weight"+type).value;
	if(field!="" && value!=""){
		if(value>=0){
			$.ajax({			//Ajax request to the doPost of the FieldWeight servlet
	        	type: "POST",
	        	url: "./../admin/FieldWeight",
	        	data : "field="+field+"&type="+type+"&value="+value,
	        	//if received a response from the server
	        	success: function( data, textStatus, jqXHR) {
	        		//If they're was an error
	        		if(data.toString().indexOf("Error code : ")!==-1){
	        			//print it and disable the selection
	        			document.getElementById("globalAnswer").innerHTML = data;
	        			$("#globalAnswer").removeClass("success").addCLass("fail").show();
		        		$('#selectpf').attr("disabled", true);
		        		$('#selectqf').attr("disabled", true);
		        		$('#submitpf').attr("disabled", true);
		        		$('#submitqf').attr("disabled", true);
		        		$('#weight'+type).attr("disabled", true);
		        	}else{//print modif done and reset the value and disable the submit and input field
		        		document.getElementById("answer"+type).innerHTML = window.i18n.msgStore['modifDoneZK'];
		        		$("#answer"+type).removeClass("fail").addClass("success").show();
		        		document.getElementById("select"+type).value = "";
		        		document.getElementById("weight"+type).value = "";
		        		$('#weight'+type).attr("disabled", true);
		        		$('#submit'+type).attr("disabled", true);
		        	}
	        	}
		 	});
		}else{
			
		}
	}
}
//Clean the Semaphore according to a type
function cleanSem(type){
	$.ajax({			//Ajax request to the doGet of the FieldWeight servlet
        type: "GET",
        url: "./../admin/FieldWeight",
        data : "sem=sem&type="+type
	 });
}
