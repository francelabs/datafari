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
AjaxFranceLabs.ResultStatsQueryWidget = AjaxFranceLabs.AbstractWidget
		.extend({

			// Variables

			pagination : false,

			type : 'result',
			
			mode : 'query',

			// Methods

			buildWidget : function() {
				var elm = $(this.elm);
				elm.addClass('resultWidget').addClass('widget').attr(
						'widgetId', this.id).append('<div class="doc_list">');
				if (this.pagination)
					$(this.elm).append('<div class="doc_list_pagination">');
				if (this.pagination === true) {
					var self = this;
					this.pagination = new AjaxFranceLabs.PagerModule({
						elm : elm.find('.doc_list_pagination'),
						nbPageDisplayed : 10,
						afterRequest : function(data) {
							this.nbElements = parseInt(data.response.numFound);
							this.pageSelected = data.response.start
									/ this.nbElmToDisplay;
							this.updatePages();
							if (this.nbPage > 1) {
								$(self.elm).find('.doc_list_pagination').css(
										'visibility', 'visible');
							}
						},
						clickHandler : function() {
							self.manager.store.get('start').val(
									this.pageSelected * this.nbElmToDisplay);
							self.manager.makeRequest();
						}
					});
				}
				if (this.pagination)
					this.pagination.init();
			},

			beforeRequest : function() {
				var elm = $(this.elm);

				elm.find('.doc_list_pagination').css('visibility', 'hidden');
				elm.find('.doc_list').empty().append(
						'<div class="bar-loader" />');
				if (this.pagination)
					this.pagination.beforeRequest();
			},
			

			afterRequest : function() {
				var data = this.manager.response, elm = $(this.elm);
				elm.find('.doc_list').empty();
				if (data.response.numFound === 0) {
				} else {
					var self = this;
					elm.find('.doc_list').append('<table class="resultGrid">');
					
					if (this.mode === 'query'){
					elm.find('table').append('<thead>');
					elm
							.find('thead')
							.append('<tr><th width="167">query</th><th width="166">date</th><th width="100">numFound</th><th width="100">QTime</th><th width="100">numClicks</th><th width="100">AVGClickPosition</th></tr>');
									
									
					elm.find('table').append('<tbody>');
					$.each(data.response.docs, function(i, doc) {
						var statLine = '<td>' + '<a href="statsQuery.jsp?query='+ doc.q +'&id='+doc.id+'">'
						+ doc.q + '</a>' + '</td>';
						statLine += '<td>' + (new Date(doc.date)).toLocaleString() + '</td>';
						statLine += '<td>' + doc.numFound + '</td>';
						statLine += '<td>' + doc.QTime + '</td>';
						statLine += '<td>' + doc.numClicks + '</td>';
						var AVGClickPosition;
						if (doc.numClicks === 0){
							AVGClickPosition = '-';
						} else {
							AVGClickPosition = doc.positionClickTot/doc.numClicks;
						}
						
						statLine += '<td>' + AVGClickPosition + '</td>';
						elm.find('tbody').append('<tr>' + statLine + '</tr>');

					});
					}
					

					if (this.mode === 'queryID'){
					elm.find('table').append('<thead>');
					elm
							.find('thead')
							.append('<tr><th width="103">query</th><th width="190">filter</th><th width="55">numFound</th><th width="36">QTime</th><th width="28">page</th><th width="244">url</th><th width="65">clickPosition</th></tr>');
									
									
					elm.find('table').append('<tbody>');
					$.each(data.response.docs[0].history, function(i, history) {
						var historySplitted = history.split('///');
						var statLine = '';
						$.each(historySplitted, function(i, value){
							statLine += '<td>' + value + '</td>';
						});

						elm.find('tbody').append('<tr>' + statLine + '</tr>');
					});
					}
					
					
					
					
					AjaxFranceLabs.addMultiElementClasses(elm.find('.doc'));
					if (this.pagination)
						this.pagination.afterRequest(data);

					$('.resultGrid').flexigrid();
				}
			}
		});
