/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.PagerModule
 * @extends	AjaxFranceLabs.AbstractModule
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#pager_module
 *
 */
AjaxFranceLabs.PagerModule = AjaxFranceLabs.AbstractModule.extend({

	//Variables

	source : null,

	prevLabel : '<',

	nextLabel : '>',

	firstLabel : '<<',

	lastLabel : '>>',

	nbElmToDisplay : 10,

	nbPage : 0,

	nbPageDisplayed : 4,

	nbElements : 0,

	fastNavigationAlwaysVisible : false,

	display : null,

	pageSelected : 0,

	type : 'pager',

	//Methods

	init : function() {
		if (!this.initialized) {
			this.initialized = true;
			var self = this, elm = $(this.elm);
			this.nbPageDisplayed = (this.nbPageDisplayed < 4) ? 4 : this.nbPageDisplayed;
			elm.append('<div>');
			elm.find('div:last').addClass('pagerModule').addClass('module').append('<span class="go_f fn fl">').append('<span class="go_p fn pn">').append('<div class="pages">').append('<span class="go_n fn pn">').append('<span class="go_l fn fl">');
			elm.find('.go_f').append('<span>' + this.firstLabel + '</span>');
			elm.find('.go_p').append('<span>' + this.prevLabel + '</span>');
			elm.find('.go_n').append('<span>' + this.nextLabel + '</span>');
			elm.find('.go_l').append('<span>' + this.lastLabel + '</span>');
			elm.find('.fn').click(function() {
				if ($(this).hasClass('go_f'))
					self.pageSelected = 0;
				else if ($(this).hasClass('go_l'))
					self.pageSelected = self.nbPage - 1;
				else if ($(this).hasClass('go_p'))
					self.pageSelected = (self.pageSelected === 0) ? 0 : self.pageSelected - 1;
				else
					self.pageSelected = (self.pageSelected === self.nbPage - 1) ? self.pageSelected : self.pageSelected + 1;
				self.clickHandler();
			});
			if (this.display === null)
				this.display = $(this.elm).find('.fn').css('display');
		}
	},

	updatePages : function() {
		
		$(this.elm).find('.pagerModule').css('display', 'block');
		var self = this, elm = $(this.elm);
		this.nbElements = (this.source !== null) ? this.source.children().length : this.nbElements;
		this.nbPage = Math.ceil(this.nbElements / this.nbElmToDisplay);
		if (this.nbPage == 1) {
			$(this.elm).find('.pagerModule').css('display', 'none');
			return false;
		}
		elm.find('.pages').empty();
		var firstPage = this.pageSelected - 10;
		if (firstPage < 0){
			firstPage = 0;
		}
		for (var p = firstPage; p < firstPage+20 && p < this.nbPage; p++) {
			$(this.elm).find('.pages').append('<span class="page" page="' + p + '"><span>' + (p + 1) + '</span></span>');
			if (p === this.pageSelected)
				$(this.elm).find('.page:last').addClass('selected');
		}
		
		
		elm.find('.page').click(function() {
			self.pageSelected = parseInt($(this).attr('page'));
			self.clickHandler();
		});
		elm.find('.page.selected').unbind('click');
		if (!this.fastNavigationAlwaysVisible) {
			if (this.pageSelected === 0)
				elm.find('.go_f, .go_p').css('display', 'none');
			else
				$(this.elm).find('.go_f, .go_p').css('display', this.display);
			if (this.pageSelected === this.nbPage - 1)
				elm.find('.go_n, .go_l').css('display', 'none');
			else
				elm.find('.go_n, .go_l').css('display', this.display);
		}
		elm.find('.page').css('display', 'none');
		elm.find('.page[page=' + this.pageSelected + ']').addClass('selected');
		elm.find('.page.selected').css('display', this.display).prev().css('display', this.display);
		for (var p = $(this.elm).find('.page:visible').length; p < this.nbPageDisplayed; p++)
			elm.find('.page:visible:last').next().css('display', this.display);
		if (elm.find('.page:visible').length < this.nbPageDisplayed)
			for (var p = $(this.elm).find('.page:visible').length; p < this.nbPageDisplayed; p++)
				elm.find('.page:visible:first').prev().css('display', this.display);
		var width = 10;
		for (var p = 0; p < this.nbPageDisplayed; p++)
			width += $(this.elm).find('.page:visible:eq(' + p + ')').outerWidth(true);
		elm.find('.pages').width(width);
		AjaxFranceLabs.clearMultiElementClasses(elm.find('.page'));
		AjaxFranceLabs.addMultiElementClasses(elm.find('.page:visible'));
	},

	clickHandler : function() {
		this.updateList();
		this.updatePages();
	},

	updateList : function() {
	}
});
