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



//	Manager.addWidget(new AjaxFranceLabs.TableWidget({
//	elm : $('#facet_type'),
//	id : 'facet_type',
//	field : 'extension',
//	name : window.i18n.msgStore['type'],
//	pagination : true,
//	selectionType : 'OR',
//	returnUnselectedFacetValues : true
//	}));

	Manager.addWidget(new AjaxFranceLabs.TableWidget({
		elm : $('#facet_categorie'),
		id : 'facet_categorie',
		field : 'categorie',
		name : 'categorie',
		pagination : true,
		selectionType : 'OR',
		returnUnselectedFacetValues : true
	}));


	Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({
		elm : $('#facet_date'),
		id : 'facet_date',
		name : 'Anciennet&eacute;e',
		selectionType : 'ONE',
		field : 'last_modified',
		pagination : true,
		queries : [ '[NOW-7DAY TO NOW]', '[NOW-1MONTH TO NOW]',
		            '[NOW-1YEAR TO NOW]'

		            ],
		            labels : [ 'Moins%20d\'une%20semaine', 'Moins%20d\'un%20mois', 'Moins%20d\'un%20an']
	}));



//	Manager.addWidget(new AjaxFranceLabs.TableWidget({
//	elm : $('#facet_language'),
//	id : 'facet_language',
//	field : 'language',
//	name : window.i18n.msgStore['language'],
//	pagination : true,
//	selectionType : 'OR',
//	returnUnselectedFacetValues : true
//	}));

//	Manager.addWidget(new AjaxFranceLabs.TableMobileWidget({
//	elm : $('#facet_type_mobile'),
//	id : 'facet_type_mobile',
//	field : 'extension',
//	name : window.i18n.msgStore['type'],
//	pagination : true,
//	selectionType : 'OR',
//	returnUnselectedFacetValues : true
//	}));

	Manager.addWidget(new AjaxFranceLabs.TableMobileWidget({
		elm : $('#facet_categorie_mobile'),
		id : 'facet_categorie_mobile',
		field : 'categorie',
		name : window.i18n.msgStore['categorie'],
		pagination : true,
		selectionType : 'OR',
		sort : 'ZtoA',
		returnUnselectedFacetValues : true
	}));


	Manager.addWidget(new AjaxFranceLabs.ResultWidget(
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
								'<span class="noResult">'+'Aucun fichier trouv&eacute;'+'</span>');
					} else {
						var self = this;
						$.each(data.response.docs,
								function(i, doc) {
							var url = doc.url.replace("localhost",window.location.hostname); 

							var positionString = Manager.store.get("start").value;
							var position = 1;
							if (positionString !== null){
								position += parseInt(positionString);
							}
							position += i;
							var description = '';
							if (data.highlighting[doc.id]) {
								$.each( data.highlighting[doc.id], function( key, value ) {
									description += value;
								});
							}
							elm.find('.doc_list').append(
									'<div class="doc e-'+ i + '" id="'+ doc.id+ '">');
							elm.find('.doc:last').append(
							'<div class="res">');

							elm.find('.doc:last .res').append('<span class="icon">');
							var extension;
							if (typeof doc.extension === "undefined"){
								extension = doc.categorie;
							} else {
								extension = doc.extension;
							}
							//elm.find('.doc:last .icon').append('<object data="images/icons/'+ extension.toLowerCase() +'-icon-24x24.png"><img src="images/icons/default-icon-24x24.png" /></object>&nbsp;');
							var urlRedirect = 'URL?url='+ url + '&id='+Manager.store.get("id").value + '&q=' + Manager.store.get("q").value + '&position='+position;
							elm.find('.doc:last .res').append('<a class="title" target="_blank" href="'+decodeURIComponent(url)+'"></a>');
							elm.find('.doc:last .title').append('<span>' +doc.title.replace(/\"/g, '').replace(/\?/g, ' ') + '</span>'); 
							elm.find('.doc:last .res').append('<p class="description">');
							elm.find('.doc:last .description').append('<div id="snippet">'+ description+ '</div>');
							elm.find('.doc:last .description').append('<div id="urlMobile"><p class="address">');
							elm.find('.doc:last .address').append('<span>' + getDate(doc.last_modified) + '</span>');
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

	Manager.addWidget(new AjaxFranceLabs.CapsuleWidget({
		elm : $('#capsule'),
		id : 'capsule'
	}));

	Manager.store.addByValue('facet', true);


	$.when(Manager.init()).then(
			function() {

				var query = getParamValue('query', decodeURIComponent(window.location.search));
				var searchType = getParamValue('searchType', window.location.search);
				var sortType = getParamValue('sortType', window.location.search);

				Manager.store.addByValue("id", UUID.generate());

				if (searchType === ''){
					searchType = 'allWords';
				}
				if (sortType === ''){
					sortType = 'score';
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


function createCookie(name,value,days) 
{
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
function getDate(date) {
	var mois = date.substring(5,7);
	var jour = date.substring(8,10);
	if (jour.substring(0,1).indexOf('0')!=-1){
		jour = jour.substring(1,2);
	}
	switch(mois){
	case '01':
		mois = 'Janvier';
		break;
	case '02':
		mois = 'F&eacute;vrier';
		break;
	case '03':
		mois = 'Mars';
		break;
	case '04':
		mois = 'Avril';
		break;
	case '05':
		mois = 'Mai';
		break;
	case '06':
		mois = 'Juin';
		break;
	case '07':
		mois = 'Juillet';
		break;		
	case '08':
		mois = 'Ao&ucirc;t';
		break;
	case '09':
		mois = 'Septembre';
		break;
	case '10':
		mois = 'Octobre';
		break;
	case '11':
		mois = 'Novembre';
		break;
	case '12':
		mois = 'D&eacute;cembre';
		break;
	default :
		mois = "Juin";
			break;
	}
	return jour+" "+mois+" "+date.substring(0,4);
}