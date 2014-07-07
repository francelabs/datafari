/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.manager
 * @extends	AjaxFranceLabs.AbstractManager
 *
 * Implements the AjaxFranceLabs.AbstractManager
 *
 */
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
