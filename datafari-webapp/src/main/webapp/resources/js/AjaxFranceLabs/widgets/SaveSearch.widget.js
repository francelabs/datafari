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
AjaxFranceLabs.SaveSearchWidget = AjaxFranceLabs.AbstractWidget.extend({

  // Variables
  saveButtonElm : "#save_search_label",
  saveSearchPopoverElm : "#save-search-popover",

  // Methods
  buildWidget : function() {
    var self = this;
    $(self.saveButtonElm).html(window.i18n.msgStore['save_search_button']);
    $(self.saveSearchPopoverElm).html(
        "<div id='save-search-form'><input type='text' id='requestName' placeholder='" + window.i18n.msgStore['searchName_placeholder']
            + "'><button id='save_search_button' type='button'>OK</button><button id='cancel_save_search_button' type='button'>" + window.i18n.msgStore['cancel']
            + "</button></div><div id='save_result' style='display:none;'></div>");
    $("#save_search_button").click(function() {
      self.saveSearchFunc();
    });
    $("#cancel_save_search_button").click(function() {
      self.clean();
    });
    $(self.saveButtonElm).popover({
      html : true,
      content : $(self.saveSearchPopoverElm),
      animation : false
    });
    $(self.saveButtonElm).click(function() {
      $('#requestName').val("");
      $("#save-search-popover").show();
    });
    // Keep header menu open when the user clicks in the popover content
    $(self.saveSearchPopoverElm).click(function(event) {
      event.stopPropagation();
    });
    // Close the popover when the user clicks outside it
    $("html").on('click', function(e) {
      // the 'is' for buttons that trigger popups
      // the 'has' for icons within a button that triggers a popup
      if (!$(self.saveButtonElm).is(e.target) && $(self.saveButtonElm).has(e.target).length === 0 && $('.popover').has(e.target).length === 0) {
        (($(self.saveButtonElm).popover('hide').data('bs.popover') || {}).inState || {}).click = false // fix for BS 3.3.6
        $(self.saveButtonElm).removeClass("active-header-link");
      }
    });
    var currentParrentDropdown = $(self.saveButtonElm).parents("li.nav-item.dropdown").children("a");
    $("li.nav-item.dropdown").children("a").click(function() {
      if ($(this) !== currentParrentDropdown || $(this).parent().hasClass("show")) {
        (($(self.saveButtonElm).popover('hide').data('bs.popover') || {}).inState || {}).click = false // fix for BS 3.3.6
        $(self.saveButtonElm).removeClass("active-header-link");
      }
    });
  },

  saveSearchFunc : function() {
    var self = this;
    var query = "";
    var fqs = self.manager.store.values("fq");
    for (var i = 0; i < fqs.length; i++) {
      query += "fq=" + fqs[i] + "&";
    }
    var name = $("#requestName").val();
    query += "q=" + self.manager.store.values("q")[0];
    $.post('./saveSearch', {
      "name" : name,
      "query" : query
    }, function(data) {
      // self.elm.html("<div id='save_result'></div>");
      if (data.code == 0) {
        $("#save_result").html(window.i18n.msgStore['saved']);
        $("#save_result").addClass("success");
      } else {
        $("#save_result").html(window.i18n.msgStore['error']);
        $("#save_result").addClass("fail");
      }
      $("#save-search-form").hide();
      $("#save_result").show();
      $("#save_result").fadeOut(1500, function() {
        self.clean();
        // Trigger click on the admin dropdown menu button to make it disappear
        $(self.saveButtonElm).parents("li.nav-item.dropdown").children("a").trigger("click");
      });
    }, "json");
  },

  clean : function() {
    var self = this;
    $('#requestName').val("");
    $("#save_result").html("");
    $("#save_result").hide();
    $("#save-search-form").show();
    $(self.saveButtonElm).popover("hide");
    $(self.saveButtonElm).removeClass("active-header-link");
    $(self.saveSearchPopoverElm).hide();
  }
});
