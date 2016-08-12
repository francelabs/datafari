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
AjaxFranceLabs.TabWidget = AjaxFranceLabs.AbstractFacetWidget.extend({

	//Variables

	type : 'tab',
	'facet.field' : true,
	actifValue : '',
	
	//Methods
	

	// Constructor
	buildWidget : function() {
		$(this.elm).addClass('tabWidget').addClass('widget').append('<ul></ul>');
		$(this.elm).hide();	
		this.manager.store.addByValue('f.' + this.field + '.facet.mincount', '0'); // Set the facet min count to 0 in order to display the tabs even if there is no results associated to their facet query
	},
	
	// Get facet field values and their corresponding numbers then return them in an object
	assocTags : function(data) {
		var tags = [];
		for (var i = 0; i < data.length - 1; i++) {
			tags.push({
				name : data[i],
				nb : data[i + 1]
			});
			i++;
		}
		return tags;
	},

	
	afterRequest : function() {
		var self = this;
		var data = this.assocTags(this.manager.response.facet_counts.facet_fields[this.field]); // Get the facet field values
		var elm = $(this.elm);
		if(data.length !== 0) { // Some values exist
			$(self.elm).show();
			elm.find('ul').empty(); // empty the widget content
			var ul = elm.children('ul'); // select the list element
			var total = 0;
			for (var i = 0; i < data.length; i++) { // For each value, create a tab (<li> element) which will contain the value and his associated number
				// For good practice, the value and the number have their own <span> tag, which allows easy tweak and CSS conf
				
				total += data[i].nb; // Calculate the total number regrouping all the values which will be used for the 'All' tab
				
				// Create the tab and his associated click function that will trigger the facet query
				ul.append("<li><span class='tab'><span class='name'>" + data[i].name + "</span><span class='number'>(" + data[i].nb + ")</span></span></li>");
				ul.find('li:lastchild').find('.tab').click(function() { // 
					self.actifValue = $(this).find('.name').text();
					self.selectHandler(self.actifValue);
				});
			}
			
			//Create the 'All' tab and his associated click function that will remove the last selected facet query to retrieve all documents on the search view
			ul.prepend("<li><span class='tab'><span class='name'>All</span><span class='number'>" + "(" + total + ")</span></span></li>");
			ul.find('li:firstchild').find('.tab').click(function() {
				self.unselectHandler(self.actifValue);
			});
		} else { 
			$(self.elm).hide();
		}
		
	}
	
	
});
