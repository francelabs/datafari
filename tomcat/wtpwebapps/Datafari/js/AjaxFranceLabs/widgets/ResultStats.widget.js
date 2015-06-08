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
AjaxFranceLabs.ResultStatsWidget = AjaxFranceLabs.AbstractWidget
		.extend({

			// Variables

			pagination : false,

			type : 'result',

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
					elm.find('table').append('<thead>');
					elm
							.find('thead')
							.append(
									'<tr><th width="123">query</th><th width="49">count</th><th width="46">frequency</th><th width="51">AVGHits</th><th width="57">numNoHits</th><th width="50">withClick</th><th width="75">withClickRatio</th><th width="95">AVGClickPosition</th><th width="63">AVGQTime</th><th width="78">MaxQTime</th></tr>');
					elm.find('table').append('<tbody>');
					$.each(data.response.docs, function(i, doc) {
						var statLine = '<td>' + '<a href="statsQuery.jsp?query='+doc.query+'">'
								+ doc.query + '</a>' + '</td>';
						statLine += '<td>' + doc.count + '</td>';
						statLine += '<td>' + doc.frequency + '</td>';
						statLine += '<td>' + doc.AVGHits + '</td>';
						statLine += '<td>' + doc.numNoHits + '</td>';
						statLine += '<td>' + doc.withClick + '</td>';
						statLine += '<td>' + doc.withClickRatio + '</td>';
						statLine += '<td>' + doc.AVGClickPosition + '</td>';
						statLine += '<td>' + doc.AVGQTime + '</td>';
						statLine += '<td>' + doc.MaxQTime + '</td>';
						elm.find('tbody').append('<tr>' + statLine + '</tr>');

					});
					AjaxFranceLabs.addMultiElementClasses(elm.find('.doc'));
					if (this.pagination)
						this.pagination.afterRequest(data);

					$('.resultGrid').flexigrid();
				}
			}
		});
