	$(document).ready(function(){
			$("li#LikesAndFavorites a").click(function(){
					setTimeout(function(){
					    $("#topbar1").text(window.i18n.msgStore['home']);
					    $("#topbar2").text(window.i18n.msgStore['searchUser']);
					    $("#topbar3").text(window.i18n.msgStore['favorites']);
						function shortText(string,maxCaracter){
							var last = string.length-4;
							if (string.length>maxCaracter+4)
								var shortText = string.substr(0,maxCaracter)+"....."+string.substr(last,4);
							else
							    shortText = string;
							return shortText;
						}
						$.post("../GetFavorites",function(data){
							if (data.code == 0){
								window.favoritesList = data.favoritesList;
								if (favoritesList!==undefined){
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
												$("#Message").text("Error while trying to delete the favorite. Try later").show();
											}
										}).fail(function(){
											$("#Message").text("Error: The server doesn't respound").show();
										});
									});
								}else{
									$("#tableResult").hide();
									$("#Message").text("No document Saved Yet").show();
								}
							}else{
								$("#tableResult").hide();
								$("#Message").text("Erreur You're not Connected").show();	
							}
						},"json").fail(function(){
							$("#tableResult").hide();
							$("#Message").text("There was a problem while trying to communicate with the server").show();		
						});
						},3000);	
					});
					
			});