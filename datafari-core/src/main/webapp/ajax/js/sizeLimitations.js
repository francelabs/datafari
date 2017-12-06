


//@ sourceURL=sizeLimitations.js


$(document).ready(function() {
	//Internationalize content
	document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-SizeLimitation'];
	document.getElementById("labelhl").innerHTML = window.i18n.msgStore['labelhl']+" : ";
	document.getElementById("submithl").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("hlname").innerHTML = window.i18n.msgStore['limitHL'];

	document.getElementById("labelindexhl").innerHTML = window.i18n.msgStore['labelindexhl']+" : ";
	document.getElementById("submitindexhl").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("hlindexname").innerHTML = window.i18n.msgStore['limitindexHL'];
	

	document.getElementById("labelindexed").innerHTML = window.i18n.msgStore['labelindexed']+" : ";
	document.getElementById("submitindexed").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("indexedname").innerHTML = window.i18n.msgStore['indexedname'];
	
	//Disable the input and submit
	$('#submithl').attr("disabled", true);
	$('#maxhl').attr("disabled", true);
	//If the semaphore was for this page and the user leaves it release the semaphores
	//On refresh
	$(window).bind('beforeunload', function(){  								
		if(document.getElementById("submithl")!==null){
			if(!document.getElementById("submithl").getAttribute('disabled')){
				  cleanSem("hl.maxAnalyzedChars");
			}
		}
	 });
	$(window).bind('beforeunload', function(){  								
		if(document.getElementById("submitindexhl")!==null){
			if(!document.getElementById("submitindexhl").getAttribute('disabled')){
				  cleanSem("maxLength");
			}
		}
	 });
	//If the user loads an other page
	$("a").click(function(e){
		if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
			if(document.getElementById("submithl")!==null){
				if(!document.getElementById("submithl").getAttribute('disabled')){
					  cleanSem("hl.maxAnalyzedChars");
				}
			}
		}
	});
	//Get hl.maxAmaxAnalyzedChars value
	getModifyNodeContent("hl.maxAnalyzedChars", "hl");
	
	//Sert the button to call the function set with the hl.maxAnalyzedChars parameter
	$("#submithl").click(function(e){
		e.preventDefault();
		setModifyNodeContent("hl.maxAnalyzedChars", "hl", "#submithl");
	});
	
	//If the user loads an other page
	$("a").click(function(e){
		if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
			if(document.getElementById("submitindexhl")!==null){
				if(!document.getElementById("submitindexhl").getAttribute('disabled')){
					  cleanSem("maxLength");
				}
			}
		}
	});
	//Get hl.maxAmaxAnalyzedChars value
	getModifyNodeContent("maxLength", "indexhl");
	//Sert the button to call the function set with the hl.maxAnalyzedChars parameter
	$("#submitindexhl").click(function(e){
		e.preventDefault();
		setModifyNodeContent("maxLength", "indexhl", "#submitindexhl");
	});
	
	
	//Get hl.maxAmaxAnalyzedChars value
	getModifyFieldType("maxTokenCount", "indexed");
	//Sert the button to call the function set with the hl.maxAnalyzedChars parameter
	$("#submitindexed").click(function(e){
		e.preventDefault();
		setModifyFieldType("maxTokenCount", "indexed", "#submitindexed");
	});
});

// use the same kind of function to be coherent with the "Limit hl size" function on the same page, but we should definitely refactor this.
function getModifyFieldType(typeConf, type){
	document.getElementById("max"+type).value = "";
	$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet
        type: "GET",
        url: "./../admin/ModifyFieldType",
        data : "type="+typeConf+"&class=solr.LimitTokenCountFilterFactory",
        //if received a response from the server
        success: function( data, textStatus, jqXHR) {	
        	//If they're was an error
        	//we should definitely change the API response !!!!
        	if(data.toString().indexOf("Error code : ")!==-1){
        		//print it and disable the input and submit
        		document.getElementById("globalAnswer").innerHTML = data;
        		$('#submit'+type).attr("disabled", true);
        		$('#max'+type).attr("disabled", true);
        	}else{		//else add the options to the select
        		document.getElementById("max"+type).value = data;    
        		$('#submit'+type).attr("disabled", false);
        		$('#max'+type).attr("disabled", false);
        	}
        }
 	});
}

