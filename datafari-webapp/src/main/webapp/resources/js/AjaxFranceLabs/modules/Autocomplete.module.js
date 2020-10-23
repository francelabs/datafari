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
 * @class AjaxFranceLabs.AutocompleteModule
 * @extends AjaxFranceLabs.AbstractModule
 * 
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#autocomplete_module
 * 
 */
AjaxFranceLabs.AutocompleteModule = AjaxFranceLabs.AbstractModule.extend({
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
  lastSuggestersXhr : null,
  entitySuggesters : null,
  response : null,
  lastTerm : null,
  src : null,
  srcSuggesters : null,
  maxSize : 10,
  doneCalls : 0,

  standardAutocomplete : function() {
    var self = this;
    var f = (self.field === null) ? '' : '&f=' + self.field;
    var aggregatorStrVal = self.manager.store.get("aggregator").val()
    var aggregator = "";
    if (self.manager.store.get("aggregator") && aggregatorStrVal != null && aggregatorStrVal != undefined) {
      aggregator = "&aggregator=" + self.manager.store.get("aggregator").valueString(aggregatorStrVal);
    }

    self.lastXhr = self.manager.executeRequest(self.connectionInfo.serverUrl, self.connectionInfo.servlet, self.connectionInfo.queryString
        + encodeURIComponent('' + AjaxFranceLabs.extractLast(self.lastTerm) + '') + aggregator + f + "&autocomplete=true" + "&spellcheck.collateParam.q.op=" + self.manager.store.get("q.op").val(),
        function(data, status, xhr) {

          if (data.spellcheck != null) {
            if (data.spellcheck.collations.length > 1) {

              $.each(data.spellcheck.collations, function(index, value) {
                if (value != 'collation') {
                  var val = value.collationQuery.replace(/"/g, '');
                  var collationObj = {
                    label : val,
                    value : val
                  };
                  self.src.push(collationObj);
                }
              });
            }
          }

          // Increase the doneCalls variable
          self.doneCalls++;
          // If the doneCalls variable equals the number of entity suggesters + 1, it means that the autocomplete is ready to be displayed
          // as we
          // have all the suggests
          if (self.entityActivated === false || self.doneCalls === self.entitySuggesters.length + 1 - self.numDisabled) {
            self.displayAutocomplete();
          }

        }, function() {
        }, 30000);
  },

  displayAutocomplete : function() {
    var self = this;
    var combinedData = [];
    // If entity suggestions are available so concat entity suggestions with standard ones then display the autocomplete
    // Else just display the autocomplete with the standard suggestions
    if (self.srcSuggesters.length > 0) {
      combinedData = self.srcSuggesters.concat(self.src);
      combinedData.splice(self.maxSize);

      // Put the autocomplete array in cache
      var term = AjaxFranceLabs.extractLast(self.lastTerm).toLowerCase();
      self.cache[term] = combinedData;
      // Display the autocomplete
      self.response(combinedData);
    } else {
      // Put the autocomplete array in cache
      var term = AjaxFranceLabs.extractLast(self.lastTerm).toLowerCase();
      self.src.splice(self.maxSize);
      self.cache[term] = self.src.slice(0);
      // Display the autocomplete
      self.response(self.src);
    }
  },

  // Function that returns an updated value for the entities-highlight-content html element, based on currentEntitiesContent (last state)
  // and searchInputContent (up to date search entered by the user)
  // the goal is to try to preserve the entities span tags
  getFinalEntitiesContent : function(currentEntitiesContent, searchInputContent) {

    // clean \u200c char from searchInputContent
    var regex = new RegExp('\u200c', "ig");
    searchInputContent = searchInputContent.replace(regex, '');

    // index of the current char to compare in the currentEntitiesContent
    var cptEnt = 0;
    // var used to skip chars in the searchInputContent if needed
    var skipUntil = -1;
    var finalEntitiesContent = "";
    for (var i = 0; i < searchInputContent.length; i++) {
      if (i > skipUntil) {
        if (currentEntitiesContent.length <= i || currentEntitiesContent.length <= cptEnt) {
          // We already analyzed all chars of the currentEntitiesContent so set the cptEnt to -1 to disable comparisons
          cptEnt = -1;
        }
        if (searchInputContent.charAt(i) === currentEntitiesContent.charAt(cptEnt) || cptEnt === -1) {
          // the chars are equal, so add the current char to finalEntitiesContent
          finalEntitiesContent += searchInputContent.charAt(i);
          if (cptEnt !== -1) {
            cptEnt++;
          }
        } else {
          if (currentEntitiesContent.charAt(cptEnt) === "<") { // char is potentially announcing the beginning of a span tag that declare
            // an entity term
            if (currentEntitiesContent.substr(cptEnt, 5) === "<span") { // the next chars indicate that this IS a span tag that declare an
              // entity term

              // This lines of code extract the entity word declared by the span tag in the eWord var, and the whole span tag in the entSpan
              // var
              var left = currentEntitiesContent.substring(cptEnt);
              var bWordI = left.indexOf(">") + 1;
              var eWordI = left.indexOf("</span>");
              // entity word
              var eWord = left.substring(bWordI, eWordI);
              // whole span tag
              var entSpan = left.substring(0, eWordI + 7);
              // Now compare the entity word with the text in the searchInputContent
              if (searchInputContent.substr(i, eWord.length) === eWord) {
                // the text of the searchInputContent is equals to the entity word, so preserve the whole span tag in the
                // finalEntitiesContent
                finalEntitiesContent += entSpan;
                // skip the chars in searchInputContent until the entity word length is reached
                skipUntil = i + eWord.length - 1;
                // set the index of the next char of the currentEntitiesContent to be analyzed to the index of the first char after the end
                // of the span tag
                cptEnt += entSpan.length;
              } else {
                // the text of the searchInputContent is not equal to the entity word, so replace the whole span tag by the text of the
                // searchInputContent until the next space char (if any, otherwise until the end of searchInputContent)
                var searchLeft = searchInputContent.substring(i);
                var eSearchWordI = searchLeft.indexOf(" ");
                if (eSearchWordI === -1) {
                  eSearchWordI = searchLeft.length;
                }
                var sWord = searchLeft.substring(0, eSearchWordI);
                finalEntitiesContent += sWord;
                // skip the chars of searchInputContent until the end of the sWord
                skipUntil = i + sWord.length - 1;
                // set the next char index to be analyzed in currentEntitiesContent to the first char at the end of the span tag
                cptEnt += entSpan.length;
              }
            } else {
              // the "<" char in currentEntitiesContent was not the beginning of a span tag. As it is different from the char in
              // searchInputContent, until a space char is encountered in both currentEntitiesContent and searchInputContent, only keep
              // searchInputContent chars

              var left = currentEntitiesContent.substring(cptEnt);
              var jump = left.indexOf(" ");
              if (jump !== -1) {
                cptEnt += jump;
              } else {
                cptEnt = -1;
              }
              var searchLeft = searchInputContent.substring(i);
              var eSearchWordI = searchLeft.indexOf(" ");
              if (eSearchWordI === -1) {
                eSearchWordI = searchLeft.length;
              }
              if (eSearchWordI === 0) {
                finalEntitiesContent += " ";
                if (cptEnt !== -1) {
                  cptEnt++;
                }
              } else {
                var sWord = searchLeft.substring(0, eSearchWordI);
                finalEntitiesContent += sWord;
                skipUntil = i + sWord.length - 1;
              }
            }
          } else {
            // the char in currentEntitiesContent is different from the char in
            // searchInputContent. So, until a space char is encountered in both currentEntitiesContent and searchInputContent, only keep
            // searchInputContent chars

            var left = currentEntitiesContent.substring(cptEnt);
            var jump = left.indexOf(" ");
            if (jump !== -1) {
              cptEnt += jump;
            } else {
              cptEnt = -1;
            }
            var searchLeft = searchInputContent.substring(i);
            var eSearchWordI = searchLeft.indexOf(" ");
            if (eSearchWordI === -1) {
              eSearchWordI = searchLeft.length;
            }
            if (eSearchWordI === 0) {
              finalEntitiesContent += " ";
              if (cptEnt !== -1) {
                cptEnt++;
              }
            } else {
              var sWord = searchLeft.substring(0, eSearchWordI);
              finalEntitiesContent += sWord;
              skipUntil = i + sWord.length - 1;
            }
          }
        }
      }
    }
    return finalEntitiesContent;
  },

  // Methods
  init : function() {
    if (!this.initialized) {
      this.initialized = true;
      var self = this;
      self.numDisabled = 0;
      // retrieve entity suggesters activation and configuration
      $.ajax({
        url : "GetEntitySuggesters",
        success : function(data) {
          if (data.code == 0 && data.entitySuggesters != undefined && data.entitySuggesters != null && data.entitySuggesters != "") {
            self.entitySuggesters = JSON.parse(data.entitySuggesters);
            for (var i = 0; i < self.entitySuggesters.length; i++) {
              var entitySuggester = self.entitySuggesters[i];
              var categoryKey = entitySuggester.categoryKey;
              var i18nKey = entitySuggester.i18nKey;
              if (entitySuggester.activated != "true") {
                self.numDisabled += 1;
              }

              entitySuggester.handler = i;
            }
            self.entityActivated = data.activated;
            if (self.entityActivated === true) {
              // activate value copy from search bar to entities html element at any input on the search bar
              self.elm.on({
                'input' : function() {
                  var entitiesContent = $(".entities-highlight-content").html();
                  var searchInputContent = self.elm.val();
                  // We cannot copy the raw data, need to preserve entities span tags of the entities html element if any
                  var fVal = self.getFinalEntitiesContent(entitiesContent, searchInputContent);
                  $(".entities-highlight-content").html(fVal);
                  // Update the manager store param if the final value is not empty
                  self.manager.store.remove("entQ");
                  if (fVal !== "") {
                    self.manager.store.addByValue("entQ", fVal);
                  }
                }
              });
            }
          }
        },
        dataType : "json",
        error : function() {
          self.entitySuggesters = [];
        }
      });

      // Enable html tags in autocomplete labels
      self.render = function(ul, item) {
        ul.addClass("autocomplete-menu");
        return $("<li class='autocomplete-line'></li>").data("item.autocomplete", item).append("<a class='autocomplete-link'>" + item.label + "</a>").appendTo(ul);
      };

      var opt = {
        delay : 250,
        minLength : 2,
        source : function(request, response) {

          // Abort any previously triggered request of standard autocomplete
          if (self.lastXhr != null) {
            self.lastXhr.abort();
          }
          // Abort any previously triggered requests of suggesters autocompletes
          if (self.lastSuggestersXhr != null) {
            for (var i = 0; i < self.lastSuggestersXhr.length; i++) {
              self.lastSuggestersXhr[i].abort();
            }
          }
          self.lastSuggestersXhr = []; // re-init suggesters xhr array
          self.src = []; // Array used for the standard autocomplete suggestions
          self.srcSuggesters = []; // Array used for the entities suggestions

          // Save the response object in local to be able to use it in asynchronous methods
          self.response = response;
          // Save the query term in local to be able to use it in asynchronous methods
          self.lastTerm = request.term;
          // Re-init the local variables
          self.doneCalls = 0;

          // If the term is in the cache so display it, no need to perform requests
          var term = AjaxFranceLabs.extractLast(request.term).toLowerCase();
          if (term in self.cache) {
            response(self.cache[term]);
            return;
          }

          var lastTerm = request.term.split(" ").pop().toLowerCase();
          if (lastTerm != '') {
            // If entity suggesters are available and the entity feature is enabled then perform request to each suggester
            if (self.entitySuggesters.length > 0 && self.entityActivated === true) {

              for (var i = 0; i < self.entitySuggesters.length; i++) {
                var entitySuggester = self.entitySuggesters[i];
                // Suggesters are individually activated, request only for those that are activated.
                if (entitySuggester.activated != "true") {
                  continue;
                }
                var core = entitySuggester.solrCore ? "core=" + entitySuggester.solrCore + "&" : "";
                var aggregatorStrVal = self.manager.store.get("aggregator").val()
                var aggregator = self.manager.store.get("aggregator") ? "aggregator=" + self.manager.store.get("aggregator").valueString(aggregatorStrVal) + "&" : "";

                self.lastSuggestersXhr.push(self.manager.executeRequest(

                entitySuggester.serverUrl, entitySuggester.servlet, aggregator + core + "action=suggest&q=" + encodeURIComponent('' + lastTerm + ''), function(data, status, xhr) {
                  if (data.suggest != null) {
                    var suggesterName = Object.keys(data.suggest)[0];
                    var suggester = data.suggest[suggesterName];
                    // Based on the suggesterName we can retrieve the configuration and then use the configuration parameters to build the
                    // autocomplete label
                    // this is the main reason why you MUST be sure in the entity-autocomplete.properties that each suggestComponent's name
                    // is
                    // unique
                    var entSugg = null;
                    for (var j = 0; j < self.entitySuggesters.length; j++) {
                      if (self.entitySuggesters[j].suggestComponent == suggesterName) {
                        // Found the correct configuration
                        entSugg = self.entitySuggesters[j];
                        break;
                      }
                    }
                    if (suggester[lastTerm].numFound > 0 && entSugg != null) {
                      var cpt = 0;
                      $.each(suggester[lastTerm].suggestions, function(index, value) {
                        // extract the suggestion
                        var suggestion = value.term.replace(/"/g, '').trim();
                        // Extract the payload if there is one
                        var payload = value.payload.trim();
                        // build the label that will be displayed in the autocomplete
                        var specificCssCLass = "";
                        if (entSugg.cssClass != undefined && entSugg.cssClass != null && entSugg.cssClass != "") {
                          specificCssCLass = "catClass='" + entSugg.cssClass + "'";
                        }
                        var suggestObj = {
                          label : "<span class='cat-id' " + specificCssCLass + ">" + entSugg.categoryKey + "</span><span class='entity-value'>" + suggestion + "</span>"
                              + " <span class='autocomplete-suggestion'>" + window.i18n.msgStore[entSugg.i18nKey] + "</span> ",
                          value : suggestion
                        };
                        if (payload) {
                          suggestObj.payload = payload;
                        }
                        // Save the created label if new in the srcSuggesters obj that will be used at the end of all autocomplete call to
                        // build the autocomplete ui element
                        if (value != 'collation' && self.srcSuggesters.indexOf(suggestObj) == -1) {
                          self.srcSuggesters.push(suggestObj);
                          cpt++;
                        }
                        // limit the number of suggestions to the limit defined in the configuration
                        if (cpt == entSugg.maxSuggest) {
                          return false;
                        }
                      });
                    }
                    self.doneCalls++;
                    // If the doneCalls variable equals the number of entity suggesters + 1, it means that the autocomplete is ready to be
                    // displayed as we have all the suggests
                    if (self.doneCalls === self.entitySuggesters.length + 1 - self.numDisabled) {
                      self.displayAutocomplete();
                    }
                  }
                }, function() {
                }, 30000));
              }

              self.standardAutocomplete();

            } else {
              self.standardAutocomplete();
            }

          } else {
            response(self.src);
          }

        },
        search : function() {
          var term = AjaxFranceLabs.extractLast(this.value);
          if (term.length < self.autocompleMinLength) {
            return false;
          }
        },
        select : function(event, ui) {
          var label = ui.item.label;
          var elm = $("<div>" + label + "</div>");
          var catIdElm = elm.find(".cat-id");
          var catId = catIdElm.html();
          var payloadProp = ui.item.payload ? "entity-payload=\"" + ui.item.payload + "\"" : "";

          if (self.singleValue === true) {
            this.value = self.valueSelectFormat(ui.item.value);
            this.value += ("\u200c");
          } else {
            var terms = AjaxFranceLabs.split(this.value);

            // If the entity suggesters are activated and a catId has been found, it means that this is an entity suggestion
            // An entity suggestion is based on the last word entered, so need to split this.value on spaces in order that the terms.pop()
            // function removes the last word that need to be replaced by the suggestion
            if (self.entityActivated === true && catId != undefined && catId != null) {
              terms = this.value.split(" ");
            }
            // Removes the last term
            terms.pop();
            // As we removed the last term, it will be replaced by the ui.item.value which is the suggestion
            terms.push(self.valueSelectFormat(ui.item.value));
            terms.push("");
            if (self.entityActivated === true && catId != undefined && catId != null) {
              // As we did not split this.value on \u200c but on spaces, need to rebuild the value with spaces only.
              // No need of the \u200c char
              this.value = terms.join(" ");
              this.value = this.value.slice(0, -1);
            } else {
              this.value = terms.join("\u200c ");
              this.value = this.value.slice(0, -2);
            }
          }

          // If the entity suggesters are activated and this is an entity suggestion, need to update the entities-highlight-content html
          // element that stores the query typed by the user but with entities encapsulated with span tags
          if (self.entityActivated === true) {

            var entitiesContent = $(".entities-highlight-content").html();
            var searchInputContent = self.elm.val();
            var fVal = self.getFinalEntitiesContent(entitiesContent, searchInputContent);

            // Figure out if the selected option is a category
            if (catId != undefined && catId != null) {
              // get a specific css class to apply if any defined
              var catClass = catIdElm.attr("catClass");
              var cssClass = "";
              if (catClass != undefined && catClass != null && catClass != "") {
                cssClass = " " + catClass;
              }
              // this is an entity suggestion so need to replace the term suggested in the original query by a span allowing us to know that
              // this term is an entity and that provides the id of the category it belongs to
              var regex = new RegExp('\\b' + ui.item.value + '\\b$', "ig");
              fVal = fVal.replace(regex, '<span class="entity-hl' + cssClass + '" entity-id="' + catId + '" ' + payloadProp + '>$&</span>');
            }

            $(".entities-highlight-content").html(fVal);
            // Update the manager store parameter
            self.manager.store.addByValue("entQ", fVal);

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
        $(this.elm).data("ui-autocomplete")._renderItem = this.render;
      }
      if (this.manager.connectionInfo.autocomplete === undefined && this.options.source == null)
        throw 'AutocompleteModule: connectionInfo not defined in Manager';
      else
        this.connectionInfo = this.manager.connectionInfo.autocomplete;

    }
  }
});
