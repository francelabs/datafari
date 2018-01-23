$(function($) {

//	Manager.addWidget(new AjaxFranceLabs.SliderWidget({
//		elm : $('#facet_slider'),
//		id : 'slider',
//		name : 'Test',
//		field : 'last_modified',
//		range : false
//	}));

//	Manager.addWidget(new AjaxFranceLabs.TabWidget({
//		elm : $('#source_tabs'),
//		id : 'facet_tab',
//		selectionType : 'ONE',
//		field : 'connector',
//		returnUnselectedFacetValues : true
//	}));

//	Manager.addWidget(new AjaxFranceLabs.OntologySuggestionWidget({
//		elm : $('#suggestion'),
//		id : 'suggestion',
//		parentLabelsField : 'ontology_parents_labels',
//		childLabelsField : 'ontology_children_labels',
//		useLanguage : true
//	}));

//	Manager.addWidget(new AjaxFranceLabs.LanguageSelectorWidget({
//		// Take the languageSelector element by ID.
//		elm : $('#languageSelector'),
//		id : 'languageSelector'
//	}));

	Manager.addWidget(new AjaxFranceLabs.LoginDatafariLinksWidget({
		// Take the loginDatafariLinks element by ID.
		elm : $('#loginDatafariLinks'),
		id : 'loginDatafariLinks'
	}));

	Manager.addWidget(new AjaxFranceLabs.TableWidget({
		elm : $('#facet_extension'),
		id : 'facet_extension',
		field : 'extension',
		name : window.i18n.msgStore['facetextension'],
		pagination : true,
		selectionType : 'OR',
		returnUnselectedFacetValues : true
	}));

	/*
	Manager.addWidget(new AjaxFranceLabs.HierarchicalFacetWidget({
		elm : $('#facet_hierarchical_url'),
		id : 'facet_hierarchical',
		field : 'urlHierarchy',
		name : window.i18n.msgStore['facethierarchicalurl'],
		pagination : true,
		selectionType : 'OR',
		returnUnselectedFacetValues : true,
		rootLevel : 0,
		maxDepth : 20,
		separator : '/',
		maxDisplay : 100
	}));
	*/

	Manager.addWidget(new AjaxFranceLabs.TableWidget({
		elm : $('#facet_language'),
		id : 'facet_language',
		field : 'language',
		name : window.i18n.msgStore['facetlanguage'],
		pagination : true,
		selectionType : 'OR',
		returnUnselectedFacetValues : true
	}));

	Manager.addWidget(new AjaxFranceLabs.TableWidget({
		elm : $('#facet_source'),
		id : 'facet_source',
		field : 'source',
		name : window.i18n.msgStore['facetsource'],
		pagination : true,
		selectionType : 'OR',
		returnUnselectedFacetValues : true
	}));

	Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({
		elm : $('#facet_last_modified'),
		id : 'facet_last_modified',
		field : 'last_modified',
		name : window.i18n.msgStore['facetlast_modified'],
		pagination : true,
		selectionType : 'ONE',
		queries : [ '[NOW-1MONTH TO NOW]',  '[NOW-1YEAR TO NOW]',  '[NOW-5YEARS TO NOW]'
		],
		labels : [ window.i18n.msgStore['facetlast_modified0'], window.i18n.msgStore['facetlast_modified1'], window.i18n.msgStore['facetlast_modified2']],
		modules : [new AjaxFranceLabs.DateSelectorFacetModule({
			id : 'dsfm-last_modified',
			field : 'last_modified'
		})]
	}));
//*
	Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({
		elm : $('#facet_file_size'),
		id : 'facet_file_size',
		field : 'original_file_size',
		name : window.i18n.msgStore['facet_File size'],
		pagination : true,
		selectionType : 'ONE',
		queries : [
			'[0 TO 102400]',
			'[102400 TO 10485760]',
			'[10485760 TO *]'
		],
		labels: [
			window.i18n.msgStore['facet_Less than 100ko'],
			window.i18n.msgStore['facet_From 100ko to 10Mo'],
			window.i18n.msgStore['facet_More than 10Mo']
		],
	}));
