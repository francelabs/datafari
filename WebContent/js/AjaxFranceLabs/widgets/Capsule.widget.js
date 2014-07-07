/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.CapsuleWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#capsule_widget
 *
 */
AjaxFranceLabs.CapsuleWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'capsule',

	//Methods

	init : function() {
		if (!this.initialized) {
			this.initialized = true;
			if (this.manager.connectionInfo.capsule === undefined)
				throw 'CapsuleWidget: connectionInfo not defined in Manager';
			else
				this.connectionInfo = this.manager.connectionInfo.capsule;
			this.buildWidget();
		}
	},

	buildWidget : function() {
		$(this.elm).addClass('capsuleWidget').addClass('widget');
	},

	beforeRequest : function() {
		$(this.elm).empty().hide();
	},

	afterRequest : function() {
		var self = this;
		this.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, (this.manager.constellio ? 'collectionName='+this.manager.collection+'&' : '')+this.connectionInfo.queryString + this.manager.store.get('q').val(), function(data) {
			if (data)
				$(self.elm).append('<span class="title">' + data.title + '</span>').append('<span class="tips">' + data.description + '</span>').show();
		});
	}
});
