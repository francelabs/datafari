/*******************************************************************************
 * Copyright 2015 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
AjaxFranceLabs.HierarchicalPagerModule = AjaxFranceLabs.PagerModule.extend({

	//Variables

	//Methods

	updatePages : function() {

		var self = this, elm = $(this.elm);
		this.nbPage = 1;
		if(this.source != null) {
			var nbEl = 0;
			var pages = 1;
			var nbByPage = this.nbElmToDisplay;
			this.source.children('li').each(function() { // Calculates the number of pages needed
				if(nbEl > 0) { // Check if it's the first element of the first page
					nbEl += $(this).find('ul').children().length + 1; // Count the sub elements and add 1 to also count the parent
					if(nbEl > nbByPage) { // If the number of elements is higher than the allowed number by page, start a new page that will contain the current element and his children 
						pages++;
						nbEl = $(this).find('ul').children().length + 1; // Count the sub elements and add 1 to also count the parent
					}
				}else { // It is the first element of the first page so no matter how many children it contains, no needs to directly increment the number of pages as it can be the only element to display
					nbEl += $(this).find('ul').children().length + 1; // Count the children elements and add 1 to also count the parent
				}
			});
			this.nbPage = pages;
		}
		if (this.nbPage <= 1) {
			$(this.elm).find('.pagerModule').css('display', 'none');
			return false;
		}
		$(this.elm).find('.pagerModule').css('display', 'block').addClass('show');
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
	}
});
