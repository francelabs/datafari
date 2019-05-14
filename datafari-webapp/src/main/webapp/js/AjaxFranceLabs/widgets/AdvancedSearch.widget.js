/*******************************************************************************
 * Copyright 2015 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
AjaxFranceLabs.AdvancedSearchWidget = AjaxFranceLabs.AbstractWidget.extend({

  // Variables

  type : 'advancedSearch',

  // List of fields allowed for the advanced search
  // they will appear in the Select element
  available_fields : null,

  // Main div element containing all the advanced search UI
  advTable : null,

  // Represents the current number of fields added to the advanced search
  // It is used for HTML ids
  // Be careful : This number is never decreased, even if a field is deleted by
  // the user or removed by the onchange method of the dropdown box to select a
  // field !
  // Therefore it must not be used to determine the current number of effective
  // fields
  fieldNumber : 0,

  // This is the list of AdvancedSearchFields objects that have been
  // instanciated and currently effective
  // The ones that are deleted by the user or removed by the onchange method of
  // the dropdown box to select a field are and must be removed from this list !
  fieldsList : [],

  // This is an object containing mapping values for fieldnames
  // It is only used to replace matching fieldnames by their mapping values in
  // the labels displayed to the user
  mappingFieldNameValues : {},

  // Represents the list of fieldnames that have a specific Solr field for exact
  // match queries associated with the name of this specific Solr field.
  // The exact match Solr field must be formated like this :
  // [original_fieldname]_exact
  exactFieldsList : null,

  // This is an object containing fields that must have the autocomplete
  // functionality. The fields are mapped to their Solr suggester's name that
  // will be used to perform suggest requests
  autocompleteFields : {},

  // This is an object containing fields that must have a list of fixed values
  // instead of a free text input. The fields are mapped to their list of fixed
  // values values
  fixedValuesFields : {},

  // Maximum char for text inputs
  maxChar : 512,

  // Methods

  buildWidget : function() {

    var self = this, elm = $(this.elm);

    // Retrieve available fields that a user can select in the advanced search
    $.get("./GetFieldsInfo", function(data) {
      self.available_fields = data;
    }, "json");

    // Retrieve the exactFieldsList
    $.get("./GetExactFields", function(data) {
      if (data.code == 0) {
        self.exactFieldsList = {};
        // Construct the exactFieldsList using a fieldname as key and
        // [fieldname]_exact as the corresponding value that represents the Solr
        // fieldname for exact match queries
        for (var cptExactFields = 0; cptExactFields < data.exactFieldsList.length; cptExactFields++) {
          self.exactFieldsList[data.exactFieldsList[cptExactFields]] = data.exactFieldsList[cptExactFields] + "_exact";
        }
      }
    }, "json");

    // Retrieve the fields that must have the autocomplete functionality
    // associated to their suggester
    $.get("GetAutocompleteAdvancedFields", function(data) {
      if (data.code == 0 && data.autocompleteFields != undefined && data.autocompleteFields != null && data.autocompleteFields != "") {
        self.autocompleteFields = JSON.parse(data.autocompleteFields);
      }
    }, "json");

    // Retrieve the fields that must have a fixed values list instead of a free
    // text input
    $.get("GetFixedValuesAdvancedFields", function(data) {
      if (data.code == 0) {
        self.fixedValuesFields = JSON.parse(data.fixedValuesFields);
      }
    }, "json");

    // Retrieve the fields that must be mapped to a provided custom label
    $.get("GetLabeledAdvancedFields", function(data) {
      if (data.code == 0) {
        self.mappingFieldNameValues = JSON.parse(data.mappingFieldNameValues);
      }
    }, "json");

    elm.addClass('advancedSearchWidget').addClass('widget').attr('widgetId', this.id);

    // Activate tooltip
    elm.tooltip();

    // Trigger the search when pressing 'Enter' on the keyboard
    elm.find('.advTable input').keypress(function(event) {
      if (event.keyCode === 13) {
        self.makeRequest();
      }
    });
  },

  reinitVariables : function() {
    this.fieldNumber = 0;
    this.fieldsList = [];
  },

  buildStartingUI : function() {
    this.reinitVariables();
    var self = this, elm = $(this.elm);

    // Retrieve the last executed query
    var baseQuery = this.manager.store.get('q').val();

    // Regex that matches field filters in the query (ie any [field]:)
    var baseQueryRegEx = /(AND\s|OR\s)*\(*[^\s\(\[\:]+:/g;
    // Get the index of the first field filter found
    var indexOf = baseQuery.search(baseQueryRegEx);
    var baseSearch = "";
    // Extract the base search which is everything before the index of the first
    // field filter or everything at all if not field filter is found
    if (indexOf != -1) {
      baseSearch = baseQuery.substring(0, indexOf);
    } else {
      baseSearch = baseQuery;
    }

    // Exact expression baseSearch regex
    var baseExactExprVal = "";
    // var baseExactExprRegEx = /\((exactContent|exactTitle):[^\)]+\)/g;
    var baseExactExprRegEx = /\((exactContent|exactTitle):[^)]* OR (exactContent|exactTitle):[^)]*\)/g;
    var baseExactExpr = baseQuery.match(baseExactExprRegEx);
    var exactContentRegEx = /exactContent:(?:(?!( AND | OR ))[^)(])+/g;
    var exactTitleRegEx = /exactTitle:(?:(?!( AND | OR ))[^)(])+/g;
    if (baseExactExpr != null && baseExactExpr != undefined && baseExactExpr != "") {
      var exactContentExpr = baseExactExpr[0].match(exactContentRegEx);
      var exactTitleExpr = baseExactExpr[0].match(exactTitleRegEx);

      if (exactContentExpr != null && exactContentExpr != undefined && exactContentExpr != "" && exactTitleExpr != null && exactTitleExpr != undefined && exactTitleExpr != "") {
        var exactContentVal = exactContentExpr[0].split(":")[1].replace(/\"/g, '').trim();
        var exactTitleVal = exactTitleExpr[0].split(":")[1].replace(/\"/g, '').trim();

        if (exactContentVal === exactTitleVal) {
          // Remove base exact expression from baseQuery
          baseQuery = baseQuery.replace(baseExactExprRegEx, '');
          // Retrieve base exact expr value
          baseExactExprVal = exactContentVal;
        }
      }

    }

    // Construct the main div which will contain every UI element of the
    // advanced search
    elm.append('<div class="advTable">');
    this.advTable = elm.find('.advTable');
    // Build the title
    this.advTable.append('<div class="head-title"><span class="header-sub-menu-label">' + window.i18n.msgStore['advancedSearch-label'] + '</span></div>');

    // Add a separator
    this.advTable.append('<span class="separator">');

    // Build the base search filters
    this.advTable.append('<div id="adv_base_search">');
    var adv_base_search = this.advTable.find('#adv_base_search');
    adv_base_search.append('<span class="subtitle left">').find('.left').append(window.i18n.msgStore['baseSearch-label']);
    var baseSearchValues = self.extractFilterFromText(baseSearch, false, true);
    baseSearchValues["exact_expression_value"] = baseExactExprVal;
    this.constructFilter("string", adv_base_search, null, baseSearchValues, null, this.fieldNumber);

    // Add a separator
    this.advTable.append('<span class="separator">');

    // Build the "add field filter" button
    this.advTable.append('<div><span id="add_adv_field" title="' + window.i18n.msgStore['advancedSearch-add-filter-tooltip'] + '" class="add-field-button">+</span></div>');
    $('#add_adv_field').click(function() {
      self.addField();
    });

    // Build the exec search button
    this.advTable.append('<button id="exec_adv_search">' + window.i18n.msgStore['advancedSearch-makesearch-btn'] + '</button>').find('button').click(function() {
      self.makeRequest();
    });

    // Add other fields filters if the base/original query searched by the user
    // is not empty
    if (baseQuery != "" && baseQuery != "*" && baseQuery != "*:*") {

      // Regex that matches every field expression (ie 'AND/OR [field]:[value]')
      var superFieldsRegEx = /(AND\s|OR\s)*[^\s\(\[\:]+:(\[[^\]]+\]|\([^\)]+\)|\"[^\"]+\"|[^\s\(\]]+)/g;
      // Put the matches of the regex in a variable
      var fields = baseQuery.match(superFieldsRegEx);
      // Object which will contain the last found field filter that corresponds
      // to a Solr field used for exact match (ie [field]_exact)
      var lastExact = {};
      if (fields != null && fields != undefined && fields != "") {
        // For each field filter, the operator (if any), the fieldname and the
        // filter values are extracted and the corresponding UI is built
        for (var cpt = 0; cpt < fields.length; cpt++) {
          var isExactField = false;
          var exactFilter = "";
          var originalFieldname = "";
          var fieldExpression = fields[cpt];

          // Extract operator if there is one
          var operator = "";
          if (fieldExpression.startsWith('AND') || fieldExpression.startsWith('OR')) {
            operator = fieldExpression.split(" ")[0];
            // Remove the found operator from the fieldExpression
            fieldExpression = fieldExpression.substring(operator.length + 1);
          }

          // Extract fieldname
          var fieldname = fieldExpression.substring(0, fieldExpression.indexOf(":"));
          var negativeExpression = false;
          // If the first char of the fieldname is -, it is because it is a
          // negative filter
          // need to remove this char to get the real fieldname
          if (fieldname.startsWith("-")) {
            fielname = fieldname.substring(1);
            negativeExpression = true;
          }
          // Remove the fielname from the fieldExpression as it only remains the
          // filter expression
          fieldExpression = fieldExpression.substring(fieldname.length + 1);

          // Determine if the previous field was an exact Solr field of this one
          if (!jQuery.isEmptyObject(lastExact) && fieldname in lastExact) {
            // The previous field was the exact field of this one
            // Get the exact filter to put it in the exact_expression_value
            // filter of the current field
            exactFilter = lastExact[fieldname]["extractedValues"]["exact_expression_value"];
            // Empty the lastExact object
            lastExact = {};
          } else if (!jQuery.isEmptyObject(lastExact)) {
            // The lastExact object is not empty
            // This means that the previous field was an exact Solr field and it
            // doesn't match with the current field
            // Thus it was not added to the UI and need to be added now !
            var lastExactOriginalFieldname = "";
            for ( var key in lastExact) {
              lastExactOriginalFieldname = key;
            }
            var lastExactFieldName = lastExactOriginalFieldname + "_exact";
            var lastExactOperator = lastExact[lastExactOriginalFieldname]["operator"];
            var lastExactExtractedValues = lastExact[lastExactOriginalFieldname]["extractedValues"];
            // Create the UI
            self.addField(lastExactFieldName, lastExactExtractedValues, lastExactOperator);
          }

          // Determine if the current field is an exact Solr field
          var moreThanExactExprRegex = /(?![^\"]*\")[^\)\s].*/g;
          if (fieldname.endsWith("_exact") && (fieldExpression.startsWith("\"") || fieldExpression.startsWith("(\"")) && fieldExpression.match(moreThanExactExprRegex) == null) {
            isExactField = true;
            originalFieldname = fieldname.replace("_exact", "");
          }

          // Determine field type
          var fieldType = "";
          for (var i = 0; i < self.available_fields.field.length; i++) {
            if (self.available_fields.field[i].name == fieldname) {
              fieldType = self.available_fields.field[i].type;
              break;
            }
          }
          if (fieldType == "tdate" || fieldType == "date") {
            fieldType = "date";
          } else if (fieldType == "int" || fieldType == "long" || fieldType == "float" || fieldType == "double" || fieldType == "tint" || fieldType == "tlong" || fieldType == "tfloat"
              || fieldType == "tdouble") {
            fieldType = "num";
          } else {
            fieldType = "text";
          }

          // Extract filters values
          var extractedValues = null;
          if (fieldType == "text") {
            extractedValues = self.extractFilterFromText(fieldExpression, negativeExpression, false);

            // If the exactFilter variable is not empty then it needs to be
            // added to the extracted values
            if (exactFilter != "") {
              extractedValues["exact_expression_value"] = (extractedValues["exact_expression_value"] + " " + exactFilter).trim();
            }
          } else {
            // If the field type is not text but a number or a date then the
            // filter value can only have two shapes : a single value or a range
            // of values
            var fromValue = null;
            var toValue = null;
            // Detect if the filter value is a range and extract the from and to
            // values if it is the case
            if (fieldExpression.startsWith('[')) {
              var values = fieldExpression.split(" ");
              fromValue = values[0].substring(1);
              toValue = values[2].substring(0, values[2].length - 1);
            } else {
              // The filter value is a direct value.
              // Thus to simplify the treatments of the advanced search, the
              // direct value is converted to a range filter with the same value
              // as from/to values
              fromValue = fieldExpression;
              toValue = fieldExpression;
            }
            // Set the extractedValues variable with the extracted from/to
            // values
            extractedValues = {
              "fromValue" : fromValue,
              "toValue" : toValue
            };
          }

          // If this field is not an exact Solr field then its filter UI can be
          // created now
          // otherwise its exact filter value needs to be saved for the next
          // iteration in order to either add it to it's corresponding "normal"
          // field or to add it as standalone field
          if (!isExactField) {
            self.addField(fieldname, extractedValues, operator);
          } else {
            lastExact[originalFieldname] = {
              "fieldname" : fieldname,
              "operator" : operator,
              "extractedValues" : extractedValues
            };
          }
        }

        // Add remaining lastExact if any
        if (!jQuery.isEmptyObject(lastExact)) {
          // The lastExact object is not empty
          // It was not added to the UI because it was the last field of the for
          // loop and thus needs to be added now !
          var lastExactOriginalFieldname = "";
          for ( var key in lastExact) {
            lastExactOriginalFieldname = key;
          }
          var lastExactFieldName = lastExactOriginalFieldname + "_exact";
          var lastExactOperator = lastExact[lastExactOriginalFieldname]["operator"];
          var lastExactExtractedValues = lastExact[lastExactOriginalFieldname]["extractedValues"];
          self.addField(lastExactFieldName, lastExactExtractedValues, lastExactOperator);
        }
      }
    }
  },

  extractFilterFromText : function(text, negativeExpression, isBasicSearchText) {
    var self = this;
    var all_words_value = "";
    var exact_expression_value = "";
    var at_least_one_word_value = "";
    var none_of_these_words_value = "";

    // Regex that will match every 'AND expression' (ie [word] AND [word])
    andExpressionRegex = /[^\s\[\(\]\)\-\"(AND|OR)]+\sAND\s[^\s\[\(\]\)\-\"(AND|OR)]+/g;
    // Regex that will match every 'OR expression' (ie [word] OR [word])
    orExpressionRegex = /[^\s\[\(\]\)\-\"(AND|OR)]+\sOR\s[^\s\[\(\]\)\-\"(AND|OR)]+/g;

    // Regex that will match any remaining 'AND [word]'
    cleanANDRegex = /(AND\s(?:(?![^\s\[\(]+AND)[^\s\[\(]+)\s?|[^\s\[\(]+\sAND)/g;
    // Regex that will match any remaining 'OR [word]'
    cleanORRegex = /(OR\s(?:(?![^\s\[\(]+OR)[^\s\[\(]+)\s?|[^\s\[\(]+\sOR)/g;

    if (!text.startsWith("(") && !isBasicSearchText) {
      // The filter value is a direct value, no needs to apply complex treatment
      // to extract multiple filters

      if (negativeExpression) {
        none_of_these_words_value = text;
      } else {
        if (text.startsWith("\"")) {
          exact_expression_value = text.substring(1, text.length - 1);
        } else {
          all_words_value = text;
        }
      }
    } else {
      // Remove the parentheses '()' that surround the expression
      if (!isBasicSearchText) {
        text = text.substring(1, text.length - 1);
      }

      // Try to extract negative exact expression (ie -"france labs")
      var exactNegativeExpressionRegex = /-"[^"]*"/g;
      var exactNegativeExpressions = text.match(exactNegativeExpressionRegex);
      if (exactNegativeExpressions != null && exactNegativeExpressions != undefined) {
        for (var cptNgtExact = 0; cptNgtExact < exactNegativeExpressions.length; cptNgtExact++) {
          var exactNegativeExpression = exactNegativeExpressions[cptNgtExact];
          none_of_these_words_value += " " + exactNegativeExpression.substring(1);
          var exactNgtExprIndex = text.search(exactNegativeExpression);
          // If there is a space in front of the exact expression then remove it
          // + the exact expression, otherwise only remove the exact expression
          if (exactNgtExprIndex > 0 && text.substring(exactNgtExprIndex - 1, exactNgtExprIndex) == " ") {
            text = text.replace(" " + exactNegativeExpression, "");
          } else {
            text = text.replace(exactNegativeExpression, "");
          }
        }
      }

      // Try to extract negative words (ie -nuclear)
      var globalValues = text.split(' ');
      for (var cptGlobal = 0; cptGlobal < globalValues.length; cptGlobal++) {
        var gbValue = globalValues[cptGlobal];
        if (gbValue.startsWith("-")) {
          none_of_these_words_value += " " + gbValue.substring(1);
          // If there is a space in front of the negative word then remove it +
          // the negative word, otherwise only remove the negative word
          if (text.search(gbValue) > 0) {
            text = text.replace(" " + gbValue, "");
          } else {
            text = text.replace(gbValue, "");
          }
        }
      }

      // Try to extract the exact expressions (ie "renew energy")
      var exactExpressionsRegex = /"[^\"]+"/g;
      var exactExpressions = text.match(exactExpressionsRegex);
      if (exactExpressions != null && exactExpressions != undefined) {
        for (var cptExact = 0; cptExact < exactExpressions.length; cptExact++) {
          var exactExpression = exactExpressions[cptExact];
          exact_expression_value += " " + exactExpression.substring(1, exactExpression.length - 1);
          var exactExprIndex = text.search(exactExpression);
          // If there is a space in front of the exact expression then remove it
          // + the exact expression, otherwise only remove the exact expression
          if (exactExprIndex > 0 && text.substring(exactExprIndex - 1, exactExprIndex) == " ") {
            text = text.replace(" " + exactExpression, "");
          } else {
            text = text.replace(exactExpression, "");
          }
        }
      }

      // Extract OR expressions
      var tempFieldExpression = text; // initialize a temp variable because at
      // the end of this OR expression
      // extraction,
      // the original text variable must stay untouched for the AND regex to
      // work
      while (tempFieldExpression.match(orExpressionRegex) != null) {
        var orExpressions = tempFieldExpression.match(orExpressionRegex);
        if (orExpressions != null) {
          for (var cptExpr = 0; cptExpr < orExpressions.length; cptExpr++) {
            var exprValues = orExpressions[cptExpr].split(" ");
            if (at_least_one_word_value.indexOf(exprValues[0]) == -1) {
              at_least_one_word_value += " " + exprValues[0];
            }
            if (at_least_one_word_value.indexOf(exprValues[2]) == -1) {
              at_least_one_word_value += " " + exprValues[2];
            }
            var exprIndex = tempFieldExpression.search(exprValues[0] + " OR");
            if (exprIndex != -1) {
              if (tempFieldExpression.substring(exprIndex - 1).startsWith(' ')) {
                tempFieldExpression = tempFieldExpression.replace(" " + exprValues[0] + " OR", "OR");
              } else {
                tempFieldExpression = tempFieldExpression.replace(exprValues[0] + " OR", "OR");
              }
            }
          }
        }
      }

      // Extract AND expressions
      tempFieldExpression = text; // reinit the temp variable with the text
      // variable which has not been modified by the
      // previous OR expression extraction
      while (tempFieldExpression.match(andExpressionRegex) != null) {
        var andExpressions = tempFieldExpression.match(andExpressionRegex);
        if (andExpressions != null) {
          for (var cptExpr = 0; cptExpr < andExpressions.length; cptExpr++) {
            var exprValues = andExpressions[cptExpr].split(" ");
            if (all_words_value.indexOf(exprValues[0]) == -1) {
              all_words_value += " " + exprValues[0];
            }
            if (all_words_value.indexOf(exprValues[2]) == -1) {
              all_words_value += " " + exprValues[2];
            }
            var exprIndex = tempFieldExpression.search(exprValues[0] + " AND");
            if (exprIndex != -1) {
              if (tempFieldExpression.substring(exprIndex - 1).startsWith(' ')) {
                tempFieldExpression = tempFieldExpression.replace(" " + exprValues[0] + " AND", "AND");
              } else {
                tempFieldExpression = tempFieldExpression.replace(exprValues[0] + " AND", "AND");
              }
            }
          }
        }
      }

      // -----------------------------------------------------------------------------------------------------------------------------------------
      // NB: the following cleaning process of the OR and AND expression seems
      // weird and ugly but after bunch of tests,
      // it appears that it needs to be done this way, in that specific order to
      // work like a charm !

      // Clean OR expressions
      while (text.match(orExpressionRegex) != null) {
        var orExpressions = text.match(orExpressionRegex);
        if (orExpressions != null) {
          for (var cptExpr = 0; cptExpr < orExpressions.length; cptExpr++) {
            var exprValues = orExpressions[cptExpr].split(" ");
            var exprIndex = text.search(exprValues[0] + " OR");
            if (exprIndex != -1) {
              if (text.substring(exprIndex - 1).startsWith(' ')) {
                text = text.replace(" " + exprValues[0] + " OR", "OR");
              } else {
                text = text.replace(exprValues[0] + " OR", "OR");
              }
            }
          }
        }
      }

      // Clean AND expressions
      while (text.match(andExpressionRegex) != null) {
        var andExpressions = text.match(andExpressionRegex);
        if (andExpressions != null) {
          for (var cptExpr = 0; cptExpr < andExpressions.length; cptExpr++) {
            var exprValues = andExpressions[cptExpr].split(" ");
            var exprIndex = text.search(exprValues[0] + " AND");
            if (exprIndex != -1) {
              if (text.substring(exprIndex - 1).startsWith(' ')) {
                text = text.replace(" " + exprValues[0] + " AND", "AND");
              } else {
                text = text.replace(exprValues[0] + " AND", "AND");
              }
            }
          }
        }
      }

      // Clean remaining alone 'OR' words
      while (text.match(cleanORRegex) != null) {
        var orExprToClean = text.match(cleanORRegex);
        for (var cptExprToClean = 0; cptExprToClean < orExprToClean.length; cptExprToClean++) {
          text = text.replace(orExprToClean[cptExprToClean], "");
        }
      }
      text = text.replace(/OR/g, "");

      // Clean remaining alone 'AND' words
      while (text.match(cleanANDRegex) != null) {
        var andExprToClean = text.match(cleanANDRegex);
        for (var cptExprToClean = 0; cptExprToClean < andExprToClean.length; cptExprToClean++) {
          text = text.replace(andExprToClean[cptExprToClean], "");
        }
      }
      text = text.replace(/AND/g, "");
      // ------------------------------------------------------------------------------------------------------------------
      // Weird cleaning process is over

      // Trim the expression
      text = text.trim();

      // Search for potential remaining words to add them to the all_words_value
      var lastWords = text.split(" ");
      for (var cptLastWords = 0; cptLastWords < lastWords.length; cptLastWords++) {
        all_words_value += " " + lastWords[cptLastWords];
      }
    }

    // Trim the final values
    all_words_value = all_words_value.trim();
    exact_expression_value = exact_expression_value.trim();
    at_least_one_word_value = at_least_one_word_value.trim();
    none_of_these_words_value = none_of_these_words_value.trim();

    // Put the values in an object and return it
    var returnValues = {};
    returnValues["all_words_value"] = all_words_value;
    returnValues["exact_expression_value"] = exact_expression_value;
    returnValues["at_least_one_word_value"] = at_least_one_word_value;
    returnValues["none_of_these_words_value"] = none_of_these_words_value;

    return returnValues;
  },

  addField : function() {
    this.addField(null, values, null);
  },

  // function that add a new filter to the advanced search UI
  // if called with no fieldName as param it just builds a dropdown box to
  // select a field and implements its onchange method
  // otherwise builds the dropdown box and initialize it with the provided
  // fielName param and trigger the constructFilter method that will build the
  // correct filters in the UI according to the field type and eventually fill
  // the inputs with provided values if any
  addField : function(fieldName, values, operator) {
    // Deactivate tooltips
    this.elm.tooltip('disable');

    var self = this;
    // Builds the dropdown box of the operator and the dropdown box of the
    // available fields
    $('#add_adv_field').parent().before('<div class="adv_field">');
    this.fieldNumber++;
    var currentFieldNum = this.fieldNumber;
    var addButton = $('#add_adv_field').parent().detach();
    var selectID = "field_" + this.fieldNumber;
    var currentDiv = this.advTable.find('.adv_field').last();
    currentDiv.append('<select class="select-operator dropdown"><option selected value="AND">AND</option><option value="OR">OR</option></select><select class="dropdown-field dropdown" id="'
        + selectID + '"><option disabled selected value>' + window.i18n.msgStore['selectField'] + '</option></select><span class="delete_button button"> X </span>');

    // Set operator if available or let the 'AND' operator which is by default
    var selectOperator = currentDiv.find('.select-operator');
    if (operator != null && operator != undefined && operator != "") {
      selectOperator.val(operator);
    }

    // Builds the delete button that remove the field
    var deleteButton = currentDiv.find('.delete_button');
    deleteButton.click(function() {
      // Remove the separator line
      if (currentDiv.next().hasClass("separator")) {
        currentDiv.next().remove();
      }
      // Remove the div itself
      currentDiv.remove();
      self.fieldsList[currentFieldNum] = null;
    });

    // Initialize the dropdown box of fields with the available fields
    var select = $("#" + selectID);
    for (var i = 0; i < this.available_fields.field.length; i++) {

      // Change the displayed name of the field if mapping values have been
      // provided
      var displayedName = this.available_fields.field[i].name;
      if (!jQuery.isEmptyObject(this.mappingFieldNameValues) && displayedName in this.mappingFieldNameValues) {
        displayedName = this.mappingFieldNameValues[displayedName];
      }
      select.append('<option value="' + this.available_fields.field[i].name + '" type="' + this.available_fields.field[i].type + '">' + displayedName + '</option>');
    }

    // Implements the onchange method of the dropdown box of fields
    select.change(function() {
      var div = select.parent(); // Retrieve the main div of the field
      var type = $("#" + selectID + " option:selected").attr("type"); // Retrieve
      // the
      // type of
      // the
      // selected
      // field
      var field = $("#" + selectID + " option:selected").attr("value"); // Retrieve
      // the
      // fieldname
      // of
      // the
      // selected
      // field
      var selectSave = select.detach(); // Detach the dropdown box of fields as
      // it is saved it with all the assigned
      // attributes and javascript methods
      var selectOperatorSave = selectOperator.detach(); // Detach the dropdown
      // box of operator as it
      // is saved it with all
      // the assigned
      // attributes and
      // javascript methods
      var deleteButtonSave = deleteButton.detach(); // Detach the delete button
      // as it is saved with all
      // its attributes and
      // javascript methods
      div.html(''); // Empty the main div
      // Reconstruct the div with the new field selected
      selectOperatorSave.appendTo(div); // Attach the operator dropdown box
      selectSave.appendTo(div); // Attach the fields dropdown box
      deleteButtonSave.appendTo(div); // Attach the delete button
      var newOperator = div.find('.select-operator'); // Retrieve the correct
      // operator dropdown box
      // because the reference
      // changes after each
      // detach/attach jquery
      // methods
      self.constructFilter(type, div, field, null, newOperator, currentFieldNum); // Builds
      // the
      // correct
      // UI
      // associated
      // to
      // the
      // new
      // selected
      // field
    });

    // Add a separator line
    $("#exec_adv_search").before('<span class="separator">');
    // Attach the add field button that was detached to build the UI
    $("#exec_adv_search").before(addButton);
    // Set/reset the tooltip of the add button and enable it
    $('#add_adv_field').attr("title", window.i18n.msgStore['advancedSearch-add-filter-tooltip']);
    self.elm.tooltip('enable');

    // Populate fields if possible
    if (fieldName != null && fieldName != undefined && fieldName != "") {
      // Set the dropdown to the fieldName value
      select.val(fieldName);
      // Get the div in which the UI filters will be added
      var div = select.parent();
      // Get the type of the field from the dropdown
      var type = $("#" + selectID + " option:selected").attr("type");
      // Build the filters UI
      self.constructFilter(type, div, fieldName, values, selectOperator, currentFieldNum);
    }
  },

  // Method that constructs the proper filter UI corresponding to the field
  // provided and the other parameters
  constructFilter : function(type, elm, field, values, operator, fieldNum) {
    var self = this;
    var fieldNameExactExpr = field;

    // If the provided field has a specific Solr field for exact match query
    // then set the fieldNameExactExpr with the name of the Solr exact field
    if (field != null && field != undefined && this.exactFieldsList != null && this.exactFieldsList != undefined && field in this.exactFieldsList) {
      fieldNameExactExpr = this.exactFieldsList[field];
    } else if (field == null) {
      // Exact fields for basic search
      fieldNameExactExpr = [ "exactContent", "exactTitle" ];
    }

    var autocompleteSuggester = null;
    if (field in self.autocompleteFields) {
      autocompleteSuggester = self.autocompleteFields[field];
    }

    var fixedValues = null;
    if (field in self.fixedValuesFields) {
      fixedValues = self.fixedValuesFields[field];
    }

    // Create the AjaxFranceLabs.AdvancedSearchField object with the provided
    // values and initialize it
    var newField = new AjaxFranceLabs.AdvancedSearchField({
      type : type,
      elm : elm,
      id : "field_" + fieldNum,
      field : field,
      values : values,
      operator : operator,
      fieldNameExactExpr : fieldNameExactExpr,
      autocompleteSuggester : autocompleteSuggester,
      fixedValues : fixedValues
    });
    // If the manager is passed through the constructor it will trigger a "too
    // much recursion" error. So this is the only way
    newField.manager = self.manager;
    newField.init(); // The init method will build the UI
    this.fieldsList[fieldNum] = newField; // Save this field object to the list
    // of effective fields

    // Put limit on fields
    elm.find("input[type=text]").each(function(index) {
      $(this).attr("maxlength", self.maxChar);
    });
  },

  beforeRequest : function() {

    if ($(this.elm).is(':visible') && !this.manager.store.isParamDefined('original_query')) {
      var finalFilter = "";
      var searchBarFilter = "";

      // for each effective fields in the list, retrieve its operator and filter
      // value and add them to the global finalFilter variable
      for (var i = 0; i < this.fieldsList.length; i++) {
        if (this.fieldsList[i] != null && this.fieldsList[i] != undefined && this.fieldsList[i].getFilter() != "") {
          var operator = "";
          if (this.fieldsList[i].operator != null && this.fieldsList[i].operator != undefined && this.fieldsList[i].operator.val() == "OR") {
            operator = "OR ";
          } else {
            operator = "AND ";
          }
          finalFilter += " " + operator + this.fieldsList[i].getFilter();
        }
      }
      finalFilter = finalFilter.trim();

      // Clean finalFilter : remove the operator that starts the final filter as
      // it is useless
      if (finalFilter.startsWith("OR")) {
        finalFilter = finalFilter.substring(2).trim();
      } else if (finalFilter.startsWith("AND")) {
        finalFilter = finalFilter.substring(3).trim();
      }

      // If the finalFilter is empty then replace it by the 'search everything'
      // expression '*:*' and let the search bar empty
      // otherwise fill the search bar with the finalFilter value
      if (finalFilter == "") {
        finalFilter = "*:*";
      } else {
        searchBarFilter = finalFilter;
      }

      // Replace the query that was in the manager by the finalFilter value
      this.manager.store.remove('q');
      this.manager.store.addByValue('q', finalFilter);

      // Hide the advanced interface
      if (window.location.hash !== "#advancedsearch") {
        // Display the basic search
        this.displayBasicSearch();
      }
      $('.searchBar input').val(searchBarFilter);
      this.updateAddressBar();
    }
  },

  displayBasicSearch : function() {
    displaySearchView();
    clearActiveLinks();
    $("#basicSearchLink").addClass("active");
  },

  /**
   * Resets the inputs of the widget: underlying elements table and fields
   */
  reset : function() {
    this.elm.html('');
  },

  makeRequest : function() {
    this.cleanResults();
    this.manager.makeRequest();
  },

  /**
   * Cleans results area (same as for basic search)
   */
  cleanResults : function() {

    this.manager.store.remove('start');

    $.each(this.manager.widgets, function(index, widget) {
      if (typeof widget.pagination !== "undefined") {
        widget.pagination.pageSelected = 0;
      }
    });
  },

  /**
   * Update the address bar and generates the id for the query
   */
  updateAddressBar : function() {

    window.history.pushState('Object', 'Title', window.location.pathname + '?query=' + this.manager.store.get('q').val() + '&lang=' + window.i18n.language + window.location.hash);

    this.manager.store.addByValue("id", UUID.generate());
  }
});
