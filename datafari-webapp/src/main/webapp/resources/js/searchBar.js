$(function($) {

  Manager.addWidget(new AjaxFranceLabs.SearchBarWidget({
    elm : $('#searchBar'),
    id : 'searchBar',
    autocomplete : true,
    noRequest : true,
    updateBrowserAddressBar : false
  }));

  // Manager.addModule(new AjaxFranceLabs.AutocompleteModule({
  // elm : $('.searchBar input[type=text]'),
  // searchForItSelf : true
  // }));

  Manager.init();

  $('.search').click(search);
  $('.search i').click(search);
  $('.searchBar input[type=button]').click(search);
  $('.searchBar input[type=text]').keypress(function(event) {
    if (event.keyCode === 13) {
      search();
    }
  });
});

function getParamValue(param, url) {
  var u = url == undefined ? document.location.href : url;
  var reg = new RegExp('(\\?|&|^)' + param + '=(.*?)(&|$)');
  matches = u.match(reg);
  if (matches === null)
    return '';
  return matches[2] != undefined ? decodeURIComponent(matches[2]).replace(/\+/g, ' ') : '';
};

function search() {

  var searchType = 'allWords';
  var radios = $('#searchBar').find('.searchMode input[type=radio]');
  $.each(radios, function(key, radio) {
    if (radio.checked) {
      searchType = radio.value;
    }
  });

  var category = "";
  if ($("#categories-select").val() != undefined && $("#categories-select").val() != null && $("#categories-select").val() != "") {
    category = "&category=" + $("#categories-select").val();
  }

  var entitiesQuery = "";
  if ($(".entities-highlight-content").html() != undefined && $(".entities-highlight-content").html() != null && $(".entities-highlight-content").html() != "") {
    entitiesQuery = "&entQ=" + $(".entities-highlight-content").html();
  }

  window.open('Search?searchType=' + searchType + '&query=' + encodeURIComponent($('.searchBar input[type=text]').val()) + '&lang=' + window.i18n.language + category + entitiesQuery, '_self');
};
