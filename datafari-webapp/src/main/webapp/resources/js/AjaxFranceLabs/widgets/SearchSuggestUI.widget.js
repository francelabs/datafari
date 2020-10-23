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
AjaxFranceLabs.SearchSuggestUIWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables
	elm : null,
	id : null,
	 
	
	//Methods
	buildWidget : function() {		
	},
	
	beforeRequest : function() {
		//this.makeRequest("select", "q=template");
	},
	
	afterRequest : function() {
		
	},
	
	makeRequest : function(requestHandler, requestParams) {
		var self = this;
		var handler = function(data) {
			self.handleResponse(data);
		}
		this.manager.executeRequest('', requestHandler, requestParams, handler);
	},
	
	handleResponse : function(data) {
		var showSuggestUI = false;
		if(showSuggestUI) {
			$("#solr .col.right").addClass("reduced");
			this.elm.show();
			this.elm.html("response received");
		} else {
			$("#solr .col.right").removeClass("reduced");
			this.elm.hide();
		}
	}
});
