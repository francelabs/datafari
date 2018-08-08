//# sourceURL=/Datafari/ajax/js/configDeduplication.js

	setupLanguage();
 	
 	function setupLanguage(){
 		$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
 		document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
 	   	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
 	   	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-Deduplication'];
 	}	   
	   
	var timerInitiate = setTimeout(function(){	
		message_info.hide();
		message_info.removeClass().addClass(CLASSMESSAGEDANGER);
		message_info_contenue.text(MESSAGESERVERDOWN);
		message_info_title.text(TITLESERVERDOWN);
		message_info.show("slow");
	},5000);
	
	var message_info = $("#message_info");
	var message_info_title = $("#message_info h4");
	var message_info_contenue = $("#message_info p");
	var CLASSMESSAGE = "bs-callout";
	var CLASSMESSAGEDANGER = CLASSMESSAGE+" bt-callout-danger";
	var CLASSMESSAGESUCCESS = CLASSMESSAGE+" bt-callout-success";
	var MESSAGESERVERDOWN = "The server doesn't respond. Please try again or contact your system adminstrator";
	var TITLESERVERDOWN = "The server doesn't respond";
	var MESSAGEERRORINTERN = "A problem was encountered while trying to submit your request. The server sent back an error message. Please try again " 
	                       + "or contact your system adminstrator ";
	var TITLEERRORINTERN  = "A problem was encountered";
	var TITLESUCCESS  = "Success";
	var MESSAGESUCCESS = "The change was saved"
	var MESSAGEERRORDEDUPLICATION = "Error the deduplication factory isn't activated, please enable it to enable deduplication";
	var TITLEERRORDEDUPLICATION  = "Error";
	
	$.post("/Datafari/ConfigDeduplication",{initiate:"true"},function(data){
		if (data.code == 0){
			clearTimeout(timerInitiate);
			$("div.deduplication").click(function click_handler(e){
				e.preventDefault();
				var element = $(e.target);

		
				while (element.find('input').length==0){
					element = element.parent();
				}
				element = element.find('input');
				var parent = element.parent().parent();
				if (parent[0].id==="deduplication_factory"){
					if (element.is(':checked')){
						$("#deduplication").off('click').on('click',function(e){
							e.preventDefault();
							message_info.hide();
							message_info.removeClass().addClass(CLASSMESSAGEDANGER);
							message_info_contenue.text(MESSAGEERRORDEDUPLICATION);
							message_info_title.text(TITLEERRORDEDUPLICATION);
							message_info.show("slow");							
						}).find("input").prop("checked",false);
		
					}
					else{
						$("#deduplication").off('click').on('click',click_handler);
					}
				}
				var enable = "true";
				if (element.is(':checked')){
					enable="false";
				}
				
				var timer = setTimeout(function(){
					// if the server doesn't response to our query
					message_info.hide();
					message_info.removeClass().addClass(CLASSMESSAGEDANGER);
					message_info_contenue.text(MESSAGESERVERDOWN);
					message_info_title.text(TITLESERVERDOWN);
					message_info.show("slow");
				},5000);
				$.post("/Datafari/ConfigDeduplication",{id:element.data("id"),enable:enable},function(data){
					if (data.code == 0){
						// if all was ok
						clearTimeout(timer);
						message_info.hide();
						message_info.removeClass().addClass(CLASSMESSAGESUCCESS);
						message_info_contenue.text(MESSAGESUCCESS);
						message_info_title.text(TITLESUCCESS);
						message_info.show("slow");
						element.prop("checked",!element.is(":checked"));		
					}else{
						// if the server say that there's an error (probably camed from datafarie.properties)
						clearTimeout(timer);
						message_info.hide();
						message_info.removeClass().addClass(CLASSMESSAGEDANGER);
						message_info_contenue.text(MESSAGEERRORINTERN);
						message_info_title.text(TITLEERRORINTERN);
						message_info.show("slow");
					}
				},"json");
			});
			if (data.checked==="")
				$("#deduplication input").prop("checked",false);
			else
				$("#deduplication input").prop("checked",true);
			if (data.checked_factory===""){
				$("#deduplication_factory input").prop("checked",false);
				$("#deduplication").off('click').on('click',function(e){
					e.preventDefault();
					message_info.hide();
					message_info.removeClass().addClass(CLASSMESSAGEDANGER);
					message_info_contenue.text(MESSAGEERRORDEDUPLICATION);
					message_info_title.text(TITLEERRORDEDUPLICATION);
					message_info.show("slow");							
				}).find("input").prop("checked",false);

			}
			else
				$("#deduplication_factory input").prop("checked",true);
			$("#deduplication input").data("id",1);
			$("#deduplication_factory input").data("id",2);			
		}else{
			clearTimeout(timerInitiate);
			//TODO code
		}
	},"json");

		
		