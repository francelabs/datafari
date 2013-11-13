/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.SpellcheckerWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#spellcheck_widget
 *
 */
AjaxFranceLabs.SpellcheckerWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'spellchecker',

	//Methods
	

	buildWidget : function() {
		$(this.elm).addClass('spellcheckerWidget').addClass('widget');
	},

	beforeRequest : function() {
		$(this.elm).empty();
	},

	afterRequest : function() {
		var self = this, res = '';
		var data = this.manager.response, elm = $(this.elm);
			if (data.spellcheck !== undefined && data.spellcheck.suggestions.length > 0) {
				$(data.spellcheck.suggestions).each(function(i, elm) {
					if (elm === 'collation'){
						res = data.spellcheck.suggestions[i+1];
					}
				});
				$(self.elm).append('<span>').find('span').append(window.i18n.msgStore['tryWith'] + '<span class="result">' + res + '</span> ?').find('.result').click(function() {
					self.manager.store.get('q').val(res);
					for (var w in self.manager.widgets) {
						if (self.manager.widgets[w].type === 'searchBar') {
							$(self.manager.widgets[w].elm).find('input[type=text]').val(res);
						}
					}
					self.manager.makeRequest();
				});
			}
		
	}
});