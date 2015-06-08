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
