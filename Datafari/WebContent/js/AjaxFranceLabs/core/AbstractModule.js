/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.AbstractModule
 * @extends	AjaxFranceLabs.Class
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/documentation#AbstractModule
 *
 */
AjaxFranceLabs.AbstractModule = AjaxFranceLabs.Class.extend({

	//Variables

	id : null,

	manager : null,

	initialized : false,

	type : null,

	//Methods

	init : function() {
	},

	beforeRequest : function() {
	},

	afterRequest : function() {
	}
});
