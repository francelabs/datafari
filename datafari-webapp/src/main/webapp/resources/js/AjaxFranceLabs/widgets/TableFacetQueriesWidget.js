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
AjaxFranceLabs.TableFacetQueriesWidget = AjaxFranceLabs.AbstractFacetWidget.extend({

  // Variables

  name : null,

  pagination : false,

  nbElmToDisplay : 10,

  sort : 'occurences',

  selectionType : 'ONE',

  field : null,

  maxDisplay : 40,

  checkedOnTop : true,

  type : 'tableFacetQueries',

  queries : [],

  labels : [],

  modules : [],

  // Methods

  buildWidget : function() {

    // Set the manager for the modules
    for (var i = 0; i < this.modules.length; i++) {
      this.modules[i].manager = this.manager;
    }

    var endAnimationEvents = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
    var animation = 'animated rotateIn';
    var self = this, elm = $(this.elm);
    $.each(this.queries, function(i, query) {
      self.manager.store.addByValue('facet.query', '{!key=' + encodeURI(self.labels[i]) + '}' + self.field + ':' + query);
    });
    elm.addClass('facet').addClass('TableFacetQueriesWidget').addClass('tableWidget').addClass('widget').attr('widgetId', this.id).append('<div class="facetSort"></div>').append('<ul></ul>');
    if (this.name != null) {
      elm.prepend('<div class="facetName"></div>').find('.facetName').append('<i class="fas fa-chevron-down"></i>').append('<span class="la label"></span>').find('.la.label').append(this.name);
      elm.find('.facetName').click(function() {
        $('.facetSort, ul, .PagerModule', $(this).parents('.tableWidget')).toggle();
        var chevron = elm.find(".facetName i");
        if (chevron.hasClass("fa-chevron-down")) {
          chevron.removeClass('fa-chevron-down').addClass('fa-chevron-up ' + animation).on(endAnimationEvents, function() {
            $(this).removeClass(animation);
          });
        } else {
          chevron.removeClass('fa-chevron-up').addClass(animation + ' fa-chevron-down').on(endAnimationEvents, function() {
            $(this).removeClass(animation);
          });
        }
      });
    }
    if (this.pagination === true) {
      this.pagination = new AjaxFranceLabs.PagerModule({
        elm : this.elm,
        updateList : function() {
          if (this.nbPage > 1) {
            $(this.source).children().css('display', 'none').slice(this.pageSelected * this.nbElmToDisplay, (this.pageSelected + 1) * this.nbElmToDisplay).css('display', this.display);
            AjaxFranceLabs.clearMultiElementClasses($('li', this.source));
            AjaxFranceLabs.addMultiElementClasses($('li:visible', this.source));
          }
        }
      });
      this.pagination.manager = this.manager;
    }
    if (this.pagination)
      this.pagination.init();
  },

  update : function() {
    var self = this, data = this.assocTags(this.manager.response.facet_counts.facet_queries), max = (data.length > this.maxDisplay) ? this.maxDisplay : data.length, elm = $(this.elm);
    elm.find('ul').empty();

    // Define the available facet queries
    var enabledQueries = [];
    if (max == self.queries.length) {
      enabledQueries = self.queries;
    } else {
      var cptQ = 0;
      for (var i = 0; i < max; i++) {
        var inArray = $.inArray(decodeURIComponent(data[i].name), self.labels);
        if (inArray != -1) {
          enabledQueries[cptQ] = self.queries[inArray];
          cptQ++;
        }
      }
    }

    for (var i = 0; i < max; i++) {
      elm.find('ul').append('<li></li>');
      elm.find('ul li:last').append('<label></label>');

      elm.find('ul li:last label').append('<div class="filterFacetCheck"></div>').append('<div class="filterFacetLabel"></div>');
      elm.find('ul li:last .filterFacetCheck').append('<input type="checkbox" value="' + data[i].name + '"/>');
      elm.find('ul li:last .filterFacetCheck input').attr('id', self.id + "-" + data[i].name);
      var filter = new RegExp(self.field + ':' + enabledQueries[i].replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
      if (this.manager.store.find('fq', filter))
        elm.find('ul li:last .filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
      elm.find('ul li:last .filterFacetCheck input').change(function() {

        if ($(this).is(':checked')) {

          if (self.selectionType === 'ONE' && elm.find('ul li .filterFacetCheck input:checked').not(this).length)
            self.remove(elm.find('ul li .filterFacetCheck input:checked').not(this).val());

          var checked = this;
          elm.find('input').each(function() {
            if (checked != this) {
              $(this).attr('checked', false);
            }
          });

          self.clickHandler();
          self.selectHandler($(this).val());
        } else {
          self.clickHandler();
          self.unselectHandler($(this).val());
        }
      });

      elm.find('ul li:last .filterFacetCheck').append('<label></label>');
      if (elm.find('ul li:last .filterFacetCheck input').is(':checked'))
        elm.find('ul li:last .filterFacetCheck label').attr('for', self.id + "-" + data[i].name).append(
            '<span class="checkboxIcon far fa-check-square">&nbsp;</span>' + '<span class="filterFacetLinkValue">' + AjaxFranceLabs.tinyString(decodeURIComponent(data[i].name), 27) + '</span>')
            .append('&nbsp;<span class="filterFacetLinkCount">(<span>' + data[i].nb + '</span>)</span>');
      else
        elm.find('ul li:last .filterFacetCheck label').attr('for', self.id + "-" + data[i].name).append(
            '<span class="checkboxIcon far fa-square">&nbsp;</span>' + '<span class="filterFacetLinkValue">' + AjaxFranceLabs.tinyString(decodeURIComponent(data[i].name), 27) + '</span>').append(
            '&nbsp;<span class="filterFacetLinkCount">(<span>' + data[i].nb + '</span>)</span>');
    }

    // Enable the modules, create one line per module
    for (var i = 0; i < this.modules.length; i++) {
      elm.find('ul').append('<li id="li-' + this.modules[i].id + '"></li>');
      this.modules[i].elm = elm.find('ul li:last');
      this.modules[i].afterRequest();
    }

    if (this.pagination) {
      this.pagination.source = $('ul', this.elm);
      this.pagination.updatePages();
    }
    this.sortBy(this.sort);
  },

  assocTags : function(data) {
    var self = this, tempData = Array();
    $.each(this.labels, function(i, label) {
      var encodedLabel = encodeURI(label);
      if (data[encodedLabel] !== undefined)
        tempData.push({
          name : encodedLabel,
          nb : data[encodedLabel]
        });
    });
    return tempData;
  },

  sortBy : function(sort) {
  },

  set : function(value) {
    this.manager.store.removeByValue('fq', new RegExp('^-?' + this.field + ':'));

    var self = this;
    var query;
    $.each(self.labels, function(i, label) {
      var encodedLabel = encodeURI(label);
      if (encodedLabel === value) {
        query = self.queries[i];
      }
    });
    var valueToAdd = '{!tag=query}' + this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(query);
    return this.manager.store.addByValue('fq', valueToAdd);
  },

  remove : function(value) {
    var self = this;
    var query;
    $.each(self.labels, function(i, label) {
      var encodedLabel = encodeURI(label);
      if (encodedLabel === value) {
        query = self.queries[i];
      }
    });
    var searchedValue = this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(query);
    return this.manager.store.removeByValue('fq', '{!tag=query}' + searchedValue);
  },
  clickHandler : function() {
  },
  afterRequest : function() {
    this.update();
  },

  beforeRequest : function() {
    // Call the modules beforeRequest method
    for (var i = 0; i < this.modules.length; i++) {
      this.modules[i].beforeRequest();
    }
  }
});
