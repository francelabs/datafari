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

	//Methods

	

	buildWidget : function() {
		$(this.elm).addClass('promolinkWidget').addClass('widget');
	},

	beforeRequest : function() {
		$(this.elm).empty().hide();
	},

	afterRequest : function() {
		var self = this;
		var data = this.manager.response, elm = $(this.elm);
		if(data.promolinkSearchComponent!==undefined){	
			if (data.promolinkSearchComponent.title !== undefined && data.promolinkSearchComponent.title.length > 0){
				$(self.elm).append('<div class="widgetContainer"></div>').show();
				$(self.elm).find('.widgetContainer').append('<span class="title">' + '<span class="annonce">Annonce</span> '+ data.promolinkSearchComponent.title + '</span></br>').append('<span class="tips" id="snippet">' + data.promolinkSearchComponent.content + '</span>').show();
			}
		}
	}
});
