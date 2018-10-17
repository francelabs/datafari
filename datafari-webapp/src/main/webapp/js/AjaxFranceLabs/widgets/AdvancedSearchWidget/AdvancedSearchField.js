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
AjaxFranceLabs.AdvancedSearchField = AjaxFranceLabs.Class.extend({

	//Variables

	elm : null,
	
	autocompleteSuggester : null,
	
	fixedValues : null,
	
	dateSelectorModule : null,
	
	values : null,

	field : null,
	
	fieldNameExactExpr : null,

	//Methodes

	init : function() {
		if(this.fixedValues != null) {
			this.buildFixedValuesFilterUI();
		} else if(this.type != "tdate" && this.type != "date" && this.type != "int" && this.type != "long" && this.type != "float" && this.type != "double" && this.type != "tint" && this.type != "tlong" && this.type != "tfloat" && this.type != "tdouble") {
			
			// The field is of Text type
			this.buildTextFieldFilterUI();
			
		} else if (this.type != "int" && this.type != "long" && this.type != "float" && this.type != "double" && this.type != "tint" && this.type != "tlong" && this.type != "tfloat" && this.type != "tdouble") {
			
			
			var fromInitValue = null;
			var toInitValue = null;
			
			// Initialize fromValue and toValue if provided
			if(this.values != null && this.values != undefined) {
				if(this.values["fromValue"] != null && this.values["fromValue"] != undefined) {
					if(this.values["fromValue"] == "*") {
						fromInitValue = "";
					} else {
						fromInitValue = this.values["fromValue"];
					}
				}
				if(this.values["toValue"] != null && this.values["toValue"] != undefined) {
					if(this.values["toValue"] == "*") {
						toInitValue = "";
					} else {
						toInitValue = this.values["toValue"];
					}
				}
			}
			
			// The field is of date or tdate type
			this.dateSelectorModule = new AjaxFranceLabs.DateSelectorFacetModule({
				elm : this.elm,
				hideGo : true,
				id : "ds_" + this.field,
				field : this.field,
				displayError : true,
				fromInitValue : fromInitValue,
				toInitValue : toInitValue
			});
			this.dateSelectorModule.createDateSelectorDiv();
			
		} else {
			
			// The field is of number type
			this.buildNumericFieldFilterUI();
		}
	},
	
	buildFixedValuesFilterUI : function() {
		var self = this;
		var select = $("<select class=\"select-equals dropdown-field dropdown\"><option selected value=''>" + window.i18n.msgStore['selectValue'] + "</option></select>");
		var select2 = $("<select class=\"select-not-equals dropdown-field dropdown\"><option selected value=''>" + window.i18n.msgStore['selectValue'] + "</option></select>");
		for(var i=0; i<self.fixedValues.length; i++) {
			var value = "";
			var label = "";
			if(typeof self.fixedValues[i] === "object") {
				value = self.fixedValues[i].value;
				label = self.fixedValues[i].label;
			} else {
				value = self.fixedValues[i];
				label = value;
			}
			select.append("<option value=\"" + value + "\">" + label + "</option>");
			select2.append("<option value=\"" + value + "\">" + label + "</option>")
		}
		// Init select with init value if available
		if(this.values != null && this.values != undefined) {
			var val = "";
			if(this.values["all_words_value"] != null && this.values["all_words_value"] != undefined && this.values["all_words_value"] != "") {
				val = this.values["all_words_value"];
			} else if(this.values["exact_expression_value"] != null && this.values["exact_expression_value"] != undefined && this.values["exact_expression_value"] != "") {
				val = this.values["exact_expression_value"];
			}
			select.val(val);
			
			if(this.values["none_of_these_words_value"] != null && this.values["none_of_these_words_value"] != undefined && this.values["none_of_these_words_value"] != "") {
				//Remove double quotes from value
				var cleanVal = this.values["none_of_these_words_value"].replace(/\"/g,"");
				select2.val(cleanVal);
			}
			
		}
		
		//Construct the divs
		var divEquals = $('<div class="equals-line"></div>');
		var divNotEquals = $('<div class="not-equals-line"></div>');
		divEquals.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['equals'] + '</label>').append(select);
		divNotEquals.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['not-equals'] + '</label>').append(select2);
		self.elm.append(divEquals).append(divNotEquals);
	},
	
	buildTextFieldFilterUI : function() {
		
		var self = this;
		this.elm.append('<div class="all_words_line">').append('<div class="exact_expression_line">').append('<div class="at_least_one_word_line">').append('<div class="none_of_these_words_line">');
		var all_words_line = this.elm.find('.all_words_line');
		var at_least_one_word_line = this.elm.find('.at_least_one_word_line');
		var exact_expression_line = this.elm.find('.exact_expression_line');
		var none_of_these_words_line = this.elm.find('.none_of_these_words_line');
		all_words_line.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['allWords'] + '</label>').append('<input type="text" class="filter-input" />');
		
		at_least_one_word_line.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['atLeastOneWord'] + '</label>').append('<input type="text" class="filter-input" />');
		
		exact_expression_line.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['exactExpression'] + '</label>').append('<input type="text" class="filter-input" />');
		
		none_of_these_words_line.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['noneOfTheseWords'] + '</label>').append('<input type="text" class="filter-input" />');
		
		// Init fields with init values if availables
		if(this.values != null && this.values != undefined) {
			all_words_line.find('input').val(this.values["all_words_value"]);
			at_least_one_word_line.find('input').val(this.values["at_least_one_word_value"]);
			exact_expression_line.find('input').val(this.values["exact_expression_value"]);
			none_of_these_words_line.find('input').val(this.values["none_of_these_words_value"]);
		}
		
		if(self.autocompleteSuggester != null && self.autocompleteSuggester != undefined) {
			// All words autocomplete
			var autocompleteAllWords = new AjaxFranceLabs.AdvancedAutocompleteModule(
				{
					servlet : self.autocompleteSuggester,
					elm : all_words_line.find('input')
				}	
			);
			// If the manager is passed through the constructor it will trigger a "too much recursion" error. So this is the only way
			autocompleteAllWords.manager = self.manager;
			autocompleteAllWords.init();
			
			
			// At least one word autocomplete
			var autocompleteAtLeastOneWord = new AjaxFranceLabs.AdvancedAutocompleteModule(
					{
						servlet : self.autocompleteSuggester,
						elm : at_least_one_word_line.find('input')
					}	
				);
				// If the manager is passed through the constructor it will trigger a "too much recursion" error. So this is the only way
			autocompleteAtLeastOneWord.manager = self.manager;
			autocompleteAtLeastOneWord.init();
			
			
			// Exact expression autocomplete
			var autocompleteExactExpression = new AjaxFranceLabs.AdvancedAutocompleteModule(
				{
					servlet : self.autocompleteSuggester,
					elm : exact_expression_line.find('input')
				}	
			);
			// If the manager is passed through the constructor it will trigger a "too much recursion" error. So this is the only way
			autocompleteExactExpression.manager = self.manager;
			autocompleteExactExpression.init();
					
			
			// None of these words autocomplete
			var autocompleteNoneOfTheseWords = new AjaxFranceLabs.AdvancedAutocompleteModule(
				{
					servlet : self.autocompleteSuggester,
					elm : none_of_these_words_line.find('input')
				}	
			);
			// If the manager is passed through the constructor it will trigger a "too much recursion" error. So this is the only way
			autocompleteNoneOfTheseWords.manager = self.manager;
			autocompleteNoneOfTheseWords.init();
		}
	},
	
	buildNumericFieldFilterUI : function() {
		
		this.elm.append('<div><label>From: </label><input type="number" class="fromNum"></input><label>To: </label><input type="number" class="toNum"></input></div>');
		
		var fromInitValue = null;
		var toInitValue = null;
		
		// Initialize fromValue and toValue if provided
		if(this.values != null && this.values != undefined) {
			if(this.values["fromValue"] != null && this.values["fromValue"] != undefined) {
				if(this.values["fromValue"] == "*") {
					fromInitValue = "";
				} else {
					fromInitValue = this.values["fromValue"];
				}
			}
			if(this.values["toValue"] != null && this.values["toValue"] != undefined) {
				if(this.values["toValue"] == "*") {
					toInitValue = "";
				} else {
					toInitValue = this.values["toValue"];
				}
			}
		}
		
		if(fromInitValue != null) {
			this.elm.find('.fromNum').val(fromInitValue);
		}
		
		if(toInitValue != null) {
			this.elm.find('.toNum').val(toInitValue);
		}
	},

	getFilter : function() {
		if(this.fixedValues != null) {
			return this.getFixedValueFilter();
		} else if(this.dateSelectorModule != null) {
			return this.dateSelectorModule.getFilter();
		} else if(this.type != "tdate" && this.type != "date" && this.type != "int" && this.type != "long" && this.type != "float" && this.type != "double" && this.type != "tint" && this.type != "tlong" && this.type != "tfloat" && this.type != "tdouble") {
			
			return this.getTextFieldFilter();
			
		} else {
			return this.getNumberFilter();
		}
	},
	
	getFixedValueFilter : function()
	{
		var filter = "";
		if(this.elm.find(".select-equals").val() != null && this.elm.find(".select-equals").val() != undefined && this.elm.find(".select-equals").val() != "") {
			filter = this.field + ":(\"" + this.elm.find(".select-equals").val() + "\"";
		}
		
		if(this.elm.find(".select-not-equals").val() != null && this.elm.find(".select-not-equals").val() != undefined && this.elm.find(".select-not-equals").val() != "") {
			if(filter == "") {
				filter = this.field + ":(-\"" + this.elm.find(".select-not-equals").val() + "\"";
			} else {
				filter += " -" + "\"" + this.elm.find(".select-not-equals").val() + "\"";
			}
		}
		
		if(filter != "") {
			filter += ")";
		}
		return filter;
	},	
	
	getNumberFilter : function() {
		var fromValue = this.elm.find('.fromNum').val();
		var toValue = this.elm.find('.toNum').val();
		var filter = "";
		
		if((fromValue != null && fromValue != undefined && fromValue != "") || (toValue != null && toValue != undefined && toValue != "")) {
			filter = this.field + ":[";
			
			if(fromValue != null && fromValue != undefined && fromValue != "") {
				filter += fromValue + " TO ";
			} else {
				filter += "* TO ";
			}
			
			if(toValue != null && toValue != undefined && toValue != "") {
				filter += toValue + "]";
			} else {
				filter += "*]";
			}
		}
		
		return filter;
	},
	
	getTextFieldFilter : function() {
		var all_words_value = this.elm.find('.all_words_line').find('input').val().trim();
		var at_least_one_word_line_value = this.elm.find('.at_least_one_word_line').find('input').val().trim();
		var exact_expression_line_value = this.elm.find('.exact_expression_line').find('input').val().trim();
		var none_of_these_words_line_value = this.elm.find('.none_of_these_words_line').find('input').val().trim();
		
		var exactExpressionsRegex = /"[^\"]+"/g;
		
		// Global filter
		var filter = "";
		
		// Add the all_words filter if available to the global filter
		if(all_words_value != null && all_words_value != undefined && all_words_value != "") {
			filter += all_words_value.trim();
		}
		
		// Add the exact expression filter if available and if no specific Solr field for exact expression have been provided
		if(exact_expression_line_value != null && exact_expression_line_value != undefined && exact_expression_line_value != "" && (this.fieldNameExactExpr == null || this.fieldNameExactExpr == undefined || this.fieldNameExactExpr == this.field)) {
			var splittedValue = exact_expression_line_value.trim().split(" ");
			if(splittedValue.length > 1) {
				filter += " \"" + exact_expression_line_value.trim() + "\"";
			} else {
				filter += " " + exact_expression_line_value.trim();
			}
		}
		
		// Add the at least one word filter if available to the global filter
		if(at_least_one_word_line_value != null && at_least_one_word_line_value != undefined && at_least_one_word_line_value != "") {
			var splittedValue = at_least_one_word_line_value.trim().split(" ");
			for(var i=0; i<splittedValue.length; i++) {
				if(i == 0) {
					filter += " " + splittedValue[i];
				} else {
					filter += " OR " + splittedValue[i];
				}
			}
		}
		
		// Add the none of these words filter if available to the global filter
		if(none_of_these_words_line_value != null && none_of_these_words_line_value != undefined && none_of_these_words_line_value != "") {
			
			// Starts with the exact negative expressions like -"france labs"
			var exactNegativeExpressions = none_of_these_words_line_value.match(exactExpressionsRegex);
			if(exactNegativeExpressions != null && exactNegativeExpressions != undefined) {
				for(var cptNgtExact=0; cptNgtExact<exactNegativeExpressions.length; cptNgtExact++) {
					var exactNegativeExpression = exactNegativeExpressions[cptNgtExact];
					filter += " -" + exactNegativeExpression;
					var exactNgtExprIndex = none_of_these_words_line_value.search(exactNegativeExpression);
					// If there is a space in front of the exact expression then remove it + the exact expression, otherwise only remove the exact expression
					if(exactNgtExprIndex > 0 && none_of_these_words_line_value.substring(exactNgtExprIndex -1, exactNgtExprIndex) == " ") {
						none_of_these_words_line_value = none_of_these_words_line_value.replace(" " + exactNegativeExpression, "");
					} else {
						none_of_these_words_line_value = none_of_these_words_line_value.replace(exactNegativeExpression, "");
					}
				}
			}
			
			// Now handle the "normal" negative words like -test
			var splittedValue = none_of_these_words_line_value.trim().split(" ");
			for(var i=0; i<splittedValue.length; i++) {
				if(splittedValue[i].trim() != "") {
					filter += " -" + splittedValue[i];
				}
			}
		}
		
		// If the filter is not empty then format/clean it before returning it, return the empty string otherwise 
		if(filter != "" || (this.fieldNameExactExpr != null && this.fieldNameExactExpr != undefined && exact_expression_line_value != null && exact_expression_line_value != undefined && exact_expression_line_value != "")) {
			var finalFilter = "";
			
			// If the fieldName is available it then add it to the final filter
			if(this.field != null && this.field != "" && filter != "") {
				finalFilter += this.field + ":";
			}
			
			// If the filter contains multiple expressions (identified by the number of spaces) 
			// then it needs to be surrounded by parenthesis
			// unless it is the filter for the basic search (in which case the finalFilter variable is empty because no fieldName has been found)
			if(filter.split(" ").length > 1) {
				if(finalFilter != "") {
					finalFilter += "(" + filter.trim() + ")";
				} else {
					finalFilter = filter.trim();
				}
			} else {
				finalFilter += filter.trim();
			}
			
			// if a specific Solr field is available for the exact expression then add it to the finalFilter
			if(this.fieldNameExactExpr != null && this.fieldNameExactExpr != undefined && this.fieldNameExactExpr != this.field && exact_expression_line_value != null && exact_expression_line_value != undefined && exact_expression_line_value != "") {
				if(Array.isArray(this.fieldNameExactExpr)) {
					var exactFilter = "(";
					for(var i=0; i<this.fieldNameExactExpr.length; i++) {
						if(exactFilter != "(") {
							exactFilter += "OR ";
						}
						exactFilter += this.fieldNameExactExpr[i] + ":" + "\"" + exact_expression_line_value.trim() + "\" ";
					}
					exactFilter += ")";
					finalFilter += " " + exactFilter;
				} else {
					finalFilter += " " + this.fieldNameExactExpr + ":" + "\"" + exact_expression_line_value.trim() + "\" ";
				}
				finalFilter = finalFilter.trim();
			}
			
			return finalFilter;
		} else {
			return filter;
		}
	}
});
