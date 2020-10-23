/*******************************************************************************************************************************************
 * Copyright 2015 France Labs
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
AjaxFranceLabs.RelevancyQueryModule = AjaxFranceLabs.AbstractModule.extend({

  // Variables
  parentWidget : "",
  isMobile : $(window).width() < 800,
  currentRelevancyQuery : null,
  relevantDocsList : null,
  createRelevancyButton : "#create-relevancy",
  relevancyPopoverElm : "#create-relevancy-popover",
  relevancyDivElm : "#relevancy-div",

  init : function() {
    $(this.createRelevancyButton).html(window.i18n.msgStore['relevancy_button']);
    var self = this;
    $(self.relevancyPopoverElm).html(
        "<input type='text' id='relevancyQueryName' placeholder='" + window.i18n.msgStore['relevancyQueryName_placeholder']
            + "'><button id='create_relevancy_button' type='button'>OK</button><button id='cancel_relevancy_create_button' type='button'>" + window.i18n.msgStore['cancel'] + "</button>");
    $("#create_relevancy_button").click(function() {
      self.addGlobalLinks();
    });
    $("#cancel_relevancy_create_button").click(function() {
      self.cleanPopover();
    });
    $(self.relevancyDivElm).html(
        "<div id='relevancy-save-form'><p>" + window.i18n.msgStore['relevancy_expl'] + "</p><br><button id='save-relevancy-setup'>" + window.i18n.msgStore['save']
            + "</button><button id='cancel_relevancy_save_button' type='button'>" + window.i18n.msgStore['cancel'] + "</button></div><div id='relevancy_save_result' style='display:none;'></div>");
    $("#cancel_relevancy_save_button").click(function() {
      self.cleanGlobalLinks();
    });
    $(self.createRelevancyButton).popover({
      html : true,
      content : $(self.relevancyPopoverElm),
      animation : false
    });
    $(self.createRelevancyButton).click(function() {
      $('#relevancyQueryName').val("");
      $(self.relevancyPopoverElm).show();
    });
    // Keep admin submenu open when the user clicks on the create relevancy query button
    // Keep open dropdown submenus (that belong to the keep-down css class) when clicking inside
    $(document).on('click.bs.dropdown.data-api', self.createRelevancyButton, function(e) {
      e.stopPropagation();
    });
    // Keep header menu open when the user clicks in the popover content
    $(self.relevancyPopoverElm).click(function(event) {
      event.stopPropagation();
    });
    // Close the popover when the user clicks outside it
    $("html").on('click', function(e) {
      // the 'is' for buttons that trigger popups
      // the 'has' for icons within a button that triggers a popup
      if (!$(self.createRelevancyButton).is(e.target) && $(self.createRelevancyButton).has(e.target).length === 0 && $('.popover').has(e.target).length === 0) {
        (($(self.createRelevancyButton).popover('hide').data('bs.popover') || {}).inState || {}).click = false // fix for BS 3.3.6
        $(self.createRelevancyButton).removeClass("active-header-link");
      }
    });
    var currentParrentDropdown = $(self.createRelevancyButton).parents("li.nav-item.dropdown").children("a");
    $("li.nav-item.dropdown").children("a").click(function() {
      if ($(this) !== currentParrentDropdown || $(this).parent().hasClass("show")) {
        (($(self.createRelevancyButton).popover('hide').data('bs.popover') || {}).inState || {}).click = false // fix for BS 3.3.6
        $(self.createRelevancyButton).removeClass("active-header-link");
      }
    });
  },

  cleanPopover : function() {
    var self = this;
    $('#relevancyQueryName').val("");
    $(self.createRelevancyButton).popover("hide");
    $(self.createRelevancyButton).removeClass("active-header-link");
    $(self.relevancyPopoverElm).hide();
  },

  setParentWidget : function(widget) {
    this.parentWidget = widget;
  },

  // Methods

  beforeRequest : function() {
    var self = this;
    var query = self.manager.store.params.q.value;
    if (query === self.currentRelevancyQuery) {
      $.get('./SearchExpert/queryRelevancy', {
        "query" : query
      }, function(data) {
        self.relevantDocsList = data.relevantDocsList;
      }, "json");
    } else {
      self.cleanGlobalLinks();
    }
  },

  afterRequest : function() {

  },

  addGlobalLinks : function() {
    var self = this;
    var query = self.manager.store.params.q.value;
    self.currentRelevancyQuery = query;
    var name = $("#relevancyQueryName").val();
    $.post('./SearchExpert/queryRelevancy', {
      "action" : "create",
      "name" : name,
      "query" : query
    }, function(data) {
      self.cleanPopover();
      // Trigger click on the admin dropdown menu button to make it disappear
      $(self.createRelevancyButton).parents("li.nav-item.dropdown").children("a").trigger("click");
      $(self.relevancyDivElm).show();
      $(".relevant-add").show();
      $(".relevant-remove").show();
      $("#save-relevancy-setup").click(function() {
        $.post('./SearchExpert/queryRelevancy', {
          "action" : "save"
        }, function(data) {
          if (data.code == 0) {
            $("#relevancy_save_result").html(window.i18n.msgStore['saved']);
            $("#relevancy_save_result").addClass("success");
          } else {
            $("#relevancy_save_result").html(window.i18n.msgStore['error']);
            $("#relevancy_save_result").addClass("fail");
          }
          $("#relevancy-save-form").hide();
          $("#relevancy_save_result").show();
          $("#relevancy_save_result").fadeOut(1500, function() {
            self.cleanGlobalLinks();
          });
        }, "json");
      });
    }, "json");

  },

  cleanGlobalLinks : function() {
    var self = this;
    self.currentRelevancyQuery = null;
    self.relevantDocsList = null;
    $(".relevant-add").hide();
    $(".relevant-remove").hide();
    $(".relevant-ok").remove();
    $(self.relevancyDivElm).hide();
  },

  addRelevancyLinks : function(resultElm, id, query) {
    var self = this;
    var style = "style='display: none;";
    if (self.currentRelevancyQuery != null) {
      style = "style='";
    }
    if (window.isLikesAndFavoritesEnabled) {
      style += " margin-right: 2em;"
    }
    style += "'";

    if (!this.isMobile) {

      // Add the 'add' and 'remove' buttons and set their onClick function
      resultElm.find(".title")
          .after("<span class='relevant-remove' " + style + " action='remove' id='" + id + "'></span><span class='relevant-add' " + style + " action='add' id='" + id + "'></span>");

      if (self.relevantDocsList != null && self.relevantDocsList != undefined && $.inArray(id, self.relevantDocsList) != -1) {
        resultElm.find(".relevant-add").after("<span class='relevant-ok'></span>");
        document.getElementById(id).getElementsByClassName("relevant-add")[0].style.display = "none";
      }
      resultElm.find(".relevant-add").click(function() {

        $.post("./SearchExpert/queryRelevancy", {
          item : $(this).attr('id'),
          query : self.manager.store.params.q.value,
          action : $(this).attr('action')
        }, function(data) {
          // If successful, reload the Solr core and refresh the searchView
          if (data.code == 0) {
            resultElm.find(".relevant-add").after("<span class='relevant-ok'></span>");
            document.getElementById(id).getElementsByClassName("relevant-add")[0].style.display = "none";

          } else {
          }
        })
      });

      // 'remove' button onClick function
      resultElm.find(".relevant-remove").click(function() {

        // Send the POST request to remove the selected doc from the elevate list
        $.post("./SearchExpert/queryRelevancy", {
          item : $(this).attr('id'),
          query : self.manager.store.params.q.value,
          action : $(this).attr('action')
        }, function(data) {
          if (data.code == 0) {
            resultElm.find(".relevant-add").show();
            resultElm.find(".relevant-ok").remove();
          } else {
          }
        }, "json")
      });

    }
  }
});
