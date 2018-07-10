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
	widgetContainerNum : 0,

	//Methods

	

	buildWidget : function() {
		$(this.elm).addClass('promolinkWidget').addClass('widget');
	},

	beforeRequest : function() {
		var self = this;
		if (self.manager.store.isParamDefined('original_query') !== true) {
			$(this.elm).empty().hide();
			self.widgetContainerNum = 0;
		}
		
	},

	afterRequest : function() {
		var self = this;
		var data = this.manager.response, elm = $(this.elm);
		if(data.promolinkSearchComponent!==undefined){	
			if (data.promolinkSearchComponent.title !== undefined && data.promolinkSearchComponent.title.length > 0){
				var title;
				if (data.promolinkSearchComponent["title_"+window.i18n.language] !== undefined){
					title = data.promolinkSearchComponent["title_"+window.i18n.language];
				} else {
					title = data.promolinkSearchComponent.title;
				}
				var content = '';
				if (data.promolinkSearchComponent["content_"+window.i18n.language] !== undefined){
					content = data.promolinkSearchComponent["content_"+window.i18n.language];
				} else {
					if (data.promolinkSearchComponent.content !== undefined){
						content = data.promolinkSearchComponent.content;
					}
				}
				if (self.manager.store.isParamDefined('original_query') === true) {
					$(self.elm).append("<span class='promolink-separator'></span>");
				}
				$(self.elm).append('<div class="widgetContainer" id="widgetContainer' + self.widgetContainerNum + '"></div>').show();
				$(self.elm).find('#widgetContainer' + self.widgetContainerNum).append('<span class="title">' + '<span class="annonce">Annonce</span> '+ title + '</span></br>').append('<span class="tips" id="snippet">' + content + '</span>').show();
				self.widgetContainerNum++;
			}
		}
	}
});
