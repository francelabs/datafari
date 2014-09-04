/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.AdvancedSearchTable
 * @extends	AjaxFranceLabs.Class
 *
 */
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
