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
AjaxFranceLabs.AdvancedSearchWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	tables : [],

	resizable : false,

	options : {},

	optionsExtend : true,

	type : 'advancedSearch',
	
	available_fields : null,
	
	advTable : null,
	
	fieldNumber : 0,
	
	fieldsList : [],

	//Methods

	buildWidget : function() {
		
		var self = this, elm = $(this.elm);
		$.get("./GetFieldsInfo", function(data) {
			self.available_fields = data;
		}, "json");
		elm.addClass('advancedSearchWidget').addClass('widget').attr('widgetId', this.id);
		
		AjaxFranceLabs.addMultiElementClasses($(this.elm).find('.advTable'));
		
		elm.find('.advTable input').keypress(function(event) {
			if (event.keyCode === 13) {
				self.makeRequest();
			}
		});
		
		if (this.resizable === true) {
			var options = {
				handles : 's',
				minHeight : 135,
				maxHeight : $(self.elm).find('.wrapper').innerHeight()
			};
			if (this.optionsExtend) {
				$.extend(this.options, this.options, options);
			}
			elm.find('.wrapper').css('overflow', 'hidden').resizable(this.options).find('.wrapper').resizable({
				start : function(event, ui) {
					elm.find('.wrapper').css('height', '100%').resizable("option", "maxHeight", elm.find('.wrapper').height());
				},
				stop : function(event, ui) {
					if (elm.find('.wrapper').resizable("option", "maxHeight") === elm.find('.wrapper').height())
						elm.find('.wrapper').css('height', '100%');
				}
			});
		}
		
		// Hidden by default: basic search is displayed
		elm.hide();		
	},
	
	buildStartingUI : function() {
		var self = this, elm = $(this.elm);
		
		var baseQuery = this.manager.store.get('q').val();
		
		
		
		var baseQueryRegEx = /(AND\s|OR\s)*[^\s\(\[\:]+:/g;
		var indexOf = baseQuery.search(baseQueryRegEx);
		var baseSearch = "";
		
		if(indexOf != -1) {
			baseSearch = baseQuery.substring(0, indexOf);
		} else {
			baseSearch = baseQuery;
		}
		console.log("base search: " + baseSearch);
		
		elm.append('<div class="advTable">');
		this.advTable = elm.find('.advTable');
		
		this.advTable.append('<span class="title left">');
		
		this.advTable.find('.title').append(window.i18n.msgStore['advancedSearch-label']);
		
		this.advTable.append('<span class="separator">');
		
		this.advTable.append('<div id="adv_base_search">');
		
		var adv_base_search = this.advTable.find('#adv_base_search');
		adv_base_search.append('<span class="subtitle left">').find('.left').append('Base search');
		var baseSearchValues = self.extractFilterFromText(baseSearch, false, true);
		this.constructFilter("string", adv_base_search, null, baseSearchValues);
		
		
		this.advTable.append('<span class="separator">');
		
		this.advTable.append('<div><span id="add_adv_field" class="button">-- Add field --</span></div>');
		$('#add_adv_field').click(function() {
			self.addField();
		});
		
		this.advTable.append('<button id="exec_adv_search">' + window.i18n.msgStore['advancedSearch-makesearch-btn'] + '</button>').find('button').click(function() {
			self.makeRequest();
		});
		
		// Add other fields filter
		var superFieldsRegEx = /(AND\s|OR\s)*[^\s\(\[\:]+:(\[[^\]]+\]|\([^\)]+\)|[^\s\(\]]+)/g;
		var fields = baseQuery.match(superFieldsRegEx);
		if(fields != null && fields != undefined && fields != "") {
			for(var cpt=0; cpt<fields.length; cpt++) {
				var fieldExpression = fields[cpt];
				console.log(fieldExpression);
				
				// Extract operator if there is one
				var operator = "";
				if(fieldExpression.startsWith('AND') || fieldExpression.startsWith('OR')) {
					operator = fieldExpression.split(" ")[0];
					console.log("Opérator: " + operator);
					fieldExpression = fieldExpression.substring(operator.length + 1);
					console.log("New expression: " + fieldExpression);
				}
				
				// Extract fieldname
				var fieldname = fieldExpression.substring(0, fieldExpression.indexOf(":"));
				var negativeExpression = false;
				// If the firest char of the fieldname is -, it is because it is a negative filter
				// need to remove this char to get the real fieldname
				if(fieldname.startsWith("-")) {
					fielname = fieldname.substring(1);
					negativeExpression = true;
				}
				console.log("fieldname: " + fieldname);
				fieldExpression = fieldExpression.substring(fieldname.length +1);
				console.log("New expression: " + fieldExpression);
				
				// Define field type
				var fieldType = "";
				for(var i=0; i<self.available_fields.field.length; i++) {
					if(self.available_fields.field[i].name == fieldname) {
						fieldType = self.available_fields.field[i].type[0];
						break;
					}
				}
				if(fieldType == "tdate" || fieldType == "date") {
					fieldType = "date";
				} else if (fieldType == "int" || fieldType == "long" || fieldType == "float" || fieldType == "double" || fieldType == "tint" || fieldType == "tlong" || fieldType == "tfloat" || fieldType == "tdouble") {
					fieldType = "num";
				} else {
					fieldType = "text";
				}
				console.log("fieldType: " + fieldType);
				
				// Define filters values
				if(fieldType == "text") {
					
					var extractedValues = self.extractFilterFromText(fieldExpression, negativeExpression, false);
					self.addField(fieldname, extractedValues);
				} else {
					if(fieldExpression.startsWith('[')) {
						var values = fieldExpression.split(" ");
						var fromValue = values[0].substring(1);
						var toValue = values[2].substring(0, values[2].length - 1);
						console.log("fromValue: " + fromValue);
						console.log("toValue: " + toValue);
					}
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
		
		andExpressionRegex = /[^\s\[\(\]\)\-\"(AND|OR)]+\sAND\s[^\s\[\(\]\)\-\"(AND|OR)]+/g;
		orExpressionRegex = /[^\s\[\(\]\)\-\"(AND|OR)]+\sOR\s[^\s\[\(\]\)\-\"(AND|OR)]+/g;
		
		cleanANDRegex = /(AND\s(?:(?![^\s\[\(]+AND)[^\s\[\(]+)\s|[^\s\[\(]+\sAND)/g;
		cleanORRegex = /(OR\s(?:(?![^\s\[\(]+OR)[^\s\[\(]+)\s|[^\s\[\(]+\sOR)/g;
		
		if(!text.startsWith("(") && !isBasicSearchText) {
			if(negativeExpression) {
				none_of_these_words_value=text;
			} else {
				if(text.startsWith("\"")) {
					exact_expression_value = text.substring(1, text.length - 1);
				} else {
					all_words_value = text;
				}
			}
		} else {
			//Remove () that surround the expression
			if(!isBasicSearchText) {
				text = text.substring(1, text.length -1);
			}
			
			// Negative words and exact match
			var globalValues = text.split(' ');
			for(var cptGlobal=0; cptGlobal < globalValues.length; cptGlobal++) {
				var gbValue = globalValues[cptGlobal];
				if(gbValue.startsWith("-")) {
					none_of_these_words_value += " " + gbValue.substring(1);
					if(text.search(gbValue) > 0) {
						text = text.replace(" " + gbValue, "");
					} else {
						text = text.replace(gbValue, "");
					}
				} else if (gbValue.startsWith("\"")) {
					exact_expression_value += " " + gbValue.substring(1, gbValue.length - 1);
					text = text.replace(" " + gbValue, "");
				}
			}
			
			// Find OR expressions
			var tempFieldExpression = text;
			while(tempFieldExpression.match(orExpressionRegex) != null) {
				var orExpressions = tempFieldExpression.match(orExpressionRegex);
				if(orExpressions != null) {
					for(var cptExpr=0; cptExpr<orExpressions.length; cptExpr++) {
						var exprValues = orExpressions[cptExpr].split(" ");
						if(at_least_one_word_value.indexOf(exprValues[0]) == -1) {
							at_least_one_word_value += " " + exprValues[0];
						}
						if(at_least_one_word_value.indexOf(exprValues[2]) == -1) {
							at_least_one_word_value += " " + exprValues[2];
						}
						var exprIndex = tempFieldExpression.search(exprValues[0] + " OR");
						if(exprIndex != -1) {
							if(tempFieldExpression.substring(exprIndex - 1).startsWith(' ')) {
								tempFieldExpression = tempFieldExpression.replace(" " + exprValues[0] + " OR", "OR");
							} else {
								tempFieldExpression = tempFieldExpression.replace(exprValues[0] + " OR", "OR");
							}
						}
					}
				}
			}
			
			// Find AND expressions
			tempFieldExpression = text;
			while(tempFieldExpression.match(andExpressionRegex) != null) {
				var andExpressions = tempFieldExpression.match(andExpressionRegex);
				if(andExpressions != null) {
					for(var cptExpr=0; cptExpr<andExpressions.length; cptExpr++) {
						var exprValues = andExpressions[cptExpr].split(" ");
						if(all_words_value.indexOf(exprValues[0]) == -1) {
							all_words_value += " " + exprValues[0];
						}
						if(all_words_value.indexOf(exprValues[2]) == -1) {
							all_words_value += " " + exprValues[2];
						}
						var exprIndex = tempFieldExpression.search(exprValues[0] + " AND");
						if(exprIndex != -1) {
							if(tempFieldExpression.substring(exprIndex - 1).startsWith(' ')) {
								tempFieldExpression = tempFieldExpression.replace(" " + exprValues[0] + " AND", "AND");
							} else {
								tempFieldExpression = tempFieldExpression.replace(exprValues[0] + " AND", "AND");
							}
						}
					}
				}
			}
			
			// Clean OR expressions
			while(text.match(orExpressionRegex) != null) {
				var orExpressions = text.match(orExpressionRegex);
				if(orExpressions != null) {
					for(var cptExpr=0; cptExpr<orExpressions.length; cptExpr++) {
						var exprValues = orExpressions[cptExpr].split(" ");
						var exprIndex = text.search(exprValues[0] + " OR");
						if(exprIndex != -1) {
							if(text.substring(exprIndex - 1).startsWith(' ')) {
								text = text.replace(" " + exprValues[0] + " OR", "OR");
							} else {
								text = text.replace(exprValues[0] + " OR", "OR");
							}
						}
					}
				}
			}
			
			// Clean AND expressions
			while(text.match(andExpressionRegex) != null) {
				var andExpressions = text.match(andExpressionRegex);
				if(andExpressions != null) {
					for(var cptExpr=0; cptExpr<andExpressions.length; cptExpr++) {
						var exprValues = andExpressions[cptExpr].split(" ");
						var exprIndex = text.search(exprValues[0] + " AND");
						if(exprIndex != -1) {
							if(text.substring(exprIndex - 1).startsWith(' ')) {
								text = text.replace(" " + exprValues[0] + " AND", "AND");
							} else {
								text = text.replace(exprValues[0] + " AND", "AND");
							}
						}
					}
				}
			}
			
			while(text.match(cleanORRegex) != null) {
				var orExprToClean = text.match(cleanORRegex);
				for(var cptExprToClean=0; cptExprToClean<orExprToClean.length; cptExprToClean++) {
					text = text.replace(orExprToClean[cptExprToClean], "");
				}
			}
			text = text.replace(/OR/g, "");
			
			
			
			while(text.match(cleanANDRegex) != null) {
				var andExprToClean = text.match(cleanANDRegex);
				for(var cptExprToClean=0; cptExprToClean<andExprToClean.length; cptExprToClean++) {
					text = text.replace(andExprToClean[cptExprToClean], "");
				}
			}
			text = text.replace(/AND/g, "");
			
			text = text.trim();
			
			console.log("Final expression: " + text);
			
			var lastWords = text.split(" ");
			for(var cptLastWords=0; cptLastWords<lastWords.length; cptLastWords++) {
				all_words_value += " " + lastWords[cptLastWords];
			}
		}
		
		// Trim the values
		all_words_value = all_words_value.trim();
		exact_expression_value = exact_expression_value.trim();
		at_least_one_word_value = at_least_one_word_value.trim();
		none_of_these_words_value = none_of_these_words_value.trim();
		
		console.log("all_words_value: " + all_words_value);
		console.log("exact_expression_value: " + exact_expression_value);
		console.log("at_least_one_word_value: " + at_least_one_word_value);
		console.log("none_of_these_words_value: " + none_of_these_words_value);
		
		var returnValues = {};
		returnValues["all_words_value"] = all_words_value;
		returnValues["exact_expression_value"] = exact_expression_value;
		returnValues["at_least_one_word_value"] = at_least_one_word_value;
		returnValues["none_of_these_words_value"] = none_of_these_words_value;
		
		return returnValues;
		
		//self.addField(fieldname, all_words_value, exact_expression_value, at_least_one_word_value, none_of_these_words_value, null, null);
	},
	
	addField : function() {
		this.addField(null, values);
	},
	
	addField : function(fieldName, values) {
		var self = this;
		$('#add_adv_field').parent().before('<div class="adv_field">');
		this.fieldNumber++;
		var currentFieldNum = this.fieldNumber;
		var addButton = $('#add_adv_field').parent().detach();
		var selectID = "field_" + this.fieldNumber;
		var currentDiv = this.advTable.find('.adv_field').last();
		currentDiv.append('<select id="' + selectID + '"><option disabled selected value>Select a field</option></select> <span class="delete_button button"> X </span>');
		var deleteButton = currentDiv.find('.delete_button');
		deleteButton.click(function() {
			// Remove the separator line
			currentDiv.next().remove();
			// Remove the div itself
			currentDiv.remove();
			self.fieldsList[currentFieldNum] = null;
		});
		var select = $("#" + selectID);
		for(var i = 0 ; i < this.available_fields.field.length ; i++){
			select.append('<option value="' + this.available_fields.field[i].name + '" type="' + this.available_fields.field[i].type + '">' + this.available_fields.field[i].name + '</option>');
		}
		select.change(function() {
			var div = select.parent();
			var type = $("#" + selectID + " option:selected").attr("type");
			var field = $("#" + selectID + " option:selected").attr("value");
			var selectSave = select.detach();
			var deleteButtonSave = deleteButton.detach();
			div.html('');
			selectSave.appendTo(div);
			deleteButtonSave.appendTo(div);
			self.constructFilter(type, div, field);
		});
		$("#exec_adv_search").before('<span class="separator">');
		$("#exec_adv_search").before(addButton);
		
		// Populate fields if possible
		if(fieldName != null && fieldName != undefined && fieldName != "") {
			select.val(fieldName);
			var div = select.parent();
			var type = $("#" + selectID + " option:selected").attr("type");
			console.log("Add field " + fieldName + " of type " + type);
			self.constructFilter(type, div, fieldName, values);
		}
	},
	
	constructFilter : function(type, elm, field) {
		
		this.constructFilter(type, elm, field, null);
		
//		var newField = new AjaxFranceLabs.NewAdvancedSearchField({
//			type : type,
//			elm : elm,
//			id : "field_" + this.fieldNumber,
//			field : field
//		});
//		newField.init();
//		this.fieldsList[this.fieldNumber] = newField;
	},
	
	constructFilter : function(type, elm, field, values) {
		var newField = new AjaxFranceLabs.NewAdvancedSearchField({
			type : type,
			elm : elm,
			id : "field_" + this.fieldNumber,
			field : field,
			values : values
		});
		newField.init();
		this.fieldsList[this.fieldNumber] = newField;
	},

	beforeRequest : function() {
		
		if ($(this.elm).is(':visible') && !this.manager.store.isParamDefined('original_query')){
			var finalFilter = "";
			var searchBarFilter = "";
			
			for (var i=0; i < this.fieldsList.length; i++) {
				if(this.fieldsList[i] != null) {
					finalFilter += " " + this.fieldsList[i].getFilter();
				}
			}
			finalFilter = finalFilter.trim();
			if(finalFilter == "") {
				finalFilter = "*:*";
			} else {
				searchBarFilter = finalFilter;
			}
			this.manager.store.remove('q');
			this.manager.store.addByValue('q', finalFilter);
			
			// Hide the advanced interface
			this.elm.hide();
			
			// Display the basic search
			$('#searchBar').show();
			$('.searchBar input').val(searchBarFilter);
			this.updateAddressBar();
		}
		
//		// If the widget is displayed and we are not performing the spellchecker query
//		if ($(this.elm).is(':visible') && !this.manager.store.isParamDefined('original_query')){
//			
//			this.manager.store.remove('fq');
//			this.manager.store.remove('q');
//			
//			var qArr = [];
//			
//			for (var table in this.tables) {
//				for (var field in this.tables[table].fieldStore) {
//					var value = this.tables[table].fieldStore[field].getValue();
//					if (!AjaxFranceLabs.empty(value)) {
//						if (this.tables[table].fieldStore[field].filter === true) {
//							this.manager.store.addByValue('fq', value);
//						} else {
//							qArr.push('(' + value + ')');
//						}
//					}
//				}			
//					
//				if (qArr.length > 0){
//					// Create the query, combining the different search fields with boolean values from radio buttons (natural order from left to right)
//					this.manager.store.addByValue('q', qArr.join($(this.elm).find('.advSearchBooleanOperator .radio:checked').val()));
//				}
//				else {
//					this.manager.store.addByValue('q', '*:*');
//				}
//			}
//			
//			this.updateAddressBar();
//		}
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
		
		window.history.pushState('Object', 'Title',
				window.location.pathname + '?query=' + this.manager.store.get('q').val() + '&lang='
						+ window.i18n.language);

		this.manager.store.addByValue("id", UUID.generate());		
	}
});
