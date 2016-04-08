$(document).ready(function() {
		setupLanguage();
		fillQuerySelector();
		var core = "FileShare";
		$("#docsTableContent").sortable({
			
			helper: function(e, tr) {
				var $originals = tr.children();
				var $helper = tr.clone();
				$helper.children().each(function(index)
				{
				  // Set helper cell sizes to match the original sizes
				  $(this).width($originals.eq(index).width());
				});
				return $helper;
			},
			  
			stop: function( event, ui ) {
				    $("#docsTableContent tr").each(function( index ) {
				    	$(this).find('.position').html((index + 1));
				    });
			  }
			  
		}).disableSelection();
		
		$("#query").change(function() {getQuery()});
		
		$("#saveElevateConf").click(function() {
			var docsList = new Array();
			$("#docsTableContent tr").each(function( index ) {
				docsList[index] = $(this).attr("id");
		    });
			$.post("../SearchAdministrator/queryElevator",{
				query : $("#query").val(),
				docs : docsList
			},function(data){
				if(data.code == 0) {
					$.get("./proxy/solr/admin/cores?action=RELOAD&core=" + core,function(){
						$("#message").html(window.i18n.msgStore["confSaved"]);
						$("#message").addClass("success");
						$("#message").show();						
					});
				} else {
					$("#message").html(window.i18n.msgStore["confSaveError"]);
					$("#message").addClass("error");
					$("#message").show();
				}
			},"json");
		});
		
		$("#addDocButton").click(function() {
			addNewDocLine();
		});
		
		$("#saveNewElevate").click(function() {
			var docsList = new Array();
			$(".docInput").each(function( index ) {
				if($(this).val()) {
					docsList[index] = $.trim($(this).val());
				}
		    });
			$.post("../SearchAdministrator/queryElevator",{
				query : $.trim($("#queryInput").val()),
				docs : docsList
			},function(data){
				if(data.code == 0) {
					$.get("./proxy/solr/admin/cores?action=RELOAD&core=" + core,function(){
						$("#message2").html(window.i18n.msgStore["confSaved"]);
						$("#message2").addClass("success");
						$("#message2").show();						
					});
				} else {
					$("#message2").html(window.i18n.msgStore["confSaveError"]);
					$("#message2").addClass("error");
					$("#message2").show();
				}
			},"json");
		});
});

function addNewDocLine() {
	$("#createTbody").append("<tr><td/><td><input type='text' class='textInput docInput'/></td><td/></tr>");
	$("#addDocButton").appendTo("#createTbody tr:last td:last");
}
	
function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
	 document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	 document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
	 document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-QueryElevator'];
	 document.getElementById("selectQuery").innerHTML = window.i18n.msgStore['selectQuery'];
	 document.getElementById("modifyElevateLabel").innerHTML = window.i18n.msgStore['modifyElevateLabel'];
	 document.getElementById("modifyDocsOderLabel").innerHTML = window.i18n.msgStore['modifyDocsOderLabel'];
	 document.getElementById("elevatorDocsListLabel").innerHTML = window.i18n.msgStore['elevatorDocsListLabel'];
	 $("#saveElevateConf").attr("value", window.i18n.msgStore["confirm"]);
	 
	 $("#createElevateLabel").html(window.i18n.msgStore["createElevateLabel"]);
	 $("#queryThLabel").html(window.i18n.msgStore["queryThLabel"]);
	 $("#saveNewElevate").attr("value", window.i18n.msgStore["confirm"]);
}

function fillQuerySelector() {
	$.get("../SearchAdministrator/queryElevator", { get: "queries"}).done(function(data)
			{
				var queries = data.queries;
				var sel = document.getElementById('query');
				for(var i = 0; i < queries.length; i++) {
				    var opt = document.createElement('option');
				    opt.innerHTML = queries[i];
				    opt.value = queries[i];
				    sel.appendChild(opt);
				}
			},"json");
}

function getQuery(){
	//Clean the docs list
	$("#docsTableContent").empty();
	
	//get the selected query
	var query = document.getElementById("query").value;
	if(query != "") {
		$.get("../SearchAdministrator/queryElevator", { get: "docs", query : query}).done(function(data)
				{
					for(var i = 0; i < data.docs.length; i++) {
					    $("#docsTableContent").append("<tr class='movable_line' id='" + data.docs[i] + "'><td>" + data.docs[i] + "</td><td class='position'>" + (i + 1) + "</td></tr>");
					}
				},"json");
	}
}