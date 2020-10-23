var NOSEARCHESFOUND = 101;
var SERVERALREADYPERFORMED = 1;
var SERVERALLOK = 0;
var SERVERGENERALERROR = -1;
var SERVERNOTCONNECTED = -2;
var SERVERPROBLEMCONNECTIONDB = -3;
var PROBLEMECONNECTIONSERVER = -404;
window.admin_messageDiv = $("#Message");
$("p.description").prepend(window.i18n.msgStore["DESCRIPTIONGETSEARCHES"]).show().prev().prev().prepend(window.i18n.msgStore["searches"]).show();
$("#topbar1").text(window.i18n.msgStore['home']);
$("#topbar2").text(window.i18n.msgStore['adminUI-MyAccount']);
$("#topbar3").text(window.i18n.msgStore['adminUI-Searches']);
function showError(code){
	var message;
	var danger = true;
	switch(code){
		case NOSEARCHESFOUND:
			danger = false;
			message = window.i18n.msgStore["NOSEARCHESFOUND"];
		break;
	case SERVERNOTCONNECTED:	
		message = window.i18n.msgStore["SERVERNOTCONNECTED"];
		break;
	case SERVERPROBLEMCONNECTIONDB:
		message = window.i18n.msgStore["SERVERPROBLEMCONNECTIONDB"];
		break;
	case PROBLEMECONNECTIONSERVER:
		message = window.i18n.msgStore["PROBLEMECONNECTIONSERVER"];
		break;
	default :
		message = window.i18n.msgStore["SERVERGENERALERROR"];
		break;
}
$("#tableResult").hide();
admin_messageDiv.text(message).show();
if (danger){
		admin_messageDiv.addClass("danger").prepend('<i class="fas fa-exclamation-triangle"></i>  <br/>');
}else{
	admin_messageDiv.removeClass("danger");
	}
}
$(document).ready(function(){
			var tableResult = $("table#tableResult");
			function shortText(string,maxCaracter){
				var last = string.length-4;
				if (string.length>maxCaracter+4)
					var shortText = string.substr(0,maxCaracter)+"....."+string.substr(last,4);
				else
				    shortText = string;
				return shortText;
			}
			setTimeout(function(){
				$.post("../GetSearches",function(data){
				$('.loading').hide();
				tableResult.show();
				if (data.code == 0){
					if (data.searchesList!==undefined && data.searchesList.length!=0){
						$.each(data.searchesList,function(name,search){
							var line = $('<tr class="tr">'+
										'<th class="title col-xs-3">' + name + '</th>'+
										'<th class="tiny col-xs-9"><a href="/Datafari/Search?lang=' + window.i18n.language + '&request='+encodeURIComponent(search)+'">' + window.i18n.msgStore["run"] + '</a></th>'+
										'<th class="text-center delete"><i class="fas fa-ban"></i></th>'+
										'</tr>'
							);
							line.data("id",search);
							line.data("name",name);
							$("table#tableResult tbody").append(line);
						});
						$('.delete i').click(function(e){
							var element = $(e.target);
							while (!element.hasClass('tr')){
								element = element.parent();
								console.log(element);
							}
							$.post("../deleteSearch",{name:element.data('name'),request:element.data('id')},function(data){
								if (data.code>=0){
									element.remove();
								}else{
									showError(data.code);
								}
							}).fail(function(){
								showError(PROBLEMECONNECTIONSERVER);
							});
						});
					}else{
						showError(NOSEARCHESFOUND);
					}
				}else{
					showError(data.code);	
				}
			},"json").fail(function(){
				$('.loading').hide();
				showError(PROBLEMECONNECTIONSERVER);
			});	
			},1000);
});