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
AjaxFranceLabs.FavoriteModule = AjaxFranceLabs.AbstractModule.extend({

	//Variables

	userId : null,

	type : 'favorite',

	//Methods

	init : function() {
		if (this.manager.connectionInfo.favorite === undefined)
			throw 'FavoriteModule: connectionInfo not defined in Manager';
		else
			this.connectionInfo = this.manager.connectionInfo.favorite;
		var self = this;
		if (!this.initialized) {
			this.initialized = true;
			$('.searchBar').after('<span class="saveSearch">Save this search</span>');
			$('.saveSearch').click(function() {
				var elm = this;
				if ($(this).hasClass('selected')) {
					self.manager.executeRequest(self.connectionInfo.serverUrl, self.connectionInfo.servlet, self.connectionInfo.queryString.removeFavoriteSearch + $(elm).attr('favid') + ((self.userID !== null) ? '&userID=' + self.userId : ''), function(data) {
						if (data.response == 'ok')
							$(elm).removeClass('selected').empty().append('Save this search').attr('favid', '');
					});
				} else {
					var elm = this;
					self.manager.executeRequest(self.connectionInfo.serverUrl, self.connectionInfo.servlet, self.connectionInfo.queryString.addFavoriteSearch + self.manager.store.get('q').val() + (self.manager.constellio ? '&collectionName='+self.manager.collection : '') + ((self.userID !== null) ? '&userID=' + self.userId : ''), function(data) {
						if (data.id !== undefined)
							$(elm).addClass('selected').empty().append('Remove this search').attr('favid', data.id);
					});
				}
			});
			if (this.connectionInfo.queryString.getUserId !== undefined) {
				this.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.getUserId, function(response) {
					self.userId = response.id;
				});
			}
		}
	},

	beforeRequest : function() {
		if (this.userId !== null)
			this.manager.store.addByValue('userID', this.userId);
	},

	afterRequest : function() {
		var self = this, resList = $('.resultWidget');
		resList.find('.doc .title').append('<span class="favorite_star">');
		resList.find('.doc .title .favorite_star').click(function() {
			var elm = this;
			if ($(this).hasClass('selected')) {
				self.manager.executeRequest(self.connectionInfo.serverUrl, self.connectionInfo.servlet, self.connectionInfo.queryString.removeFavoriteDocument + $(this).parents('.doc').attr('favid') + ((self.userID !== null) ? '&userID=' + self.userId : ''), function(data) {
					if (data.response == 'ok')
						$(elm).removeClass('selected').parents('.doc').attr('favid', '');
				});
			} else {
				var elm = this;
				self.manager.executeRequest(self.connectionInfo.serverUrl, self.connectionInfo.servlet, self.connectionInfo.queryString.addFavoriteDocument + $(this).parents('.doc').attr('doc_uniquekey') + (self.manager.constellio ? '&collectionName='+self.manager.collection : '') + ((self.userID !== null) ? '&userID=' + self.userId : ''), function(data) {
					if (data.id !== undefined)
						$(elm).addClass('selected').parents('.doc').attr('favid', data.id);
				});
			}
			return false;
		});
		self.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.isFavoriteSearch + self.manager.store.get('q').val() + (self.manager.constellio ? '&collectionName='+self.manager.collection : '') + ((self.userID !== null) ? '&userID=' + self.userId : ''), function(data) {
			if (data.isFavorite == false)
				$('.saveSearch').removeClass('selected').empty().append('Save this search').attr('favid', '');
			else
				$('.saveSearch').addClass('selected').empty().append('Remove this search').attr('favid', data.favoriteSearchID);
		});
		var documents = new Array();
		$(self.manager.response.response.docs).each(function(i, elm) {
			documents.push(elm.doc_uniqueKey);
		});
		documents = {
			docs_id : documents
		};
		if (documents.docs_id.length > 0) {
			self.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.isFavoriteDocument + $.param(documents) + (self.manager.constellio ? '&collectionName='+self.manager.collection : '') + ((self.userID !== null) ? '&userID=' + self.userId : ''), function(data) {
				$.each(data.documents, function(i, elm) {
					if (elm.isFavorite == true)
						resList.find('.doc[doc_uniquekey="' + elm.uniqueKey + '"]').attr('favid', elm.favoriteDocumentID).find('.favorite_star').addClass('selected');
				});
			});
		}
	},

	removeFavoriteDocument : function(id, callback) {
		this.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.removeFavoriteDocument + id + ((this.userID !== null) ? '&userID=' + this.userId : ''), callback);
	},

	removeFavoriteSearch : function(id, callback) {
		this.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.removeFavoriteSearch + id + ((this.userID !== null) ? '&userID=' + this.userId : ''), callback);
	},

	removeSearchInHistory : function(id, callback) {
		this.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.removeHistory + id + ((this.userID !== null) ? '&userID=' + this.userId : ''), callback);
	},

	getFavoriteDocuments : function(callback, first, count) {
		this.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.getFavoriteDocument + '&first=' + first + '&count=' + count + ((this.userID !== null) ? '&userID=' + this.userId : ''), callback);
	},

	getFavoriteSearches : function(callback, first, count) {
		this.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.getFavoriteSearch + '&first=' + first + '&count=' + count + ((this.userID !== null) ? '&userID=' + this.userId : ''), callback);
	},

	getSearchHistory : function(callback, first, count) {
		if (arguments.length < 3) {
			count = 10;
			if (arguments.length < 2)
				first = 0;
		}
		this.manager.executeRequest(this.connectionInfo.serverUrl, this.connectionInfo.servlet, this.connectionInfo.queryString.getHistory + '&first=' + first + '&count=' + count + ((this.userID !== null) ? '&userID=' + this.userId : ''), callback);
	}
});
