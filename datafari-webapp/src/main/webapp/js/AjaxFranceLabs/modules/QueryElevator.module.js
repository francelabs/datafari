

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
AjaxFranceLabs.QueryElevatorModule = AjaxFranceLabs.AbstractModule.extend({

	//Variables
	core : "@MAINCOLLECTION@",
	parentWidget : "",
	activated : false,
	isMobile : $(window).width()<800,
	elevatedDocs : null,

	setParentWidget : function(widget) {
		var self = this;
		this.parentWidget = widget;
		$.get("./WidgetManager", {id:"queryelevator", activated:self.activated}, function(data){
			if(data.activated === "true") {
				self.activated = true
			} else {
				self.activated = false;
			}
		});
	}, 
	
	initElevatedDocs : function(query) {
		var self = this;
		$.get("./SearchExpert/queryElevator", { get: "docs", query : query},function(data){
			if(data.code == 0) {
				self.elevatedDocs = data.docs;
			} else {
				self.elevatedDocs = [];
			}
		},"json")
	},
	
	updateNums : function() {
		var self = this;
		for(var i=0; i < self.elevatedDocs.length; i++) {
			var docId = self.elevatedDocs[i];
			$(document.getElementById(docId)).find(".elevator-num").html(i + 1);
		}
	},

	addElevatorLinks : function(resultElm, id, query) {
		
		var self = this;
		
		if(self.activated == true) {
		
			var style = "";
			if (window.isLikesAndFavoritesEnabled) {
				style = "style='margin-right: 2em;'"
			}
			
			if(!this.isMobile) {
				// Get the position of the current doc
				var docPos = "";
				if(self.elevatedDocs != null && self.elevatedDocs != undefined) {
					docPos = self.elevatedDocs.indexOf(id);
				}
				
				// Add the up link and set his onClick function
				resultElm.find(".title").after("<span class='elevator-up' " + style + " action='up' id='" + id + "'></span>");
				resultElm.find(".elevator-up").click(function() {
					
					var currentDocID = $(this).attr('id');
					// Send the POST request to elevate the selected doc
					$.post("./SearchExpert/queryElevator", {item: currentDocID, query: self.manager.store.params.q.value, action: $(this).attr('action')},function(data){
						// If successful, reload the Solr core and refresh the searchView
						if(data.code == 0) {
							var docIndex = self.elevatedDocs.indexOf(currentDocID);
							if(docIndex === -1) {
								self.elevatedDocs.push(currentDocID);
							} else if(docIndex !== 0) {
								self.elevatedDocs.splice(docIndex, 1);
								self.elevatedDocs.splice(docIndex - 1, 0, currentDocID);
							}
							if(resultElm.find(".elevator-num")[0] !== undefined) {
								self.updateNums();
							} else  {
								resultElm.find(".elevator-up").after("<span class='elevator-num' " + style + ">" + self.elevatedDocs.length + "</span>");
								document.getElementById(id).getElementsByClassName("elevator-down")[0].style.display = "";
								document.getElementById(id).getElementsByClassName("elevator-remove")[0].style.display = "";
							}
						}
					})
				});
				if(docPos !== "" && docPos !== -1) {
					resultElm.find(".elevator-up").after("<span class='elevator-num' " + style + ">" + (docPos + 1) + "</span>");
				}
				
				
				// Add the down link and set his onClick function
				resultElm.find(".description #urlMobile .address").append("<span class='elevator-down' " + style + " action='down' id='" + id + "'></span>");
				resultElm.find(".description #urlMobile .address .elevator-down").click(function() {
					
					var currentDocID = $(this).attr('id');
					
					// Send the POST request to remove the selected doc from the elevate list
					$.post("./SearchExpert/queryElevator", {item: currentDocID, query: self.manager.store.params.q.value, action: $(this).attr('action')},function(data){
						if(data.code == 0) {
							var index = self.elevatedDocs.indexOf(currentDocID);
							if(index < self.elevatedDocs.length -1) {
								self.elevatedDocs.splice(index, 1);
								self.elevatedDocs.splice(index + 1, 0, currentDocID);
							}
							self.updateNums();
						}
						else {
						}
					},"json")
				});
				
				// Add the remove link and set his onClick function
				resultElm.find(".description #urlMobile .address").append("<span class='elevator-remove' " + style + " action='remove' id='" + id + "'><i class='fa fa-trash-o'></i></span>");
				resultElm.find(".description #urlMobile .address .elevator-remove").click(function() {
					
					var currentDocID = $(this).attr('id');
					
					// Send the POST request to remove the selected doc from the elevate list
					$.post("./SearchExpert/queryElevator", {item: currentDocID, query: self.manager.store.params.q.value, action: $(this).attr('action')},function(data){
						if(data.code == 0) {
							document.getElementById(id).getElementsByClassName("elevator-down")[0].style.display = "none";
							document.getElementById(id).getElementsByClassName("elevator-num")[0].remove();
							document.getElementById(id).getElementsByClassName("elevator-remove")[0].remove();
							var index = self.elevatedDocs.indexOf(currentDocID);
							self.elevatedDocs.splice(index, 1);
							self.updateNums();
						}
					},"json")
				});
				
				if(docPos === "" || docPos === -1) {
					document.getElementById(id).getElementsByClassName("elevator-down")[0].style.display = "none";
					document.getElementById(id).getElementsByClassName("elevator-remove")[0].style.display = "none";
				}
				
			}
			
		
		}
	}
});
