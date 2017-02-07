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
AjaxFranceLabs.NewAdvancedSearchField = AjaxFranceLabs.Class.extend({

	//Variables

	elm : null,

	autocomplete : false,

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
	
	dateSelectorModule : null,
	
	values : null,

	field : null,

	//Methodes

	init : function() {
		if(this.type != "tdate" && this.type != "date" && this.type != "int" && this.type != "long" && this.type != "float" && this.type != "double" && this.type != "tint" && this.type != "tlong" && this.type != "tfloat" && this.type != "tdouble") {
			
			// The field is of Text type
			this.buildTextFieldFilterUI();
			
		} else if (this.type != "int" && this.type != "long" && this.type != "float" && this.type != "double" && this.type != "tint" && this.type != "tlong" && this.type != "tfloat" && this.type != "tdouble") {
			
			// The field is of date or tdate type
			this.dateSelectorModule = new AjaxFranceLabs.DateSelectorFacetModule({
				elm : this.elm,
				hideGo : true,
				id : "field_" + this.fieldNumber,
				field : this.field
			});
			this.dateSelectorModule.createDateSelectorDiv();
			
		} else {
			
			// The field is of number type
			this.buildNumericFieldFilterUI();
		}
	},
	
	buildTextFieldFilterUI : function() {
		
		this.elm.append('<div class="all_words_line">').append('<div class="exact_expression_line">').append('<div class="at_least_one_word_line">').append('<div class="none_of_these_words_line">');
		var all_words_line = this.elm.find('.all_words_line');
		var at_least_one_word_line = this.elm.find('.at_least_one_word_line');
		var exact_expression_line = this.elm.find('.exact_expression_line');
		var none_of_these_words_line = this.elm.find('.none_of_these_words_line');
		all_words_line.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['allWords'] + '</label>').append('<input type="text" />');
		
		at_least_one_word_line.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['atLeastOneWord'] + '</label>').append('<input type="text" />');
		
		exact_expression_line.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['exactExpression'] + '</label>').append('<input type="text" />');
		
		none_of_these_words_line.append('<span class="left">').find('.left').append('<label>' + window.i18n.msgStore['noneOfTheseWords'] + '</label>').append('<input type="text" />');
		
		if(this.values != null && this.values != undefined) {
			all_words_line.find('input').val(this.values["all_words_value"]);
			at_least_one_word_line.find('input').val(this.values["at_least_one_word_value"]);
			exact_expression_line.find('input').val(this.values["exact_expression_value"]);
			none_of_these_words_line.find('input').val(this.values["none_of_these_words_value"]);
		}
	},
	
	buildNumericFieldFilterUI : function() {
		
		this.elm.append('<div><label>From: </label><input type="number" class="fromNum"></input><label>To: </label><input type="number" class="toNum"></input></div>');
	},

	getFilter : function() {
		if(this.dateSelectorModule != null) {
			return this.dateSelectorModule.getFilter();
		} else if(this.type != "tdate" && this.type != "date" && this.type != "int" && this.type != "long" && this.type != "float" && this.type != "double" && this.type != "tint" && this.type != "tlong" && this.type != "tfloat" && this.type != "tdouble") {
			
			return this.getTextFieldFilter();
			
		} else {
			return this.getNumberFilter();
		}
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
		var all_words_value = this.elm.find('.all_words_line').find('input').val();
		var at_least_one_word_line_value = this.elm.find('.at_least_one_word_line').find('input').val();
		var exact_expression_line_value = this.elm.find('.exact_expression_line').find('input').val();
		var none_of_these_words_line_value = this.elm.find('.none_of_these_words_line').find('input').val();
		
		var filter = "";
		if(all_words_value != null && all_words_value != undefined && all_words_value != "") {
			filter = all_words_value.trim();
		}
		
		if(exact_expression_line_value != null && exact_expression_line_value != undefined && exact_expression_line_value != "") {
			var splittedValue = exact_expression_line_value.trim().split(" ");
			if(splittedValue.length > 1) {
				filter += " \"" + exact_expression_line_value.trim() + "\"";
			} else {
				filter += " " + exact_expression_line_value.trim();
			}
		}
		
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
		
		if(none_of_these_words_line_value != null && none_of_these_words_line_value != undefined && none_of_these_words_line_value != "") {
			var splittedValue = none_of_these_words_line_value.trim().split(" ");
			for(var i=0; i<splittedValue.length; i++) {
				filter += " -" + splittedValue[i];
			}
		}
		
		if(filter != "") {
			var finalFilter = "";
			if(this.field != null && this.field != "") {
				finalFilter += this.field + ":";
			}
			if(filter.split(" ").length > 1) {
				if(finalFilter != "") {
					finalFilter += "(" + filter.trim() + ")";
				} else {
					finalFilter = filter.trim();
				}
			} else {
				finalFilter += filter.trim();
			}
			return finalFilter;
		} else {
			return filter;
		}
	}
});
