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

 AjaxFranceLabs.ResultIllustratedWidget = AjaxFranceLabs.ResultWidget.extend({
						afterRequest : function() {
							var data = this.manager.response, elm = $(this.elm),self=this;
							if (!this.isMobile)
								elm.find('.doc_list').empty();
							else
								elm.find('.doc_list .bar-loader').remove();
							if (data.response.numFound === 0) {
								elm
										.find('.doc_list')
										.append(
												'<div class="doc no"><div class="res no"><span class="title noResult">'+window.i18n.msgStore['noResult']+'</span></div></div>');
							} else {
								var self = this;
								$.each(data.response.docs,
												function(i, doc) {
													if (doc.url != undefined){
										                var url = doc.url.replace("localhost",window.location.hostname); 
										                var positionString = Manager.store.get("start").value;
										                var position = 1;
										                
										                if (positionString !== null){
										                	position += parseInt(positionString);
										                }
										               
										                position += i;
														var description = '';
														if (data.highlighting[doc.id]) {
															$.each( data.highlighting[doc.id], function( key, value ) {
																description += value;
																});
														}
														elm.find('.doc_list').append(
																		'<div class="doc e-'+ i + '" id="'+ doc.id+ '"></div>');
														elm.find('.doc:last').append(
																		'<img src="images/feuille.jpg"/><div class="res"></div>');
														//for putting an image to illustrate the document
														//elm.find('.doc:last').append('<img src="images/feuille.jpg"/><div class="res"></div>');
	
	
														elm.find('.doc:last .res').append('<span ="icon"></span>');
														var extension;
														if (typeof doc.extension === "undefined"){
															extension = doc.source;
														} else {
															extension = doc.extension;
														}
														if (self.isMobile){
															if (extension.toLowerCase()!==undefined && extension.toLowerCase()!="")
																elm.find('.doc:last .icon').append('<span>['+ extension.toUpperCase() +']</span> ');
														}	
														else
															elm.find('.doc:last .icon').append('<object data="images/icons/'+ extension.toLowerCase() +'-icon-24x24.png"><img src="images/icons/default-icon-24x24.png" /></object>&nbsp;');
	
										                var urlRedirect = 'URL?url='+ url + '&id='+Manager.store.get("id").value + '&q=' + Manager.store.get("q").value + '&position='+position;
														elm.find('.doc:last .res').append('<a class="title" target="_blank" href="'+urlRedirect+'"></a>');
														elm.find('.doc:last .title').append('<span>' +decodeURIComponent(doc.title) + '</span>');
														elm.find('.doc:last .res').append('<p class="description"></p>');
														elm.find('.doc:last .description').append('<div id="snippet">'+ description+ '</div>');
														elm.find('.doc:last .description').append('<div id="urlMobile"><p class="address"></p></div>');
														elm.find('.doc:last .address').append('<a href="'+urlRedirect+'" title="'+decodeURIComponent(url)+'">' + AjaxFranceLabs.tinyUrl(decodeURIComponent(url)) + '</a>');
													}
												});
								
								AjaxFranceLabs.addMultiElementClasses(elm
										.find('.doc'));
								if (this.pagination)
									this.pagination.afterRequest(data);
							}
							if (this.isMobile){
								if ( $(".doc_list").children().length<parseInt($("#number_results_mobile span").text(),10)){
									if (data.response.docs.length!=0){	
										$("#results .doc_list_pagination").show();
										if (this.firstTimeWaypoint){
											this.firstTimeWaypoint = false;
											var waypoin = $(".doc_list_pagination").waypoint(function(e){
												this.destroy();
												self.firstTimeWaypoint = true;
												self.mutex_locked=true;
												self.pagination.pageSelected ++;
												self.nextPage();
												self.mutex_locked=false;
												Waypoint.refreshAll();
											},{ offset: 'bottom-in-view'});
											while(self.mutex_locked)
												sleep(1);
											//home made mutex  used to stop the browser from executing multiple times the lines above 
											// without waiting the precedent execution (no native mutex are enable in javascript)
										}
									}
								}else{
									$("#results .doc_list_pagination").hide();
									$("#spinner_mobile").hide();
								}
							}
						}
					});	