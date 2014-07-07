/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.AdvancedSearchField
 * @extends	AjaxFranceLabs.Class
 *
 */
AjaxFranceLabs.AdvancedSearchField = AjaxFranceLabs.Class.extend({

	//Variables

	parent : null,

	elm : null,

	label : '',

	range : false,

	description : '',

	multiInput : false,

	addInputLabel : "Add input",

	autocomplete : false,

	autocompleteOptions : {
		optionsExtend : true,
		render : null,
		field : null,
		valueSelectFormat : function(value) {
			return value;
		},
		singleValue : false,
		openOnFieldFocus : false
	},

	manager : null,

	filter : false,

	field : null,

	//Methodes

	init : function() {
		$(this.parent).append('<span class="field">');
		this.elm = $(this.parent).find('.field:last');
		var elm = $(this.elm);
		elm.append('<span class="left">').append('<span class="right"></span>');
		elm.find('.left').append('<label>' + this.label + '</label>').append('<div class="inputs">');
		elm.find('.right').append('<span>' + this.description + '</span>');
		if (this.range === false)
			elm.find('.inputs').append('<span>').find('span:last').append('<input type="text" />');
		else
			$(this.elm).find('.inputs').append('<span>').find('span:last').append('<input type="text" />').append('<span class="to">to</span>').append('<input type="text" />').parents('.field').addClass('range');
		if (this.autocomplete && !this.range) {
			this.autocompleteOptions.elm = elm.find('.inputs input');
			var autocomplete = new AjaxFranceLabs.AutocompleteModule(this.autocompleteOptions);
			autocomplete.manager = this.manager;
			autocomplete.init();
		}

		if (this.multiInput) {
			var self = this;
			elm.find('.left').append('<span class="addInput">' + this.addInputLabel + '</span>').find('.addInput').click(function() {
				if (self.range === false) {
					elm.find('.inputs').append('<span>').find('span:last').append('<input type="text" />').append('<span class="removeInput">x</span>');
					if (self.autocomplete && !self.range) {
						self.autocompleteOptions.elm = $(self.elm).find('.inputs input');
						var autocomplete = new AjaxFranceLabs.AutocompleteModule(self.autocompleteOptions);
						autocomplete.manager = self.manager;
						autocomplete.init();
					}
				} else {
					elm.find('.inputs').append('<span>').find('span:last').append('<input type="text" />').append('<span class="to">to</span>').append('<input type="text" />').parents('.field').addClass('range').append('<span class="removeInput">x</span>');
				}
				elm.find('.inputs > span:last .removeInput').click(function() {
					$(this).parent().remove();
				});
				elm.find('.inputs > span:last input:first').focus();
			});
		}
	},

	getValue : function() {
		var prefix = (this.field !== null) ? this.field : '', res = '', self = this, elm = $(this.elm);
		if (!this.range) {
			elm.find('.inputs > span').not('.removeInput').each(function(index) {
				if (!AjaxFranceLabs.empty(AjaxFranceLabs.trim($(this).find('input').val().replace(/\u200c/g, '')))) {
					if (!AjaxFranceLabs.empty(res))
						res += ',';
					res += AjaxFranceLabs.trim($(this).find('input').val().replace(/\u200c/g, ''));
				}
			});
		} else {
			elm.find('.inputs > span').not('.removeInput').each(function(index) {
				var val_1 = AjaxFranceLabs.trim($(this).find('input:eq(0)').val().replace(/\u200c/g, ''));
				val_2 = AjaxFranceLabs.trim($(this).find('input:eq(1)').val().replace(/\u200c/g, ''));
				val_1 = (AjaxFranceLabs.empty(val_1)) ? '*' : val_1;
				val_2 = (AjaxFranceLabs.empty(val_2)) ? '*' : val_2;
				if (val_1 !== '*' || val_2 !== '*') {
					if (!AjaxFranceLabs.empty(res))
						res += ',';
					res += '[' + val_1 + ' TO ' + val_2 + ']';
				}
			});
		}
		if (!AjaxFranceLabs.empty(res))
			res = (!AjaxFranceLabs.empty(prefix) ? prefix + ':' : '') + res;
		return res;
	}
});
