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
AjaxFranceLabs.AdvancedSearchTable = AjaxFranceLabs.Class.extend({

	//Variables

	parent : null,

	elm : null,

	title : '',

	description : '',

	fieldStore : [],

	manager : null,

	//Methods

	init : function() {
		$(this.parent).append('<div class="advTable">');
		this.elm = $(this.parent).find('.advTable:last');
		var elm = $(this.elm);
		elm.append('<span class="title">').find('.title').append('<span class="left">').append('<span class="right">').find('.left').append(this.title).parent().find('.right').append(this.description);
		for (var field in this.fieldStore) {
			this.fieldStore[field].parent = this.elm;
			this.fieldStore[field].manager = this.manager;
			this.fieldStore[field].init();
		}
		AjaxFranceLabs.addMultiElementClasses(elm.find('.field'));
		
		// If we have more than one field in advanced search
		if (this.fieldStore.length > 1){
			// Take all the fields except the last one, and add the span for the boolean operators below the current element
			elm.find('.field:not(:last-child)').after('<span class="advSearchBooleanOperator left"></span>');
			// Insert AND and OR radios
			elm.find('.advSearchBooleanOperator')
				.append('<span><input type="radio" name="advSearchRadios" class="radio" value="AND" checked="checked" id="advancedSearchRadioAnd"><label for="advancedSearchRadioAnd"><span>&nbsp;</span>AND</label></span>')
				.append('<span><input type="radio" name="advSearchRadios" class="radio" value="OR" id="advancedSearchRadioOr"><label for="advancedSearchRadioOr"><span>&nbsp;</span>OR</label></span>')
				.append('<span><input type="radio" name="advSearchRadios" class="radio" value="NOT" id="advancedSearchRadioNot"><label for="advancedSearchRadioNot"><span>&nbsp;</span>NOT</label></span>');
		}
	},

	addField : function(field) {
		this.fieldStore.push(field);
	},
	
	reset : function() {
		for (var field in this.fieldStore){
			this.fieldStore[field].reset();
		}
		$(this.elm).find('input#advancedSearchRadioAnd').prop('checked', true);
	}
});
