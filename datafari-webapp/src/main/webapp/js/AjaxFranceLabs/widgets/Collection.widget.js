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
