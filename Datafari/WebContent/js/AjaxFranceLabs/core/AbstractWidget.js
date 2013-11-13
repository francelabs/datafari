/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.AbstractWidget
 * @extends	AjaxFranceLabs.Class
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/documentation#AbstractWidget
 *
 */
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
	}
});
