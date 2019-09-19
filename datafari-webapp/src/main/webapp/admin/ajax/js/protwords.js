//# sourceURL=/Datafari/admin/ajax/js/protwords.js



var previous="";	
	$(document).ready(function() {

		//If the user refresh the page  
		$(window).bind('beforeunload', function(e){
			if(document.getElementById("language").value !== ""){
			  cleanSem(document.getElementById("language").value);
			}
		 });
		//If the user loads an other page
		$("a").click(function(e){
			if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
				if(document.getElementById("language").value !== ""){
					cleanSem(document.getElementById("language").value);
					$("#ajaxResponse").empty();
				}
			}
		});
		 
		 setupLanguage();

	});
	

	function setupLanguage(){
		$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
		 document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
		 document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
		 document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-Protwords'];
		 document.getElementById("documentation-protwords").innerHTML = window.i18n.msgStore['documentation-protwords'];
		 document.getElementById("Modify").innerHTML = window.i18n.msgStore['selectLang'];
		 document.getElementById("protExplain").innerHTML = window.i18n.msgStore['protExplain'];
		 $('#protwordsBox').text(window.i18n.msgStore['adminUI-Protwords']);
		 
	}
	
function getFile(){
	//If the language has been changed while a file has been opened
	if(previous!==""){
		//Clean the previous semaphore
		cleanSem(previous);
	}
	//clean the response area
	$("#ajaxResponse").empty();
	//get the language
	var language = document.getElementById("language").value;
	previous = language;
	//if a language has been selected
	if(language != ""){
    var dataString = "language=" + language;
	$.ajax({									//Ajax request to get the content
		url : './../admin/ProtWords',
		type : 'GET',
		data : dataString,
		success: function( data, textStatus, jqXHR) {
			//If the semaphore was already acquired
			if(data==="File already in use"){
				$("#ajaxResponse").append("<div class=col-sm-4></div>");
				$("#ajaxResponse").append("<h3 class=col-sm-4>"+window.i18n.msgStore['usedFile']+"</h3>");
				$("#ajaxResponse").append("<div class=col-sm-4></div>");
			}
			//If the servlet catched an exception
        	if(data.toString().indexOf("Error code : ")!==-1){
        		$("#ajaxResponse").append("<div class=col-xs-3></div>");
				$("#ajaxResponse").append("<h3 class=col-xs-6>"+data+"</h3>");
				$("#ajaxResponse").append("<div class=col-xs-3></div>");
        	}
			//Else print the content of the file in a textArea
			else{
				$("#ajaxResponse").append("<legend>"+window.i18n.msgStore['protwords']+"</legend>");
				$("#ajaxResponse").append("<form class=\"col-sm-12\" id=\"res\">");
				$("#res").append("<div class=\"form-group\" id=\"div1\">");
				$("#div1").append("<fieldset id=\"fields\">");
				$("#fields").append("<textarea id=\"input\" required resizable=\"true\"></textarea>");
				var lines = data.split("\n"),i=0, str="";
				while(i<lines.length){
					if(lines[i]!=""){
					str = str+lines[i]+"<br />";
					}
					i++;
				}
				document.getElementById("input").value = str;
				$("#input").cleditor({
		            width: 500, // width not including margins, borders or padding
		            height: 250, // height not including margins, borders or padding
	                controls: // controls to add to the toolbar
	                    " undo redo | cut copy paste pastetext | print source",
	                useCSS: false, // use CSS to style HTML when possible (not supported in ie) 
	                bodyStyle: // style to assign to document body contained within the editor
	                    "margin:4px; font:10pt Arial,Verdana; cursor:text"
		        });
				$("#fields").append("<div class=\"alert alert-warning\" id=\"reload-warning\">" + window.i18n.msgStore['adminUI-ReloadWarning'] + "</div>");
				$("#fields").append("<div><div class='col-sm-3'><label>Click to save and activate the modifications</label></div><button type=\"Submit\" class=\"btn btn-primary btn-label-left\" id=\"submit\" data-loading-text=\"<i class='fa fa-spinner fa-spin'></i> " + 
						window.i18n.msgStore['confirm'] + "\">"+window.i18n.msgStore['confirm']+"</button></div>");
				$("#div1").append("</fieldset>");
				$("#res").append("</div>");
				$("#ajaxResponse").append("</form>");
				//On submit send the value of the textArea to upload()
				$("#res").submit(function(e){
        			e.preventDefault();
 				});
				$("#res").submit(function(e){
					upload(document.getElementById("input").value);
				});
			}
		},
       error: function(jqXHR, textStatus, errorThrown){
            console.log("Something really bad happened " + textStatus);
             $("#ajaxResponse").html(jqXHR.responseText);
       }
	});
	}
}
function upload(text){
	$("#submit").button("loading");
	//Get the language and the content
	var language = document.getElementById("language").value
	var content = text;
	$.ajax({										//Ajax request to rewrite the file
		url : './ProtWords',
		type : 'POST',
		// Form data
		data : {content: content, language: language},	
		datetype : "text",
		contenttype: "text-plain/utf-8",
		success: function( data, textStatus, jqXHR) {
        	if(data.toString().indexOf("Error code : ")!==-1){
        		$("#ajaxResponse").append("<div class=col-xs-3></div>");
				$("#ajaxResponse").append("<h3 class=col-xs-6>"+data+"</h3>");
				$("#ajaxResponse").append("<div class=col-xs-3></div>");
        	}else{
				$("#ajaxResponse").empty();
				$("#ajaxResponse").append(window.i18n.msgStore['modifDone']);
        	}
		},
	 	error: function(jqXHR, textStatus, errorThrown){
         	console.log("Something really bad happened " + textStatus);
         	$("#ajaxResponse").html(jqXHR.responseText);
    	},
    	complete: function(jqXHR, textStatus){
        	//allow the user to select a language
        	$('#language').attr("disabled", false);
    		$("#submit").button("reset");
    	}
	});
}
function cleanSem(lang) {
	var language = lang;
	if(language != ""){	//If the user was editing a file
    	var dataString = "language=" + language;
    	$.ajax({													//Post request to release the semaphore
    		url : './ProtWords',
    		type : 'POST',
    		data : dataString,
    		async : false
    	});
	}
}