//*/
	Manager.addWidget(new AjaxFranceLabs.TableMobileWidget({
		elm : $('#facet_type_mobile'),
		id : 'facet_type_mobile',
		field : 'extension',
		name : window.i18n.msgStore['type'],
		pagination : true,
		selectionType : 'OR',
		returnUnselectedFacetValues : true
	}));

	Manager.addWidget(new AjaxFranceLabs.TableMobileWidget({
		elm : $('#facet_source_mobile'),
		id : 'facet_source_mobile',
		field : 'source',
		name : window.i18n.msgStore['source'],
		pagination : true,
		selectionType : 'OR',
		sort : 'ZtoA',
		returnUnselectedFacetValues : true
	}));


    var location = window.history.location || window.location;

	Manager.addWidget(new AjaxFranceLabs.SearchBarWidget({
		elm : $('#searchBar'),
		id : 'searchBar',
		autocomplete : true,
		activateAdvancedSearchLink : true
	}));

	/*
	* Add advanced search widget
	*/

	var as = new AjaxFranceLabs.AdvancedSearchWidget({
		// Take the advancedSearch element by ID.
		elm : $('#advancedSearch'),
		id : 'advancedSearch'
	});

	//createAdvancedSearchTable(as);

	Manager.addWidget(as);

	Manager.addWidget(new AjaxFranceLabs.SearchInformationWidget({
		elm : $('#search_information'),
		id : 'searchInformation'
	}));

	if (window.isLikesAndFavoritesEnabled)
		Manager.addWidget(new AjaxFranceLabs.LikesAndFavoritesWidget());
	else
		//Manager.addWidget(new AjaxFranceLabs.ExternalResultWidget());
		Manager.addWidget(new AjaxFranceLabs.SubClassResultWidget());
		//Manager.addWidget(new AjaxFranceLabs.PrevisualizeResultWidget());

	/*new  AjaxFranceLabs.ResultIllustratedWidget({
		elm : $('#results'),
		id : 'documents',
		pagination : true,
		firstTimeWaypoint : true,
		isMobile : $(window).width()<800,
		mutex_locked:false,
		}));
		/*new AjaxFranceLabs.ResultWidget(
					{
						elm : $('#results'),
						id : 'documents',
						pagination : true,
						firstTimeWaypoint : true,
						isMobile : $(window).width()<800,
						mutex_locked:false,

						afterRequest : function() {
							var data = this.manager.response, elm = $(this.elm),self=this;
							if (!this.isMobile)
								elm.find('.doc_list').empty();
							else
								elm.find('.doc_list .bar-loader').remove();
							if (data.response.numFound === 0) {
								elm
										.find('.doc_list')
										.append(
												'<div class="doc no"><div class="res no"><span class="title noResult">'+window.i18n.msgStore['noResult']+'</span></div></div>');
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
																	'<div class="doc e-'+ i + '" id="'+ doc.id+ '"></div>');
													elm.find('.doc:last').append(
																	'<div class="res"></div>');

													elm.find('.doc:last .res').append('<span class="icon"></span>');
													var extension;
													if (typeof doc.extension === "undefined"){
														extension = doc.source;
													} else {
														extension = doc.extension;
													}
													if (self.isMobile){
														if (extension.toLowerCase()!==undefined && extension.toLowerCase()!="")
															elm.find('.doc:last .icon').append('<span>['+ extension.toUpperCase() +']</span> ');
													}
													else
														elm.find('.doc:last .icon').append('<object data="images/icons/'+ extension.toLowerCase() +'-icon-24x24.png"><img src="images/icons/default-icon-24x24.png" /></object>&nbsp;');

									                var urlRedirect = 'URL?url='+ url + '&id='+Manager.store.get("id").value + '&q=' + Manager.store.get("q").value + '&position='+position;
													elm.find('.doc:last .res').append('<a class="title" target="_blank" href="'+urlRedirect+'"></a>');
													elm.find('.doc:last .title').append('<span>' +decodeURIComponent(doc.title) + '</span>')
													.append('<span class="favorite"><i class="fa fa-bookmark-o"></i></span>');
													elm.find('.doc:last .res').append('<p class="description">');
													elm.find('.doc:last .description').append('<div id="snippet">'+ description+ '</div>');
													elm.find('.doc:last .description').append('<div id="urlMobile"><p class="address">')
													.append('<div class="metadonne"><span class="liker">Like</span>  <i class="fa fa-thumbs-up"></i><span class="likes">12</span></div>');
													elm.find('.doc:last .address').append('<span>' + AjaxFranceLabs.tinyUrl(decodeURIComponent(url)) + '</span>');

												});

								AjaxFranceLabs.addMultiElementClasses(elm
										.find('.doc'));
								if (this.pagination)
									this.pagination.afterRequest(data);
							}
							if (this.isMobile){
								if ( $(".doc_list").children().length<parseInt($("#number_results_mobile span").text(),10)){
									if (data.response.docs.length!=0){
										$("#results .doc_list_pagination").show();
										if (this.firstTimeWaypoint){
											this.firstTimeWaypoint = false;
											var waypoin = $(".doc_list_pagination").waypoint(function(e){
												this.destroy();
												self.firstTimeWaypoint = true;
												self.mutex_locked=true;
												self.pagination.pageSelected ++;
												self.nextPage();
												self.mutex_locked=false;
												Waypoint.refreshAll();
											},{ offset: 'bottom-in-view'});
											while(self.mutex_locked)
												sleep(1);
											//home made mutex  used to stop the browser from executing multiple times the lines above
											// without waiting the precedent execution (no native mutex are enable in javascript)
										}
									}
								}else{
									$("#results .doc_list_pagination").hide();
								}
							}
						}
					}));*/



		Manager.addWidget(new AjaxFranceLabs.PromolinkWidget({
		elm : $('#promolink'),
		id : 'promolink'
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

function createAdvancedSearchTable(as) {

	var ast = new AjaxFranceLabs.AdvancedSearchTable({
		parent : '#advancedSearch',
		title : window.i18n.msgStore['advancedSearch-label'],
		description : window.i18n.msgStore['advancedSearch-descr']
	});

	// TODO Call Solr to create rows dynamically, based on the index fields

	var asf = new AjaxFranceLabs.AdvancedSearchField({
		parent : '#advancedSearchTable',
		label : window.i18n.msgStore['advancedSearch-title-label'],
		description : window.i18n.msgStore['advancedSearch-title-descr'],
		field : 'title'
	});

	ast.addField(asf);

	asf = new AjaxFranceLabs.AdvancedSearchField({
		parent : '#advancedSearchTable',
		label : window.i18n.msgStore['advancedSearch-content-label'],
		description : window.i18n.msgStore['advancedSearch-content-descr'],
		field : 'content'
	});

	ast.addField(asf);

	as.addTable(ast);
};


function getParamValue(param,url)
{
	var u = url == undefined ? document.location.href : url;
	var reg = new RegExp('(\\?|&|^)'+param+'=(.*?)(&|$)');
	matches = u.match(reg);
	if (matches === null)
		return '';
	return matches[2] != undefined ? decodeURIComponent(matches[2]).replace(/\+/g,' ') : '';
}

function replaceAll(str, find, replace) {
	  return str.replace(new RegExp(find, 'g'), replace);
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
