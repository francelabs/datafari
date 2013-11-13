$(function($) {

	Manager.addWidget(new AjaxFranceLabs.SearchBarWidget({
		elm : $('#searchBar'),
		id : 'searchBar',
		autocomplete : true
	}));

	Manager.addWidget(new AjaxFranceLabs.SearchInformationWidget({
		elm : $('#result_information'),
		id : 'searchInformation'
	}));

	
	
	Manager.addWidget(new AjaxFranceLabs.TableWidget({
		elm : $('#facet_type'),
		id : 'facet_type',
		field : 'extension',
		name : 'Format',
		pagination : true,
		selectionType : 'OR',
		returnUnselectedFacetValues : true
	}));
	
	Manager.addWidget(new AjaxFranceLabs.TableWidget({
		elm : $('#facet_source'),
		id : 'facet_source',
		field : 'root',
		name : 'Source',
		pagination : true,
		selectionType : 'OR',
		returnUnselectedFacetValues : true
	}));
	
	

	Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({
		elm : $('#facet_date'),
		id : 'facet_date',
		name : 'Derni&egrave;re modification',
		selectionType : 'ONE',
		field : 'lastModified',
		pagination : true,
		queries : [ '[NOW-1MONTH TO NOW]', '[NOW-1YEAR TO NOW]',
				'[NOW-5YEARS TO NOW]'

		],
		labels : [ 'Moins%20de%20un%20mois', 'Moins%20de%20un%20an', 'Moins%20de%205%20ans']
	}));
	
	
	
	Manager
			.addWidget(new AjaxFranceLabs.ResultWidget(
					{
						elm : $('#results'),
						id : 'documents',
						pagination : true,
						afterRequest : function() {
							var data = this.manager.response, elm = $(this.elm);
							elm.find('.doc_list').empty();
							if (data.response.numFound === 0) {
								elm
										.find('.doc_list')
										.append(
												'<span class="noResult">Aucun fichier trouv&eacute;</span>');
							} else {
								var self = this;
								$.each(data.response.docs,
												function(i, doc) {
													var url = doc.url;
													var description = '';
													if (data.highlighting[doc.id].doc_hl) {
														description = data.highlighting[doc.id].doc_hl[0];
													}
													elm.find('.doc_list').append(
																	'<div class="doc e-'+ i + '" id="'+ doc.id+ '">');
													elm.find('.doc:last').append(
																	'<div class="res">');

													elm.find('.doc:last .res').append('<span class="icon">');

													elm.find('.doc:last .icon').append('<object data="images/icons/'+ doc.extension.toLowerCase() +'-icon-24x24.png"><img src="images/icons/default-icon-24x24.png" /></object>&nbsp;');
													elm.find('.doc:last .res').append('<a class="title" target="_blank" href="' + url + '">');
													elm.find('.doc:last .title').append('<span>' +decodeURIComponent(doc.filename) + '</span>'); 
													elm.find('.doc:last .res').append('<p class="description">');
													elm.find('.doc:last .description').append('<span>'+ description+ '</span>');
													elm.find('.doc:last .description').append('<p class="address">');
													elm.find('.doc:last .address').append('<span>' + AjaxFranceLabs.tinyUrl(decodeURIComponent(url)) + '</span>');
													
												});
								
								AjaxFranceLabs.addMultiElementClasses(elm
										.find('.doc'));
								if (this.pagination)
									this.pagination.afterRequest(data);
							}
						}
					}));

	
	Manager.addWidget(new AjaxFranceLabs.SpellcheckerWidget({
		elm : $('#spellchecker'),
		id : 'spellchecker'
	}));
	
	Manager.store.addByValue('facet', true);


	$.when(Manager.init()).then(
			function() {
				
				var query = getParamValue('query', decodeURIComponent(window.location.search));
				var searchType = getParamValue('searchType', window.location.search);
				
				if (searchType === ''){
					searchType = 'allWords';
				}
				$('#searchBar').find('.searchBar input[type=text]').val(query);
				
				/*
				 * atLeastOneWord allWords exactExpression
				 */
				var radios = $('#searchBar').find(
							'.searchMode input[type=radio]');

				$.each(radios, function(key, radio) {
						if (radio.value === searchType) {
							radio.checked=true;
						}
				});


				Manager.makeRequest();
			});
});


function getParamValue(param,url)
{
	var u = url == undefined ? document.location.href : url;
	var reg = new RegExp('(\\?|&|^)'+param+'=(.*?)(&|$)');
	matches = u.match(reg);
	if (matches === null)
		return '';
	return matches[2] != undefined ? decodeURIComponent(matches[2]).replace(/\+/g,' ') : '';
}


function createCookie(name,value,days) {
	if (days) {
		var date = new Date();
		date.setTime(date.getTime()+(days*24*60*60*1000));
		var expires = "; expires="+date.toGMTString();
	}
	else var expires = "";
	document.cookie = name+"="+value+expires+"; path=/";
}

function readCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

function eraseCookie(name) {
	createCookie(name,"",-1);
}