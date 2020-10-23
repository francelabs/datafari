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
AjaxFranceLabs.SelectSourcesWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'selectSources',
	select : null,
	previousSourceValue : null,
	sources : {},

	//Methods

	buildWidget : function() {
		var self = this;
		var allSelected = "";
		
		// Define the current value of the select, which will be set as the previous value
		var tmp = [];
		// Search for the 'source' param in the address bar
		location.search.substr(1).split("&").forEach(function (item) {
			tmp = item.split("=");
	        if (tmp[0] === "source") {
	        	self.previousSourceValue = decodeURIComponent(tmp[1]);
	        }
		});
		if(this.previousSourceValue == null || this.previousSourceValue == undefined) {
			this.previousSourceValue = "all";
		}
		if(this.previousSourceValue == "all") {
			allSelected = "selected";
		}
		
		// Build the 'Select' element
		this.select = $("<select class='selectSources'></select>");
		this.select.append("<option value='all' " + allSelected + ">" + window.i18n.msgStore['all']) + "</option>";
		for(var sourceLabel in this.sources) {
			var selected = "";
			if(this.previousSourceValue == this.sources[sourceLabel]) {
				selected = "selected";
			}
			this.select.append("<option value='" + this.sources[sourceLabel] + "' " + selected + ">" + sourceLabel + "</option>");
		}
		
		$(this.elm).addClass('selectSourcesWidget').addClass('widget').attr('widgetId', this.id).append(this.select);
	},

	beforeRequest : function() {
		// If the select as changed since the last request, force a page reload to redirect to the right page and trigger the right request handler
		// Else force the source value even if it has not changed (it also inits the value in the manager on the first page load)
		if(this.previousSourceValue != this.select.val()) {
			
			if(location.search.indexOf("source") != -1) {
				var previousSource = location.search.substr(location.search.indexOf("source"));
				if(previousSource.indexOf('&') != -1) {
					previousSource = previousSource.substr(0,previousSource.indexOf('&'));
				}
				
				location.search = location.search.replace(previousSource, "source=" + this.select.val());
			} else {
				window.location.href = window.location.href + "&source=" + this.select.val();
			}
			
		} else {
			this.manager.store.remove("source");
			this.manager.store.addByValue("source", this.select.val());
		}
	},

	afterRequest : function() {
	}
});
