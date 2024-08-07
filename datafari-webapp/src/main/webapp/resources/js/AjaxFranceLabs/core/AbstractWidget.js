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
AjaxFranceLabs.AbstractWidget = AjaxFranceLabs.Class.extend({

	//Variables

	id : null,

	elm : null,

	manager : null,

	initialized : false,

	type : null,

	//Methods

	init : function() {
		if (!this.initialized) {
			this.initialized = true;
			this.buildWidget();
		}
	},

	buildWidget : function() {
	},

	beforeRequest : function() {
	},

	afterRequest : function() {
	},

	requestError : function(status, error, jqxhr) {
	},

	/**
	 * Interface method to reset the model of the widget
	 */
	reset : function() {
	}
});
