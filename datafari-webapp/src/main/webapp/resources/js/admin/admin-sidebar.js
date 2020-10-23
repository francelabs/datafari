function getUrlVars() {
  var vars = {};
  var parts = window.location.search.replace(/[?&]+([^=&]+)=([^&]*)/gi, function(m, key, value) {
    vars[key] = value;
  });
  return vars;
}

function getUrlParam(parameter, defaultvalue) {
  var urlparameter = defaultvalue;
  if (window.location.search.indexOf(parameter) > -1) {
    urlparameter = getUrlVars()[parameter];
  }
  return urlparameter;
}

$(document).ready(function() {

  // load content
  var contentToLoad = getUrlParam("page", "");
  if (contentToLoad !== "") {
    $('#content').empty();
    $('#content').load("./ajax/" + contentToLoad + ".jsp");

    var linkElm = $("a[href=\"?page=" + contentToLoad + "\"]");
    // add active class to change css style
    $(".active-button").removeClass("active-button");
    linkElm.addClass("active-button");

    $(".active-sub-menu").removeClass("active-sub-menu");
    var ulParent = linkElm.closest("ul");
    ulParent.collapse('toggle');
    ulParent.addClass("active-sub-menu");
    ulParent.closest("li").children("a").addClass("active-button");
  }

  $("#sidebar a.ajax-link").on('click', function(e) {
    // Do not load content if already active
    if ($(this).hasClass("active-button")) {
      e.preventDefault();
    }
  });

  // add language to ajax links
  $("#sidebar a.ajax-link").each(function() {
    var href = $(this).attr("href");
    $(this).attr("href", href + "&lang=" + window.i18n.language);
  });

  // Add slideDown animation to Bootstrap dropdown when expanding.
  $('.collapse-effect').on('show.bs.collapse', function() {
    $(this).find('.collapse').first().stop(true, true).slideDown();
  });

  // Add slideUp animation to Bootstrap dropdown when collapsing.
  $('.collapse-effect').on('hide.bs.collapse', function() {
    $(this).find('.collapse').first().stop(true, true).slideUp();
  });
});