/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.CollectionWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#collection_widget
 *
 */
AjaxFranceLabs.CollectionWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'collection',

	//Methods

	buildWidget : function() {
		var self = this, elm = $(this.elm);
		elm.addClass('collectionWidget').addClass('widget').attr('widgetId', this.id).append('<div class="title">').append('<ul class="content">').find('.title').append('<span>Collection</span>');
		$(this.manager.collections).each(function() {
			var collectionName = this.toString();
			elm.find('.content').append('<li><span>' + collectionName + '</span></li>');
			if (collectionName == self.manager.collection) {
				elm.find('.content li').addClass('selected');
				self.manager.store.addByValue('collectionName', collectionName);
			}
			elm.find('.content li:last').click(function() {
				self.manager.store.remove('collectionName');
				self.manager.store.addByValue('collectionName', collectionName);
				if (!$(this).hasClass('selected'))
					self.manager.makeRequest();
				elm.find('.content li').removeClass('selected');
				$(this).addClass('selected');
			});
		});
		AjaxFranceLabs.addMultiElementClasses(elm.find('.content li'));
	}
});
