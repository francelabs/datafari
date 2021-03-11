$(function($) {

  // Container must be a String and not a jquery object
  // It must resolve to the container object when calling $(container)
  // It must be able to be used as a javascript object key
  // Manager.addWidget(new AjaxFranceLabs.TabWidget({
  // id : 'tabwidget',
  // elm : $("#facet_tabs"),
  // container: "#tabs-container",
  // rawTabs : [ {
  // "label" : "Test",
  // "href" : "http://datafariee.datafari.com/Datafari/Search",
  // "keepURLParams" : true,
  // }, {
  // "label" : "Test2",
  // "href" : "http://www.google.com"
  // } ],
  // actifValue : "Test"
  // }));

  // Manager.addWidget(new AjaxFranceLabs.TabWidget({
  //   id : 'tabwidget2',
  //   elm : $("#facet_tabs2"),
  //   container: "#tabs-container",
  //   field: "repo_source",
  //   showEmpty: true
  // }));

  // Manager.addExternalSource(new AjaxFranceLabs.DropBoxExternalSource({
  // id : 'dropbox1',
  // resultsDiv : $("#external-results_div")
  // }));

  Manager.addWidget(new AjaxFranceLabs.SearchSuggestUIWidget({
    elm : $('#search-suggest-ui'),
    id : 'searchSuggestUI'
  }));

  Manager.addWidget(new AjaxFranceLabs.SaveSearchWidget({
    elm : $('#save_search'),
    id : 'saveSearch'
  }));

  // Manager.addWidget(new AjaxFranceLabs.SliderWidget({
  // elm : $('#facet_slider'),
  // id : 'slider',
  // name : 'Test',
  // field : 'last_modified',
  // range : false
  // }));

  // Manager.addWidget(new AjaxFranceLabs.OntologySuggestionWidget({
  // elm : $('#suggestion'),
  // id : 'suggestion',
  // parentLabelsField : 'ontology_parents_labels',
  // childLabelsField : 'ontology_children_labels',
  // useLanguage : true
  // }));

  // Manager.addWidget(new AjaxFranceLabs.LanguageSelectorWidget({
  // // Take the languageSelector element by ID.
  // elm : $('#languageSelector'),
  // id : 'languageSelector'
  // }));

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
   * Manager.addWidget(new AjaxFranceLabs.HierarchicalFacetWidget({ elm : $('#facet_hierarchical_url'), id : 'facet_hierarchical', field :
   * 'urlHierarchy', name : window.i18n.msgStore['facethierarchicalurl'], pagination : true, selectionType : 'OR',
   * returnUnselectedFacetValues : true, rootLevel : 0, maxDepth : 20, separator : '/', maxDisplay : 100 }));
   */

  /*
   * Entity Extracttion
   */
  /*
   * Entity person widget
   */
  Manager.addWidget(new AjaxFranceLabs.TableWidget({
    elm : $('#facet_entity_person'),
    id : 'facet_entity_person',
    field : 'entity_person',
    name : 'Person',
    pagination : true,
    selectionType : 'OR',
    returnUnselectedFacetValues : true
  }));
  // */

  /*
   * Entity phone widget
   */
  Manager.addWidget(new AjaxFranceLabs.TableWidget({
    elm : $('#facet_entity_phone_present'),
    id : 'facet_entity_phone_present',
    field : 'entity_phone_present',
    name : 'Phone',
    pagination : true,
    selectionType : 'OR',
    returnUnselectedFacetValues : true
  }));

  Manager.addWidget(new AjaxFranceLabs.TableWidget({
    elm : $('#facet_entity_phone'),
    id : 'facet_entity_phone',
    field : 'entity_phone',
    name : 'Phone',
    pagination : true,
    selectionType : 'OR',
    returnUnselectedFacetValues : true
  }));
  // */

  /*
   * Special entity widget
   */
  Manager.addWidget(new AjaxFranceLabs.TableWidget({
    elm : $('#facet_entity_special_present'),
    id : 'facet_entity_special_present',
    field : 'entity_special_present',
    name : 'Special Entity',
    pagination : true,
    selectionType : 'OR',
    returnUnselectedFacetValues : true
  }));
  // */

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
    field : 'repo_source',
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
	    queries : [ '[NOW-1MONTH TO NOW]', '[NOW-1YEAR TO NOW]', '[NOW-5YEARS TO NOW]' ],
	    labels : [ window.i18n.msgStore['facetlast_modified0'], window.i18n.msgStore['facetlast_modified1'], window.i18n.msgStore['facetlast_modified2'] ],
	    modules : [ new AjaxFranceLabs.DateSelectorFacetModule({
	      id : 'dsfm-last_modified',
	      field : 'last_modified'
	    }) ]
	  }));
  
  Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({
	    elm : $('#facet_creation_date'),
	    id : 'facet_creation_date',
	    field : 'creation_date',
	    name : window.i18n.msgStore['facetcreation_date'],
	    pagination : true,
	    selectionType : 'ONE',
	    queries : [ '[NOW/DAY TO NOW]', '[NOW/DAY-7DAY TO NOW/DAY]', '[NOW/DAY-30DAY TO NOW/DAY-8DAY]','([1970-09-01T00:01:00Z TO NOW/DAY-31DAY] || [* TO 1970-08-31T23:59:59Z])', '[1970-09-01T00:00:00Z TO 1970-09-01T00:00:00Z]' ],
	    labels : [ window.i18n.msgStore['facetcreation_date0'], window.i18n.msgStore['facetcreation_date1'], window.i18n.msgStore['facetcreation_date2'],window.i18n.msgStore['facetcreation_date3'],window.i18n.msgStore['facetcreation_date4'] ],
	    modules : [ new AjaxFranceLabs.DateSelectorFacetModule({
	      id : 'dsfm-creation_date',
	      field : 'creation_date'
	    }) ]
	  }));

  Manager.addWidget(new AjaxFranceLabs.AggregatorWidget({
    elm : $('#facet_aggregator'),
    id : 'facet_aggregator',
    name : window.i18n.msgStore['facet_external_datafaris'],
    pagination : true
  }));
  // *
  Manager.addWidget(new AjaxFranceLabs.TableFacetQueriesWidget({
    elm : $('#facet_file_size'),
    id : 'facet_file_size',
    field : 'original_file_size',
    name : window.i18n.msgStore['facet_File size'],
    pagination : true,
    selectionType : 'ONE',
    queries : [ '[0 TO 102400]', '[102400 TO 10485760]', '[10485760 TO *]' ],
    labels : [ window.i18n.msgStore['facet_Less than 100ko'], window.i18n.msgStore['facet_From 100ko to 10Mo'], window.i18n.msgStore['facet_More than 10Mo'] ],
    modules : [ new AjaxFranceLabs.NumberSelectorFacetModule({
        id : 'nsfm-file_size',
        field : 'original_file_size',
        multiplier : {"Ko" : 1024, "Mo" : 1024000, "Go" : 1024000000}
      }) ]
  }));
  // */

  Manager.addWidget(new AjaxFranceLabs.TagCloudWidget({
    elm : $('#facet_tag_cloud'),
    id : 'facet_tag_cloud',
    name : window.i18n.msgStore['facet_tag_cloud'] ? window.i18n.msgStore['facet_tag_cloud'] : "Tag Cloud",
    pagination : true,
    discardedValues : [ 'Other Topics' ]
  }));

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

  // createAdvancedSearchTable(as);

  Manager.addWidget(as);

  Manager.addWidget(new AjaxFranceLabs.SearchInformationWidget({
    elm : $('#search_information'),
    id : 'searchInformation'
  }));

  if (window.isLikesAndFavoritesEnabled)
    Manager.addWidget(new AjaxFranceLabs.LikesAndFavoritesWidget({
      /* List of repo sources for which the open folder link must be displayed */
      openFolderSources: []
    }));
  else
    Manager.addWidget(new AjaxFranceLabs.SubClassResultWidget({
      /* List of repo sources for which the open folder link must be displayed */
      openFolderSources: []
    }));
  // Manager.addWidget(new AjaxFranceLabs.ExternalResultWidget());

  Manager.addWidget(new AjaxFranceLabs.PromolinkWidget({
    elm : $('#promolink'),
    id : 'promolink'
  }));

  Manager.addWidget(new AjaxFranceLabs.SpellcheckerWidget({
    elm : $('#spellchecker'),
    id : 'spellchecker'
  }));

  Manager.store.addByValue('facet', true);

  for (var i = 0; i < widgetIdsToRemove.length; i++) {
    var widgetId = widgetIdsToRemove[i];
    Manager.removeWidget(widgetId);
  }

  buildCustomWidgets();

  $.when(Manager.init()).then(function() {

    var query = getParamValue('query', decodeURIComponent(window.location.search));
    var searchType = getParamValue('searchType', window.location.search);
    var sortType = getParamValue('sortType', window.location.search);
    var category = getParamValue('category', window.location.search);

    Manager.store.addByValue("id", UUID.generate());

    if (searchType === '') {
      searchType = 'allWords';
    }
    if (sortType === '') {
      sortType = 'score';
    }
    $('#searchBar').find('.searchBar input[type=text]').val(query);
    if (category != "") {
      self.manager.store.addByValue("category", category);
    }

    /*
     * atLeastOneWord allWords exactExpression
     */
    var radios = $('#searchBar').find('.searchMode input[type=radio]');

    $.each(radios, function(key, radio) {
      if (radio.value === searchType) {
        radio.checked = true;
      }
    });

    var q = window.location.search.substring(1);
    var n = q.indexOf("request");
    if (n != -1) {
      var request = decodeURIComponent(q.substring(n + 8, q.length));
      var args = request.split("&");
      for (var i = 0; i < args.length; i++) {
        var paramName = args[i].substring(0, args[i].indexOf("="));
        var paramValue = decodeURI(args[i].substring(args[i].indexOf("=") + 1));
        Manager.store.addByValue(paramName, paramValue);

        if (paramName == "q") {
          $('#searchBar').find('.searchBar input[type=text]').val(paramValue);
        }
      }
    }

    // Retrieve specific query conf for current user if exists
    // Then apply it (or not) and execute query
    $.ajax({
      url : "./GetUserQueryConf",
      success : function(data) {
        if (data.query_conf != undefined && data.query_conf != "") {
          var query_conf = JSON.parse(data.query_conf);
          if (query_conf.qf != undefined && query_conf.qf != "") {
            Manager.store.addByValue("qf", query_conf.qf);
          } else {
            Manager.store.remove("qf");
          }
          if (query_conf.pf != undefined && query_conf.pf != "") {
            Manager.store.addByValue("pf", query_conf.pf);
          } else {
            Manager.store.remove("pf");
          }
        }
        Manager.makeRequest();
      },
      dataType : "json",
      error : function() {
        Manager.makeRequest();
      }
    });

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

function getParamValue(param, url) {
  var u = url == undefined ? document.location.href : url;
  var reg = new RegExp('(\\?|&|^)' + param + '=(.*?)(&|$)');
  matches = u.match(reg);
  if (matches === null)
    return '';
  return matches[2] != undefined ? decodeURIComponent(matches[2]).replace(/\+/g, ' ') : '';
}

function replaceAll(str, find, replace) {
  return str.replace(new RegExp(find, 'g'), replace);
}

function createCookie(name, value, days) {
  if (days) {
    var date = new Date();
    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
    var expires = "; expires=" + date.toGMTString();
  } else
    var expires = "";
  document.cookie = name + "=" + value + expires + "; path=/";
}

function readCookie(name) {
  var nameEQ = name + "=";
  var ca = document.cookie.split(';');
  for (var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ')
      c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) == 0)
      return c.substring(nameEQ.length, c.length);
  }
  return null;
}

function eraseCookie(name) {
  createCookie(name, "", -1);
}
function formatDate(date_string, format) {
  var date_to_convert = new Date(date_string);
  var date = date_to_convert.toLocaleDateString(format);
  return date;
}

function nFormatter(num, digits) {
  var si = [ {
    value : 1,
    symbol : ""
  }, {
    value : 1E3,
    symbol : "k"
  }, {
    value : 1E6,
    symbol : "M"
  }, {
    value : 1E9,
    symbol : "G"
  }, {
    value : 1E12,
    symbol : "T"
  }, {
    value : 1E15,
    symbol : "P"
  }, {
    value : 1E18,
    symbol : "E"
  } ];
  var rx = /\.0+$|(\.[0-9]*[1-9])0+$/;
  var i;
  for (i = si.length - 1; i > 0; i--) {
    if (num >= si[i].value) {
      break;
    }
  }
  return (num / si[i].value).toFixed(digits).replace(rx, "$1") + si[i].symbol;
}
