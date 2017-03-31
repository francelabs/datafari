

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
	core : "FileShare",
	parentWidget : "",
	isMobile : $(window).width()<800,

	setParentWidget : function(widget) {
		this.parentWidget = widget;
	}, 
	
	//Methods

	addElevatorLinks : function(resultElm, id, query) {
		var style = "";
		if (window.isLikesAndFavoritesEnabled) {
			style = "style='margin-right: 2em;'"
		}
		
		if(!this.isMobile) {
			var self = this;
			
			// Add the up link and set his onClick function
			resultElm.find(".title").after("<span class='elevator-up' " + style + " action='up' id='" + id + "'></span>");
			resultElm.find(".elevator-up").click(function() {
				
				self.parentWidget.beforeRequest(); // Display the loader icon
				
				// Send the POST request to elevate the selected doc
				$.post("./SearchExpert/queryElevator", {item: $(this).attr('id'), query: self.manager.store.params.q.value, action: $(this).attr('action')},function(data){
					// If successful, reload the Solr core and refresh the searchView
					if(data.code == 0) {
						$.get("./admin/proxy/solr/admin/cores?action=RELOAD&core=" + self.core,function(){
							self.manager.makeRequest();
						});
					}
				})
			});
			
			// Add the down link and set his onClick function
			resultElm.find(".description #urlMobile .address").append("<span class='elevator-down' " + style + " action='down' id='" + id + "'></span>");
			resultElm.find(".description #urlMobile .address .elevator-down").click(function() {
				
				self.parentWidget.beforeRequest(); // Display the loader icon
				
				// Send the POST request to remove the selected doc from the elevate list
				$.post("./SearchExpert/queryElevator", {item: $(this).attr('id'), query: self.manager.store.params.q.value, action: $(this).attr('action')},function(data){
					if(data.code == 0) {
						$.get("./admin/proxy/solr/admin/cores?action=RELOAD&core=" + self.core,function(){
							self.manager.makeRequest();
						});
					}
				},"json")
			});
		
		}
	}
});
