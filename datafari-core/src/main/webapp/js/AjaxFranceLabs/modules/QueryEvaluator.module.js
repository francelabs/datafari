/*******************************************************************************
 * Copyright 2015 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
AjaxFranceLabs.QueryEvaluatorModule = AjaxFranceLabs.AbstractModule
		.extend({

			// Variables
			core : "FileShare",
			parentWidget : "",
			isMobile : $(window).width() < 800,

			setParentWidget : function(widget) {
				this.parentWidget = widget;
			},

			// Methods

			addEvaluatorLinks : function(resultElm, id, query) {
				// var style = "";
				// if (window.isLikesAndFavoritesEnabled) {
				// style = "style='margin-right: 2em;'"
				// }

				if (!this.isMobile) {
					var self = this;

					//			
					// // Send the POST request to elevate the selected doc
					// $.post("./SearchExpert/queryEvaluator", {item:
					// $(this).attr('id'), query:
					// self.manager.store.params.q.value, action:
					// $(this).attr('action')},function(data){
					//
					// })

					// Add the up link and set his onClick function
					resultElm.find(".title").after(
							"<span class='evaluator-up-clicked' id='" + id
									+ "'></span>");
					resultElm.find(".evaluator-up-clicked").hide().click(
							function() {
								$.post("./SearchExpert/queryEvaluator", {
									item : $(this).attr('id'),
									query : self.manager.store.params.q.value,
									action : $(this).attr('action')
								}, function(data) {
									if (data.code == 0) {
										resultElm.find('.evaluator-up-clicked')
												.hide();
										resultElm.find(
												'.evaluator-up-unclicked')
												.show();
									}

								})
							});

					// Add the up link and set his onClick function
					resultElm.find(".title").after(
							"<span class='evaluator-up-unclicked' id='" + id
									+ "'></span>");
					resultElm
							.find(".evaluator-up-unclicked")
							.show()
							.click(
									function() {
										$
												.post(
														"./SearchExpert/queryEvaluator",
														{
															item : $(this)
																	.attr('id'),
															query : self.manager.store.params.q.value,
															action : $(this)
																	.attr(
																			'action'),
															rank : 10
														},
														function(data) {
															if (data.code == 0) {
																if (resultElm
																		.find(
																				'.evaluator-down-clicked')
																		.is(
																				":visible")) {
																	resultElm
																			.find(
																					'.evaluator-down-clicked')
																			.hide();
																	resultElm
																			.find(
																					'.evaluator-down-unclicked')
																			.show();
																	resultElm
																			.find(
																					'.evaluator-up-clicked')
																			.show();
																	resultElm
																			.find(
																					'.evaluator-up-unclicked')
																			.hide();
																} else {
																	resultElm
																			.find(
																					'.evaluator-up-clicked')
																			.show();
																	resultElm
																			.find(
																					'.evaluator-up-unclicked')
																			.hide();
																}
															}
														})

									});

					// Add the down link and set his onClick function
					resultElm.find(".description #urlMobile .address").append(
							"<span class='evaluator-down-clicked' id='" + id
									+ "'></span>");
					resultElm
							.find(
									".description #urlMobile .address .evaluator-down-clicked")
							.hide()
							.click(
									function() {
										$
												.post(
														"./SearchExpert/queryEvaluator",
														{
															item : $(this)
																	.attr('id'),
															query : self.manager.store.params.q.value,
															action : $(this)
																	.attr(
																			'action')
														},
														function(data) {
															if (data.code == 0) {
																resultElm
																		.find(
																				'.evaluator-down-clicked')
																		.hide();
																resultElm
																		.find(
																				'.evaluator-down-unclicked')
																		.show();
															}
														})

									});

					// Add the down link and set his onClick function
					resultElm.find(".description #urlMobile .address").append(
							"<span class='evaluator-down-unclicked' id='" + id
									+ "'></span>");
					resultElm
							.find(
									".description #urlMobile .address .evaluator-down-unclicked")
							.show()
							.click(
									function() {
										$
												.post(
														"./SearchExpert/queryEvaluator",
														{
															item : $(this)
																	.attr('id'),
															query : self.manager.store.params.q.value,
															action : $(this)
																	.attr(
																			'action'),
															rank : 1
														},
														function(data) {
															if (data.code == 0) {
																if (resultElm
																		.find(
																				'.evaluator-up-clicked')
																		.is(
																				":visible")) {
																	resultElm
																			.find(
																					'.evaluator-up-clicked')
																			.hide();
																	resultElm
																			.find(
																					'.evaluator-up-unclicked')
																			.show();
																	resultElm
																			.find(
																					'.evaluator-down-clicked')
																			.show();
																	resultElm
																			.find(
																					'.evaluator-down-unclicked')
																			.hide();
																} else {
																	resultElm
																			.find(
																					'.evaluator-down-clicked')
																			.show();
																	resultElm
																			.find(
																					'.evaluator-down-unclicked')
																			.hide();
																}
															}
														})
									});

				}
			}
		});