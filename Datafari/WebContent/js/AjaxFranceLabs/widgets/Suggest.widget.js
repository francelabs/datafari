/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.SuggestWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#suggest_widget
 *
 */
AjaxFranceLabs.SuggestWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	boosts : {},

	resultWidget : null,

	type : 'suggest',

	//Methods

	init : function() {
		var manager = $.extend(true, {}, this.manager);
		manager.widgets = {};
		this.resultWidget = new AjaxFranceLabs.ResultWidget({
			elm : this.elm,
			id : 'suggestions'
		});
		this.resultWidget.manager = manager;
		this.resultWidget.init();
		manager.addWidget(this.resultWidget);
		this.buildWidget();
	},

	buildWidget : function() {
		$(this.elm).addClass('suggestWidget').addClass('widget');
	},

	afterRequest : function() {
		if (this.manager.store.get('q').value !== '*:*') {
			var parsedQ = this.manager.store.get('q').value.match(/\((.*?)\)|\[(.*?)\]|(\S*)/g), newQ = '', defaultSearchField = '';
			for (var search in parsedQ) {
				if (!AjaxFranceLabs.empty(AjaxFranceLabs.trim(parsedQ[search])) && parsedQ[search] !== 'AND' && parsedQ[search] !== 'OR') {
					var match = parsedQ[search].match(/\((.*?):.*?\)/);
					if (match === null) {
						if (!AjaxFranceLabs.empty(defaultSearchField))
							defaultSearchField += "+";
						defaultSearchField += parsedQ[search];
					} else {
						if (!AjaxFranceLabs.empty(newQ))
							newQ += ' OR ';
						newQ += parsedQ[search];
						var boost = 1;
						if (this.boosts[match[1]] !== undefined)
							boost = this.boosts[match[1]];
						newQ += '^' + boost
					}
				}
			}
			if (!AjaxFranceLabs.empty(defaultSearchField)) {
				if (this.manager.constellio)
					defaultSearchField = '(doc_defaultSearchField:' + defaultSearchField + ')';
				else
					defaultSearchField = '(defaultSearchField:' + defaultSearchField + ')';
				if (!AjaxFranceLabs.empty(newQ))
					newQ += ' OR ';
				newQ += defaultSearchField;
			}
			this.resultWidget.manager.params = this.manager.params;
			this.resultWidget.manager.store.remove('q');
			this.resultWidget.manager.store.addByValue('q', newQ);
			this.resultWidget.manager.executeRequest();
		}
	}
});
