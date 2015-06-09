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
AjaxFranceLabs.Manager = AjaxFranceLabs.AbstractManager.extend({

	executeRequest : function(server, servlet, string, handler) {
		var self = this;
		server = server || this.serverUrl;
		servlet = servlet || this.servlet;
		string = string || this.store.string();
		handler = handler ||
		function(data) {
			self.handleResponse(data);
		};
		if (this.proxyUrl) {
			return $.post(this.proxyUrl, {
				query : string
			}, handler, 'json');
		} else {
			return $.getJSON(server + servlet + '?' + string + '&wt=json&json.wrf=?', handler);		}
	}
	
});
