$(document).ready(function() {
	
	$('#favorites').click(function() {
		initUI();
		$("#favoritesUi").show();
		$("#favorites").addClass("active");
		loadFavorites(window.current);
	});
	
	//Internationalize content
	$("#favorites-label").text(window.i18n.msgStore['favorites']);
	$("#doc-name").text(window.i18n.msgStore['doc-name']);
	$("#doc-source").text(window.i18n.msgStore['source']);
	$("#doc-delete").text(window.i18n.msgStore['delete']);
	
	
	
	$("#previous").hide();
	$("#next").hide();


	$("#previous").click(function() {

		window.currentPage -= 1;
		loadFavorites();
	});
	

	$("#next").click(function() {

		window.currentPage += 1;
		loadFavorites();
	});
	

	window.currentPage = 0;
	window.cursors = [''];
	$("#previous").hide();

	$("#next").hide();
	
	
	
    loadFavorites();
	
});

function initUI() {
	$("#results_div").hide();
	$("#search_information").hide();
	$("#sortMode").hide();
	$("#advancedSearch").hide();
	$("#parametersUi").hide();
	$("#searchBar").show();
	clearActiveLinks();
}

function clearActiveLinks() {
	$("#loginDatafariLinks").find(".active").removeClass("active");
}

var NOFAVORITESFOUND = 101;
var SERVERALREADYPERFORMED = 1;
var SERVERALLOK = 0;
var SERVERGENERALERROR = -1;
var SERVERNOTCONNECTED = -2;
var SERVERPROBLEMCONNECTIONDB = -3;
var PROBLEMECONNECTIONSERVER = -404;

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
	$("#Message").text(message).show();
	if (danger){
		$("#Message").addClass("danger").prepend('<i class="fa fa-exclamation-triangle"></i>  <br/>');
	}else{
		$("#Message").removeClass("danger");
	}
}


function shortText(string,maxCaracter){
	var last = string.length-4;
	if (string.length>maxCaracter+4)
		var shortText = string.substr(0,maxCaracter)+"....."+string.substr(last,4);
	else
	    shortText = string;
	return shortText;
}

function loadFavorites(){
	$('.loading').show();
	$("#tableResult").hide();
		var params = {
		    nextToken: window.cursors[window.currentPage]
		};
	
		$.getJSON("./GetFavorites",params, function(data){
		$('.loading').hide();
		$("#tableResult").show();
		if (data.code == 0){
			$("table#tableResult tbody").empty();
			window.favoritesList = data.favoritesList;
			
			// add paging
			if (window.currentPage == 0){
				$("#previous").hide();
			} else {
				$("#previous").show();
			}
			
			if (data.nextToken !== undefined){
				window.cursors[window.currentPage+1] = data.nextToken;
				$("#next").show();
			} else {

				$("#next").hide();
			}
			
			if (favoritesList!==undefined && favoritesList.length!=0){
				
				// add each favorite found in Json response
				$.each(favoritesList,function(index,favorite){
                    
					var linkPrefix = "http://"+window.location.hostname+":"+window.location.port+"/Datafari/URL?url=";
					var splitArray = favorite.split("/");
                    
                	var line = $('<tr class="tr">'+
                            '<th class="title col-xs-3"><a href="'+linkPrefix+favorite+'">'+shortText(splitArray[splitArray.length-1],30)+'</a></th>'+
                            '<th class="tiny col-xs-9">'+favorite+"</th>"+
                            '<th class="text-center delete"><i class="fa fa-times"></i></th>'+
                            '</tr>');
					
					line.data("id",favorite);
					$("table#tableResult tbody").append(line);
				});
				// handle favorite deletion
				$('.delete i').click(function(e){
					var element = $(e.target);
					while (!element.hasClass('tr')){
						element = element.parent();
					}
					$.post("./deleteFavorite",{idDocument:element.data('id')},function(data){
						if (data.code>=0){
							loadFavorites(window.current);
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


		
}
