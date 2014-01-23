/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.AbstractManager
 * @extends	AjaxFranceLabs.Class
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/documentation#AbstractManager
 *
 */
AjaxFranceLabs.AbstractManager = AjaxFranceLabs.Class.extend({

	//Variables

	constellio : true,

	serverUrl : 'http://localhost:8080/constellio/app/',

	servlet : 'select',

	response : {},

	widgets : {},

	modules : {},

	store : new AjaxFranceLabs.ParameterStore({}),

	initialized : false,

	collections : [],

	collection : null,

	connectionInfo : {},

	//Methods

	init : function() {
		if (this.initialized) {
			return $.Deferred().resolve;
		}
		if (this.constellio && this.connectionInfo.collection === undefined)
			throw 'AbstractManager: connectionInfo.collection not defined';
		var self = this;
		this.initialized = true;
		this.store.addByValue('q', '*:*');
		this.store.addByValue('rows', 10);
		if (this.constellio) {
			return $.when(this.executeRequest(this.connectionInfo.collection.serverUrl, this.connectionInfo.collection.servlet, this.connectionInfo.collection.queryString, function(data) {
				self.collection = data.defaultCollection;
				self.collections = data.collections;
			})).done(function() {
				self.store.addByValue('collectionName', self.collection);
			}).always(function() {
				for (var widget in self.widgets) {
					self.widgets[widget].init();
				}
				for (var module in self.modules) {
					self.modules[module].init();
				}
			});
		} else {
			for (var widget in self.widgets) {
				self.widgets[widget].init();
			}
			for (var module in self.modules) {
				self.modules[module].init();
			}
			return $.Deferred().resolve;
		}
	},

	addModule : function(module) {
		module.manager = this;
		this.modules[module.id] = module;
	},

	addWidget : function(widget) {
		widget.manager = this;
		this.widgets[widget.id] = widget;
	},

	handleResponse : function(data) {
		this.response = data;
		for (var widget in this.widgets) {
			this.widgets[widget].afterRequest();
		}
		for (var module in this.modules) {
			this.modules[module].afterRequest();
		}
	},

	makeRequest : function(servlet) {
		var self = this;
		$.when(this.init()).done(function() {
			for (var widget in self.widgets) {
				self.widgets[widget].beforeRequest();
			}
			for (var module in self.modules) {
				self.modules[module].beforeRequest();
			}
			self.executeRequest('', servlet);
		});
	},
	

	generateAndSetQueryID: function(){
		this.store.addByValue("id", UUID.generate());
	},

	executeRequest : function() {
		throw 'AbstractManager.executeRequest must be implemented.';
	}
});
