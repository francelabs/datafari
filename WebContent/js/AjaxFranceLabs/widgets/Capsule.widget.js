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

	

	buildWidget : function() {
		$(this.elm).addClass('capsuleWidget').addClass('widget');
	},

	beforeRequest : function() {
		$(this.elm).empty().hide();
	},

	afterRequest : function() {
		var self = this;
		var data = this.manager.response, elm = $(this.elm);
			if (data.capsuleSearchComponent !== undefined && data.capsuleSearchComponent.title.length > 0)
				$(self.elm).append('<span class="title">' + data.capsuleSearchComponent.title + '</span>').append('<span class="tips">' + data.capsuleSearchComponent.body + '</span>').show();
		
	}
});
