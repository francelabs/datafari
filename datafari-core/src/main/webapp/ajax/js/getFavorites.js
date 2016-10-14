//@ sourceURL=getFavorites.js

		var NOFAVORITESFOUND = 101;
		var SERVERALREADYPERFORMED = 1;
		var SERVERALLOK = 0;
		var SERVERGENERALERROR = -1;
		var SERVERNOTCONNECTED = -2;
		var SERVERPROBLEMCONNECTIONDB = -3;
		var PROBLEMECONNECTIONSERVER = -404;
		window.admin_messageDiv = $("#Message");
		$("p.description").prepend(window.i18n.msgStore["DESCRIPTIONGETFAVORITES"]).show().prev().prev().prepend(window.i18n.msgStore["favorites"]).show();
	    $("#topbar1").text(window.i18n.msgStore['home']);
	    $("#topbar2").text(window.i18n.msgStore['adminUI-MyAccount']);
	    $("#topbar3").text(window.i18n.msgStore['adminUI-Favorites']);
		function showError(code){
			var message;
			var danger = true;
			switch(code){
				case NOFAVORITESFOUND:
					danger = false;
					message = window.i18n.msgStore["NOFAVORITESFOUND"];
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
					admin_messageDiv.addClass("danger").prepend('<i class="fa fa-exclamation-triangle"></i>  <br/>');
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
							$.get("../GetFavorites",function(data){
							$('.loading').hide();
							tableResult.show();
							if (data.code == 0){
								window.favoritesList = data.favoritesList;
								if (favoritesList!==undefined && favoritesList.length!=0){
									var linkPrefix = "http://"+window.location.hostname+":"+window.location.port+"/Datafari/URL?url=";
									$.each(favoritesList,function(index,favorite){
										var splitArray = favorite.split("/");
										console.log(splitArray);
										var line = $('<tr class="tr">'+
													'<th class="title col-xs-3"><a href="'+linkPrefix+favorite+'">'+shortText(splitArray[splitArray.length-1],30)+'</a></th>'+
													'<th class="tiny col-xs-9">'+favorite+"</th>"+
													'<th class="text-center delete"><i class="fa fa-ban"></i></th>'+
													'</tr>'
										);
										line.data("id",favorite);
										$("table#tableResult tbody").append(line);
									});
									$('.delete i').click(function(e){
										var element = $(e.target);
										while (!element.hasClass('tr')){
											element = element.parent();
											console.log(element);
										}
										$.post("../deleteFavorite",{idDocument:element.data('id')},function(data){
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
									showError(NOFAVORITESFOUND);
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