/*******************************************************************************************************************************************
 * Copyright 2015 - 2016 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************************************************************************/

AjaxFranceLabs.SearchBarWidget = AjaxFranceLabs.AbstractWidget.extend({

  // Variables

  autocomplete : false,

  noRequest : false,

  autocompleteOptions : {
    optionsExtend : true,
    render : null,
    field : null,
    valueSelectFormat : function(value) {
      return value;
    },
    singleValue : false,
    openOnFieldFocus : false
  },

  updateBrowserAddressBar : true,

  removeContentButton : false,

  type : 'searchBar',

  activateAdvancedSearchLink : false,

  // Methods

  updateAddressBar : function() {
    if (this.updateBrowserAddressBar == true) {
      var searchType = 'allWords';
      var radios = $('#searchBar').find('.searchMode input[type=radio]');
      $.each(radios, function(key, radio) {
        if (radio.checked) {
          searchType = radio.value;
        }
      });
      var query = $('.searchBar input[type=text]').val();

      var source = "";
      if ($(".selectSources").val() != undefined && $(".selectSources").val() != null && $(".selectSources").val() != "") {
        source = "&source=" + $(".selectSources").val();
      }

      var category = "";
      if ($("#categories-select").val() != undefined && $("#categories-select").val() != null && $("#categories-select").val() != "") {
        category = "&category=" + $("#categories-select").val();
      }

      var entitiesQuery = "";
      if ($(".entities-highlight-content").html() != undefined && $(".entities-highlight-content").html() != null && $(".entities-highlight-content").html() != "") {
        entitiesQuery = "&entQ=" + $(".entities-highlight-content").html();
      }

      window.history.pushState('Object', 'Title', window.location.pathname + '?searchType=' + searchType + '&query=' + query + '&lang=' + window.i18n.language + source + category + entitiesQuery);

      Manager.store.addByValue("id", UUID.generate());
    }
  },

  clean : function() {
    this.manager.store.remove('start');
    this.manager.store.remove('fq');
    $.each(this.manager.widgets, function(index, widget) {
      if (typeof widget.pagination !== "undefined") {
        widget.pagination.pageSelected = 0;
      }
    });

  },

  buildWidget : function() {
    var self = this, elm = $(this.elm);
    $("#basicSearchLink").html(window.i18n.msgStore['search']);
    $("#basicSearchLink").click(function() {
      self.initBasicSearchUI();
    });
    $("#basicSearchLink").addClass("active");

    elm.addClass('searchBarWidget').addClass('widget').append('<div id="searchBarContent" class="search-view-header"></div>');
    $("#results_action").append('<span id="sortMode" class="pull-right"></div>');
    elm.find('#searchBarContent').append('<div class="searchBar"></div>');

    // Add categories select
    // elm.find('.searchBar').append('<select id="categories-select"><option value="">' + window.i18n.msgStore["allCategories"] +
    // '</option></select>');

    // Add the hidden div that will have the same shape and css properties as the input search bar to simulate highlighting on terms
    elm.find('.searchBar').append('<div class="entities-highlight search-input"><div class="entities-highlight-content"></div></div>');

    elm.find('.searchBar').append('<input class="search-input" type="text" autocorrect="off" autocapitalize="off"/>').append(
        '<div id="search-btn" class="search bc-color"><i class="fas fa-search"></i></div>');

    // Fill entities query if available
    var entitiesQuery = getParamValue('entQ', window.location.search);
    if (entitiesQuery != undefined && entitiesQuery != null && entitiesQuery != "") {
      $(".entities-highlight-content").html(entitiesQuery);
      self.manager.store.addByValue("entQ", entitiesQuery);
    }

    // Fill categories select
    // $.get("GetEntitySuggesters", function(data) {
    // if (data.code == 0 && data.entitySuggesters != undefined && data.entitySuggesters != null && data.entitySuggesters != "" &&
    // data.activated === true) {
    // var entitySuggesters = JSON.parse(data.entitySuggesters);
    // for (var i = 0; i < entitySuggesters.length; i++) {
    // var entitySuggester = entitySuggesters[i];
    // $("#categories-select").append("<option value='" + entitySuggester.categoryKey + "'>" +
    // window.i18n.msgStore[entitySuggester.categoryi18nKey] + "</option>");
    // }
    // var category = getParamValue('category', window.location.search);
    // $("#categories-select").val(category);
    // $("#categories-select").show();
    // elm.find('.searchBar input[type=text]').removeClass("categories-off");
    // elm.find('.searchBar input[type=text]').addClass("categories-on");
    // } else {
    // $("#categories-select").hide();
    // elm.find('.searchBar input[type=text]').removeClass("categories-on");
    // elm.find('.searchBar input[type=text]').addClass("categories-off");
    // }
    // }, "json");
    // $("#categories-select").change(function() {
    // self.manager.store.remove("category");
    // var value = $(this).val();
    // if (value != "") {
    // self.manager.store.addByValue("category", value)
    // }
    // });

    if (this.removeContentButton)
      elm.find('.searchBar').append('<span class="removeContent"></span>').find('.removeContent').css('display', 'none').append('<span>X</span>').click(function() {
        elm.find('.searchBar input[type=text]').val('');
        $(this).css('display', 'none');
        self.makeRequest();
      });

    elm.find('.searchMode').append('<div></div>').append('<div></div>').append('<div></div>').append('<div></div>');
    elm.find('.searchMode div').attr('style', 'display: inline').append('<input type="radio" name="searchType" class="radio" />').append('<label></label>');
    elm.find('.searchMode div:eq(0)').find('input').attr('value', 'allWords').attr('checked', 'true').attr('id', 'allWords').parent().find('label').attr('for', 'allWords').append(
        '<span>&nbsp;</span>').append(window.i18n.msgStore['allWords']);
    elm.find('.searchMode div:eq(1)').find('input').attr('value', 'atLeastOneWord').attr('id', 'atLeastOneWord').parent().find('label').attr('for', 'atLeastOneWord').append('<span>&nbsp;</span>')
        .append(window.i18n.msgStore['atLeastOneWord']);
    elm.find('.searchMode div:eq(2)').find('input').attr('value', 'exactExpression').attr('id', 'exactExpression').parent().find('label').attr('for', 'exactExpression').append('<span>&nbsp;</span>')
        .append(window.i18n.msgStore['exactExpression']);

    elm.find('.searchBar input[type=text]').keypress(function(event) {
      if (event.keyCode === 13) {
        if (self.autocomplete)
          self.elm.find('.searchBar input[type=text]').autocomplete("close");
        self.makeRequest();
      }
    }).keyup(function() {
      if (self.removeContentButton) {
        if (AjaxFranceLabs.empty(elm.find('.searchBar input[type=text]').val()))
          elm.find('.searchBar .removeContent:visible').css('display', 'none');
        else
          elm.find('.searchBar .removeContent:hidden').css('display', 'block');
      }
    });
    elm.find('.searchBar .search').click(function() {
      self.makeRequest();

    });
    if (this.autocomplete === true) {
      this.autocompleteOptions.elm = elm.find('.searchBar input[type=text]');
      this.autocomplete = new AjaxFranceLabs.AutocompleteModule(this.autocompleteOptions);
      this.autocomplete.manager = this.manager;
      this.autocomplete.init();
    } else if (this.autocomplete) {
      this.autocomplete.elm = elm.find('.searchBar input[type=text]');
      this.autocomplete.manager = this.manager;
      this.autocomplete.init();
    }
    var sortModeDiv = document.getElementById("sortMode");
    if (sortModeDiv != undefined) {
      var textSort = document.createTextNode(window.i18n.msgStore['sortType']);
      sortModeDiv.appendChild(textSort);
      var selectList = document.createElement("select");
      selectList.id = "mySelect";
      selectList.onchange = function() {
        self.manager.makeRequest();
      };
      sortModeDiv.appendChild(selectList);

      var optionScore = document.createElement("option");
      optionScore.setAttribute("value", "score");
      optionScore.setAttribute("selected", "");
      optionScore.text = window.i18n.msgStore['score'];

      selectList.appendChild(optionScore);

      var optionDate = document.createElement("option");
      optionDate.setAttribute("value", "date");
      optionDate.text = window.i18n.msgStore['date'];
      selectList.appendChild(optionDate);
    }

    if (this.activateAdvancedSearchLink) {

      $(window).ready(function() {
        if (window.location.hash === "#advancedsearch") {
          $('#advancedSearchLink').click();
        }
      });

      $('#advancedSearchLink').click(function(event) {

        // Hide other UIs
        hideSearchView();
        clearActiveLinks();
        $("#dropdown-search-tools").addClass("active");

        destroyDatatables();

        // Reset the widget status (radios, entered text, ...)
        self.manager.getWidgetByID('advancedSearch').reset();

        // Initialize the advancedSearch UI
        self.manager.getWidgetByID('advancedSearch').buildStartingUI();

        // Display the advanced search
        $('#advancedSearch').removeClass('force-hide');

        // Perform a "select all" request: *:*
        // self.manager.makeDefaultRequest();

        // Prevent page reload
        event.preventDefault();
      });

      $('#advancedSearchLinkMobile').click(function(event) {

        // Hide other UIs
        hideSearchView();
        clearActiveLinks();
        $("#dropdown-search-tools").addClass("active");

        destroyDatatables();

        // Reset the widget status (radios, entered text, ...)
        self.manager.getWidgetByID('advancedSearch').reset();

        // Initialize the advancedSearch UI
        self.manager.getWidgetByID('advancedSearch').buildStartingUI();

        // Display the advanced search
        $('#advancedSearch').removeClass('force-hide');

        // Perform a "select all" request: *:*
        // self.manager.makeDefaultRequest();

        // Prevent page reload
        event.preventDefault();
      });
    }
  },

  beforeRequest : function() {

    /*
     * Check if we are executing a synthetic request, built by spellChecker widget after a misspelled search term or not. If it is a
     * "normal" request, update the Q parameter with the value from searchBar and do other useful stuff. Else, it is a "synthetic" request;
     * do nothing as the spellchecker itself has already replaced the Q parameter with the corrected search term.
     */
    if (this.manager.store.isParamDefined('original_query') !== true) {

      var search = (AjaxFranceLabs.empty(AjaxFranceLabs.trim($(this.elm).find('.searchBar input').val()))) ? '*:*' : AjaxFranceLabs.trim($(this.elm).find('.searchBar input').val());
      // Make spellchecker case insensitive
      // Manager.store.addByValue("spellcheck.q", search);
      var testSelect = document.getElementById("mySelect");
      if (testSelect.options[testSelect.selectedIndex].value == 'date') {
        Manager.store.addByValue("sort", 'last_modified desc');
      } else {
        Manager.store.addByValue("sort", 'score desc');
      }

      if (this.autocomplete)
        search = search.replace(/\u200c/g, '');
      search = search.replace(' in People', '');
      switch ($(this.elm).find('input[name=searchType]:checked').val()) {
      case "allWords":
        Manager.store.addByValue("q.op", 'AND');
        Manager.store.addByValue("spellcheck.collateParam.q.op", 'AND');
        break;
      case "atLeastOneWord":
        Manager.store.addByValue("q.op", 'OR');
        Manager.store.addByValue("spellcheck.collateParam.q.op", 'OR');
        break;
      case 'exactExpression':
        search = '"' + search + '"';
        break;
      default:
        Manager.store.addByValue("q.op", 'AND');
        Manager.store.addByValue("spellcheck.collateParam.q.op", 'AND');
        break
      }

      this.manager.store.get('q').val(search);

    }

  },

  afterRequest : function() {
    /*
     * It appeared that sometimes when making a request, the autocomplete was displayed, this part of the code has been made to counter this
     * little glitch, after a request, we close the autocomplete
     */
    if (this.autocomplete) {
      this.elm.find('.searchBar input[type=text]').autocomplete("close");
    }
  },

  makeRequest : function() {
    if (!this.noRequest) {
      this.initBasicSearchUI();
      this.clean();
      this.updateAddressBar();
      this.manager.makeRequest();
    }

  },

  initBasicSearchUI : function() {
    if (typeof destroyDatatables !== "undefined") {
      destroyDatatables();
    }
    displaySearchView();
    clearActiveLinks();
    $("#basicSearchLink").addClass("active");
  },

  /**
   * Resets the inputs of the widget
   */
  reset : function() {
    var elm = $(this.elm);
    elm.find('input[type="text"]').val('');
    elm.find('input#allWords').prop('checked', true);
  }

});
