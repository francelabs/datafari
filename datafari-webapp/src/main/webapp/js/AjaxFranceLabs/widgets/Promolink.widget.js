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
AjaxFranceLabs.PromolinkWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	type : 'promolink',
	// Number of promolinks for the current query
	widgetContainerNum : 0,

	//Methods

	

	buildWidget : function() {
		$(this.elm).addClass('promolinkWidget').addClass('widget');
	},

	beforeRequest : function() {
		var self = this;
		// Do not hide the widget and re-init the widgetContainerNum param in case self.manager.store.isParamDefined('original_query') exists 
		// because it means that the query has been automatically corrected (certainly by the spellchecker) so we should not erase promolinks that may have been found for the original query
		if (self.manager.store.isParamDefined('original_query') !== true) {
			$(this.elm).empty().hide();
			self.widgetContainerNum = 0;
		}
		
	},

	afterRequest : function() {
		var self = this;
		var data = this.manager.response, elm = $(this.elm);
		if(data.promolinkSearchComponent!==undefined){	
			for(var i=0; i < data.promolinkSearchComponent.length; i++) {
				var promolink = data.promolinkSearchComponent[i];
				if (promolink !== undefined && promolink.title.length > 0) {
					var title;
					if (promolink["title_"+window.i18n.language] !== undefined){
						title = promolink["title_"+window.i18n.language];
					} else {
						title = promolink.title;
					}
					var content = '';
					if (promolink["content_"+window.i18n.language] !== undefined){
						content = promolink["content_"+window.i18n.language];
					} else {
						if (promolink.content !== undefined){
							content = promolink.content;
						}
					}
					if (self.widgetContainerNum > 0) { // If there already is/are some promolink(s) then add a separator
						$(self.elm).append("<span class='promolink-separator'></span>");
					}
					$(self.elm).append('<div class="widgetContainer" id="widgetContainer' + self.widgetContainerNum + '"></div>').show();
					$(self.elm).find('#widgetContainer' + self.widgetContainerNum).append('<span class="title">' + '<span class="annonce">Annonce</span> '+ title + '</span></br>').append('<span class="tips" id="snippet">' + content + '</span>').show();
					// Increment the number of generated promolinks for the current query
					self.widgetContainerNum++;
				}
			}
		}
	}
});