//use the same kind of function to be coherent with the "Limit hl size" function on the same page, but we should definitely refactor this.
function setModifyFieldType(typeConf, type, buttonId){
	var value = document.getElementById("max"+type).value;
	if(value>0 && value % 1 === 0){
		// Deactivate the calling button
		$(buttonId).prop("disabled", true);
		$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet to modify the solrconfig file
        	type: "POST",
        	url: "./../admin/ModifyFieldType",
        	data : "type="+typeConf+"&value="+value+"&class=solr.LimitTokenCountFilterFactory",
        	//if received a response from the server
        	success: function( data, textStatus, jqXHR) {
        		// Reactivate the calling button
        		$(buttonId).prop("disabled", false);
        		//If the semaphore was already acquired
        		//If they're was an error
            	//we should definitely change the API response !!!!
        		if(data.toString().indexOf("Error code : ")!==-1){        		
        			//print it and disable the input and submit
        			document.getElementById("globalAnswer").innerHTML = data;
        			$('#submit'+type).attr("disabled", true);
    	    		$('#max'+type).attr("disabled", true);
   		     	}else{		//else add the options to the select
   		     		document.getElementById("answer"+type).innerHTML = window.i18n.msgStore['modifDoneNextIndexation'];
   		     		$("#answer"+type).addClass("success");
   		     		$("#answer"+type).fadeOut(3000,function(){
   		     			$("#answer"+type).removeClass("success");
   		     			$("#answer"+type).html("");
   		     			$("#answer"+type).show();
   		     		});
    	    	}
    	    }
 		});
	}else{
		document.getElementById("answer"+type).innerHTML = window.i18n.msgStore['inputMust'];
	}
}

//Call the get function with the correct parameter
function getModifyNodeContent(typeConf, type){
	document.getElementById("max"+type).value = "";
	$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet
        type: "GET",
        url: "./../admin/ModifyNodeContent",
        data : "type="+typeConf+"&attr=name",
        //if received a response from the server
        success: function( data, textStatus, jqXHR) {	
        	//If the semaphore was already acquired
        	if(data === "File already in use"){
        		//Print it and disable the input and submit
        		document.getElementById("answer"+type).innerHTML = window.i18n.msgStore['usedFile'];
        		$('#submit'+type).attr("disabled", true);
        		$('#max'+type).attr("disabled", true);
        	}//If they're was an error
        	else if(data.toString().indexOf("Error code : ")!==-1){
        		//print it and disable the input and submit
        		document.getElementById("globalAnswer").innerHTML = data;
        		$('#submit'+type).attr("disabled", true);
        		$('#max'+type).attr("disabled", true);
        	}else{		//else add the options to the select
        		document.getElementById("max"+type).value = data;    
        		$('#submit'+type).attr("disabled", false);
        		$('#max'+type).attr("disabled", false);
        	}
        }
 	});
}
function setModifyNodeContent(typeConf, type, buttonId){
	var value = document.getElementById("max"+type).value;
	if(value>0 && value % 1 === 0){
		// Deactivate the calling button
		$(buttonId).prop("disabled", true);
		$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet to modify the solrconfig file
        	type: "POST",
        	url: "./../admin/ModifyNodeContent",
        	data : "type="+typeConf+"&value="+value+"&attr=name",
        	//if received a response from the server
        	success: function( data, textStatus, jqXHR) {
        		// Reactivate the calling button
        		$(buttonId).prop("disabled", false);
        		//If the semaphore was already acquired
        		if(data === "File already in use"){
        			//Print it and disable the input and submit
        			document.getElementById("answer"+type).innerHTML = window.i18n.msgStore['usedFile'];
        			$('#submit'+type).attr("disabled", true);
        			$('#max'+type).attr("disabled", true);
        		}//If they're was an error
        		else if(data.toString().indexOf("Error code : ")!==-1){        		
        			//print it and disable the input and submit
        			document.getElementById("globalAnswer").innerHTML = data;
        			$('#submit'+type).attr("disabled", true);
    	    		$('#max'+type).attr("disabled", true);
   		     	}else{		//else add the options to the select
   		     		document.getElementById("answer"+type).innerHTML = window.i18n.msgStore['modifDoneNextIndexation'];
   		     		$("#answer"+type).addClass("success");
		     		$("#answer"+type).fadeOut(3000,function(){
		     			$("#answer"+type).removeClass("success");
		     			$("#answer"+type).html("");
		     			$("#answer"+type).show();
		     		});
    	    	}
    	    }
 		});
	}else{
		document.getElementById("answer"+type).innerHTML = window.i18n.msgStore['inputMust'];
	}
}
function cleanSem(type){
	$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet to release the semaphore
        type: "GET",
        url: "./../admin/ModifyNodeContent",
        data : "sem=sem&type="+type
	 });
}
