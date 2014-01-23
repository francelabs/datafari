/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.SliderWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#slider_widget
 *
 */
AjaxFranceLabs.SliderWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'slider',

	//Methods

	buildWidget : function() {
		$(this.elm).addClass('sliderWidget').addClass('widget').addClass('widget').attr('widgetId', this.id).slider(this);
	}
});
