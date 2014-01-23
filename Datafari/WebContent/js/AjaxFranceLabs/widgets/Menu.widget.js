/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.MenuWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#menu_widget
 *
 */
AjaxFranceLabs.MenuWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	name : 'User space',

	menuItems : {},

	type : 'userMenu',

	//Methods

	buildWidget : function() {
		var self = this, elm = $(this.elm);
		elm.addClass('menuWidget').addClass('widget').append('<span class="menuLink">' + this.name + '<span class="arrow"></span></span>').append('<ul class="menu" style="display:none;"></ul>');
		$(this.menuItems).each(function(index, elm) {
			elm.find('.menu').append('<li><span>' + elm.name + '</span></li>').find('li:last').click(function() {
				window.location = elm.link;
			});
		});
		elm.find('.menuLink').hover(function() {
			$(self.elm).find('.menu').css('display', 'block');
		})
		elm.mouseleave(function() {
			elm.find('.menu').css('display', 'none');
		});
	}
});
