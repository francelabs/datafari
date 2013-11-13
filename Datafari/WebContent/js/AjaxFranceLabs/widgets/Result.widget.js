/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.ResultWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#result_widget
 *
 */
AjaxFranceLabs.ResultWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	pagination : false,

	type : 'result',

	//Methods

	buildWidget : function() {
		var elm = $(this.elm);
		elm.addClass('resultWidget').addClass('widget').attr('widgetId', this.id).append('<div class="doc_list">');
		if (this.pagination)
			$(this.elm).append('<div class="doc_list_pagination">');
		if (this.pagination === true) {
			var self = this;
			this.pagination = new AjaxFranceLabs.PagerModule({
				elm : elm.find('.doc_list_pagination'),
				nbPageDisplayed : 10,
				afterRequest : function(data) {
					this.nbElements = parseInt(data.response.numFound);
					this.pageSelected = data.response.start / this.nbElmToDisplay;
					this.updatePages();
					if (this.nbPage > 1) {
						$(self.elm).find('.doc_list_pagination').css('visibility', 'visible');
					}
				},
				clickHandler : function() {
					self.manager.store.get('start').val(this.pageSelected * this.nbElmToDisplay);
					self.manager.makeRequest();
				}
			});
		}
		if (this.pagination)
			this.pagination.init();
	},

	beforeRequest : function() {
		var elm = $(this.elm);
		elm.find('.doc_list_pagination').css('visibility', 'hidden');
		elm.find('.doc_list').empty().append('<div class="bar-loader" />');
		if (this.pagination)
			this.pagination.beforeRequest();
	},

	afterRequest : function() {
		//This is an example, this method must be overide using your own results
		var data = this.manager.response, elm = $(this.elm);
		elm.find('.doc_list').empty();
		if (data.response.numFound === 0) {
			elm.find('.doc_list').append('<span class="noResult">No document found.</span>');
		} else {
			var self = this;
			$.each(data.response.docs, function(i, doc) {
				//Append your document
			});
			AjaxFranceLabs.addMultiElementClasses(elm.find('.doc'));
			if (this.pagination)
				this.pagination.afterRequest(data);
		}
	}
});
