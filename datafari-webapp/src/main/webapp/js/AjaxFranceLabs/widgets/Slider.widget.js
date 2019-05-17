/*****************************************************************************************************************************************************
 * Copyright 2015 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 ****************************************************************************************************************************************************/
AjaxFranceLabs.SliderWidget = AjaxFranceLabs.AbstractFacetWidget.extend({

  // Variables

  name : null,

  field : null,

  type : 'slider',

  elm : null,

  range : false,

  currentFilter : '',

  min : -100,

  max : 100,

  unit : 'YEAR',

  fieldType : 'date',

  defaultValue : 0,

  step : 1,

  comparator : 'greater',

  // Methods

  buildWidget : function() {
    var endAnimationEvents = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
    var animation = 'animated rotateIn';
    var self = this, elm = $(this.elm);
    elm.hide();
    elm.addClass('facet').addClass('tableWidget').addClass('widget').attr('widgetId', this.id);
    elm.append('<ul></ul>');
    var ul = elm.find('ul');
    if (this.range) {
      ul.append('<li><div id="fromDiv">' + window.i18n.msgStore['fromLabel'] + ': <span id="from"></span></div></li>');
      ul.append('<li><div id ="toDiv">' + window.i18n.msgStore['toLabel'] + ': <span id="to"></span></div></li>');
    } else {
      ul.append('<li><div id="currentFilter"></div></li>');
    }
    ul.append('<li><div id="' + this.id + '" class="slider"></div></li>');
    if (this.name != null) {
      elm.prepend('<div class="facetName">').find('.facetName').append('<i class="fas fa-chevron-down"></i>').append('<span class="label la"></span>')
          .find('.label.la').append(this.name);
      elm.find('.facetName').toggle(function() {
        $("#" + self.id).hide();
        elm.find(".facetName i").removeClass('fa-chevron-down').addClass('fa-chevron-up ' + animation).on(endAnimationEvents, function() {
          $(this).removeClass(animation);
        });
      }, function() {
        $("#" + self.id).show();
        elm.find(".facetName i").removeClass('fa-chevron-up').addClass(animation + ' fa-chevron-down').on(endAnimationEvents, function() {
          $(this).removeClass(animation);
        });
      });
    }
    $("#" + this.id).slider({
      range : self.range,
      change : function(event, ui) {
        self.sliderChanged();
      },
      min : self.min,
      max : self.max,
      step : self.step,
      slide : function(event, ui) {
        self.changeFilterValue(ui, true);
      }
    });

    if (this.range) {
      $("#" + self.id).slider("option", "values", [ this.min, this.max ]);
    } else {
      $("#" + self.id).slider("option", "value", this.defaultValue);
    }

  },

  update : function() {
    var self = this, elm = $(this.elm);
    elm.show(); // show the widget

  },

  changeFilterValue : function(slider, slide) {
    if (this.range) {
      var selectedMin;
      var selectedMax;
      if (!slide) {
        selectedMin = slider.slider("values", 0);
        selectedMax = slider.slider("values", 1);
      } else {
        selectedMin = slider.values[0];
        selectedMax = slider.values[1];
      }
      var sentenceMin = selectedMin;
      var sentenceMax = selectedMax;
      if (this.fieldType == "date") {
        sentenceMin = "NOW " + this.getSign(selectedMin) + selectedMin + " " + this.unit + this.getPlurial(selectedMin);

        sentenceMax = "NOW " + this.getSign(selectedMax) + selectedMax + " " + this.unit + this.getPlurial(selectedMax);
      }

      this.elm.find("#from").html(sentenceMin);
      this.elm.find("#to").html(sentenceMax);
    } else {
      var value;
      if (!slide) {
        value = slider.slider("value");
      } else {
        value = slider.value;
      }
      if (this.fieldType == "date") {
        if (this.comparator == "greater") {
          this.elm.find("#currentFilter").html("Greater than NOW " + this.getSign(value) + value + " " + this.unit + this.getPlurial(value));
        } else {
          this.elm.find("#currentFilter").html("Less than NOW " + this.getSign(value) + value + " " + this.unit + this.getPlurial(value));
        }
      } else {
        if (this.comparator == "greater") {
          this.elm.find("#currentFilter").html("Greater than " + value);
        } else {
          this.elm.find("#currentFilter").html("Less than " + value);
        }
      }
    }
  },

  getFilter : function() {
    this.changeFilterValue($("#" + this.id), false);
    if (this.range) {
      var selectedMin = $("#" + this.id).slider("values", 0);
      var selectedMax = $("#" + this.id).slider("values", 1);
      if (this.fieldType == "date") {
        return this.getDateRangeFilter(selectedMin, selectedMax);
      } else {
        return this.getNormalRangeFilter(selectedMin, selectedMax);
      }
    } else {
      var selectedValue = $("#" + this.id).slider("value");
      if (this.fieldType == "date") {
        return this.getDateFilter(selectedValue);
      } else {
        return this.getNormalFilter(selectedValue);
      }
    }
  },

  getDateFilter : function(value) {
    var filter;
    var sign = this.getSign(value);
    if (this.comparator == "greater") {
      filter = this.field + ":[NOW" + sign + value + this.unit + " TO *]";
    } else {
      filter = this.field + ":[* TO NOW" + sign + value + this.unit + "]";
    }
    return filter;
  },

  getNormalFilter : function(value) {
    var filter;
    if (this.comparator == "greater") {
      filter = this.field + ":[" + value + " TO *]";
    } else {
      filter = this.field + ":[* TO " + value + "]";
    }
    return filter;
  },

  getDateRangeFilter : function(minValue, maxValue) {
    var filter;
    var minSign = this.getSign(minValue);
    var maxSign = this.getSign(maxValue);
    filter = this.field + ":[NOW" + minSign + minValue + this.unit + " TO NOW" + maxSign + maxValue + this.unit + "]";
    return filter;

  },

  getNormalRangeFilter : function(minValue, maxValue) {
    var filter;
    var minSign = this.getSign(minValue);
    var maxSign = this.getSign(maxValue);
    filter = this.field + ":[" + minValue + " TO " + maxValue + "]";
    return filter;

  },

  sliderChanged : function() {
    var filter = this.getFilter();

    if (this.currentFilter != '') {
      this.manager.store.removeByValue("fq", this.currentFilter);
    }
    this.manager.store.addByValue("fq", filter);
    this.currentFilter = filter;

    var testSelect = document.getElementById("mySelect");
    // Statement needed to detect if we are in the buildWidget function
    // If testSelect is undefined so we are in the buildWidget function and we don't need to force a makeRequest as it will be automatically triggered
    // Plus if we are in the buildWidget function, the makeRequest will trigger an error and the searchView will not work
    if (testSelect != undefined) {
      this.manager.makeRequest();
    }
  },

  getSign : function(value) {
    if (value < 0) {
      return "";
    } else {
      return "+";
    }
  },

  getPlurial : function(value) {
    var plurial = "";
    if (value < -1 || value > 1) {
      plurial = "S";
    }
    return plurial;
  },

  clickHandler : function() {
  },

  afterRequest : function() {
    this.update();
  }
});
