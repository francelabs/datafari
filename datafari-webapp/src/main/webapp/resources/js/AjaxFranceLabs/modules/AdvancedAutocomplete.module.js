/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
/**
 * 
 * @class AjaxFranceLabs.CustomAutocompleteModule
 * @extends AjaxFranceLabs.AbstractModule
 * 
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#autocomplete_module
 * 
 */
AjaxFranceLabs.AdvancedAutocompleteModule = AjaxFranceLabs.AbstractModule.extend({
  // Variables
  optionsExtend : true,
  options : {},
  render : null,
  field : null,
  openOnFieldFocus : false,
  valueSelectFormat : function(value) {
    return value
  },
  singleValue : false,
  type : 'autocomplete',
  cache : [],
  lastXhr : null,
  serverUrl : '',
  servlet : '',
  queryString : 'action=suggest&q=',
  // Methods
  init : function() {
    if (!this.initialized) {
      this.initialized = true;
      var self = this;
      var opt = {
        delay : 50,
        minLength : 0,
        source : function(request, response) {
          // Abort any currently running previous request
          if (self.lastXhr != null) {
            self.lastXhr.abort();
          }
          var lastTerm = AjaxFranceLabs.extractLast(request.term);
          var term = lastTerm.toLowerCase();
          if (term in self.cache) {
            response(self.cache[term]);
            return;
          }
          if (lastTerm != '') {

            var src = [];
            self.lastXhr = self.manager.executeRequest(

            self.serverUrl, self.servlet, self.queryString + encodeURIComponent('' + lastTerm + ''), function(data, status, xhr) {

              if (data.suggest != null) {
                var suggester = data.suggest[Object.keys(data.suggest)[0]];
                if (suggester[lastTerm].numFound > 0) {

                  $.each(suggester[lastTerm].suggestions, function(index, value) {
                    var suggestion = suggester[lastTerm].suggestions[index].term.replace(/"/g, '').replace(/<b>/g, '').replace(/<\/b>/g, '').trim().toLowerCase();
                    if (value != 'collation' && src.indexOf(suggestion) == -1) {
                      src.push(suggestion);
                    }
                  });
                  self.cache[term] = src;
                }
              }
              response(src);
            }, function() {
            }, 30000);
          } else {
            response([]);
          }

        },
        search : function() {
          var term = AjaxFranceLabs.extractLast(this.value);
          if (term.length < self.autocompleMinLength) {
            return false;
          }
        },
        select : function(event, ui) {
          if (self.singleValue === true) {
            this.value = self.valueSelectFormat(ui.item.value);
            this.value += ("\u200c");
          } else {
            var terms = AjaxFranceLabs.split(this.value);
            terms.pop();
            terms.push(self.valueSelectFormat(ui.item.value));
            terms.push("");
            this.value = terms.join("\u200c ");
            this.value = this.value.slice(0, -2);
          }
          return false;
        },
        focus : function() {
          return false;
        }
      };
      if (this.optionsExtend === true) {
        var newOpt = {};
        $.extend(true, newOpt, opt, this.options);
        this.options = newOpt;
      }
      $(this.elm).autocomplete(this.options);
      if (this.openOnFieldFocus === true) {
        $(this.elm).click(function() {
          $(this).autocomplete('search');
        });
      }
      if (typeof this.render === 'function') {
        $(this.elm).data("autocomplete")._renderItem = this.render;
      }
    }
  }
});
