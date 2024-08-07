$(function($) {

  // Setup languages
  window.i18n.setLanguageUrl('/Datafari/resources/js/AjaxFranceLabs/locale/');
  window.i18n.setCustomLanguageUrl('/Datafari/resources/customs/i18n/');
  window.i18n.setupLanguage('Datafari doc preview page');
  var languages = [ 'en' ];
  if (typeof langHeader !== 'undefined') {
    languages = langHeader
  }

  $.get('/Datafari/applyLang?lang=' + window.i18n.language, function(data) {
    if (data.code == 0 && data.lang != window.i18n.language) {
      window.location.replace("/Datafari/applyLang?urlRedirect=" + encodeURIComponent(window.location.href));
    }
  }, "json");

  new AjaxFranceLabs.LanguageSelectorWidget({
    // Take the languageSelector element by ID.
    languages : languages,
    elm : $('#languageSelector'),
    id : 'languageSelector'
  }).buildWidget();

  // Setup loading div
  $("#loading-div").html("<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['preview-loading']);

  var isMobile = $(window).width() < 800;
  var manager = new AjaxFranceLabs.PreviewManager();
  var docContentDiv = $("#docContent");

  // Add widgets

  // The widget that
  // will perform
  // requests. It is a mandatory widget !
  manager.addWidget(new AjaxFranceLabs.PreviewRequestWidget({
    id : "preview-request"
  }));

  // The widget that will build the preview content div. It is a mandatory
  // widget !
  manager.addWidget(new AjaxFranceLabs.PreviewContentBuilderWidget({
    id : "preview-content-builder"
  }));

  manager.addWidget(new AjaxFranceLabs.DocInfosWidget({
    elm : $("#doc-infos-widget"),
    id : "doc-infos-widget"
  }));

  manager.addWidget(new AjaxFranceLabs.PreviewNavigationWidget({
    elm : $("#preview-nav"),
    id : "preview-nav",
    name : window.i18n.msgStore['preview-navigation']
  }));

  manager.addWidget(new AjaxFranceLabs.ShareLinksWidget({
    elm : $("#share-links"),
    id : "share-links",
    name : window.i18n.msgStore['preview-share']
  }));

  manager.addWidget(new AjaxFranceLabs.PropertiesWidget({
    elm : $("#doc-properties"),
    id : "doc-properties",
    name : window.i18n.msgStore['properties']
  }));

  manager.addWidget(new AjaxFranceLabs.QueryHighlightingWidget({
    elm : $("#query-highlighting"),
    id : "query-highlighting",
    name : window.i18n.msgStore['preview-highlight']
  }));

  // Remove widgets
  for (var i = 0; i < widgetIdsToRemove.length; i++) {
    var widgetId = widgetIdsToRemove[i];
    manager.removeWidget(widgetId);
  }

  // Build custom widgets
  buildCustomWidgets(manager);

  // init manager
  manager.init();

  function queryRequestHandler(data) {
    var rawParams = params;
    rawParams = rawParams.substring(1);
    rawParams = rawParams.replace("docId=" + docId + "&", "");
    rawParams = rawParams.replace(/docPos=[0-9]*\&/, "");
    var doc = data.response.docs[0];
    if (docPos && docPos != 0) {
      doc = data.response.docs[1];
    }
    if (doc != undefined) {
      updateUI(doc);
      // docContentDiv, docPos and qId parameters are inherited from this
      // javascript (near manager.performRequestFromQuery call)
      manager.uiUpdate(docContentDiv, doc.id, docPos, rawParams, data, qId);
    } else {
      previewError();
    }
    // Update finished, hide loading-div
    $("#loading-div").hide();
    if (doc != undefined) {
      $("#previewUI").show();
    }
  }

  function docIdRequestHandler(data) {
    var doc = data.response.docs[0];
    if (doc != undefined) {
      updateUI(doc);
      // docContentDiv parameter is inherited from this javascript
      // (near manager.performRequestFromDocId call)
      manager.uiUpdate(docContentDiv, doc.id, null, null, data, null);
    } else {
      previewError();
    }
    // Update finished, hide loading-div
    $("#loading-div").hide();
    if (doc != undefined) {
      $("#previewUI").show();
    }
  }

  function updateUI(doc) {
    manager.buildPreviewContent(doc, docContentDiv);
  }

  function previewError() {
    $("#preview-error").html('<i class="fas fa-exclamation-triangle"></i> ' + window.i18n.msgStore['preview-unavailable']);
    $("#preview-error").show();
  }

  function getQueryString() {
    var key = false, res = {}, itm = null;
    // get the query string without the ?
    var qs = window.location.search.substring(1);
    // check for the key as an argument
    if (arguments.length > 0) {
      key = arguments[0];
    }
    // make a regex pattern to grab key/value
    var pattern = /([^&=]+)=([^&]*)/g;
    // loop the items in the query string, either
    // find a match to the argument, or build an object
    // with key/value pairs
    while (itm = pattern.exec(qs)) {
      if (key !== false && decodeURIComponent(itm[1]) === key) {
        return decodeURIComponent(itm[2]);
      } else if (key === false) {
        res[decodeURIComponent(itm[1])] = decodeURIComponent(itm[2]);
      }
    }

    return key === false ? res : null;
  }

  var q = getQueryString("q");
  var docPos = getQueryString("docPos");
  if (docPos != undefined && docPos != null) {
    docPos = parseInt(docPos);
  }
  var docId = getQueryString("docId");
  var aggregator = getQueryString("aggregator");
  var qId = getQueryString("id");
  if (q != null) {
    var params = window.location.search;
    manager.performRequestFromQuery("SearchAggregator/select", params, docPos, queryRequestHandler);
  } else if (docId != undefined && docId != null) {
    manager.performRequestFromDocId("SearchAggregator/select", docId, docIdRequestHandler, aggregator);
  } else {
    // Update finished, hide loading-div
    $("#loading-div").hide();
    previewError();
  }

});
