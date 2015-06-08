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
	},

	addField : function(field) {
		this.fieldStore.push(field);
	}
});
