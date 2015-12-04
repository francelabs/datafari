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
AjaxFranceLabs.HierarchicalFacetWidget = AjaxFranceLabs.AbstractFacetWidget.extend({

	//Variables

	name : null,

	pagination : false,

	nbElmToDisplay : 10,

	sort : 'occurences',

	maxDisplay : 40,

	checkedOnTop : true,

	type : 'table',

	'facet.field' : true,
	
	// The root level defines the depth from which the hierarchical tree will starts
	rootLevel : 0,
	
	// Defines the maximum depth to display, starting from the root
	// For example, with rootLevel=1 and maxDepth=3, the maximum dispalyed depth will be /home/france/labs/datafari/search (/home/france = rootLevel) 
	maxDepth : 3,
	
	// Separator of each level
	separator : '/',
	
	LevelsHashMap : {},

	//Methods

	buildWidget : function() {
		var endAnimationEvents= 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
		var animation = 'animated rotateIn';
		var self = this, elm = $(this.elm);
		elm.hide();
		elm.addClass('facet').addClass('hierarchicalWidget').addClass('widget').attr('widgetId', this.id).append('<ul></ul>');
		if(this.name != null){
			elm.prepend('<div class="facetName">')
				.find('.facetName').append('<i class="label fa fa-chevron-down"></i>').append('<span class="label la"></span>')
				.find('.label.la').append(this.name);
			elm.find('.facetName').toggle(function() {
					$('.facetSort, ul, .pagerModule.show', $(this).parents('.hierarchicalWidget')).hide();
					elm.find(".facetName i").removeClass('fa-chevron-down').addClass('fa-chevron-up '+animation).on(endAnimationEvents,function(){
						$(this).removeClass(animation);
					});
				}, function() {
					$('.facetSort, ul, .pagerModule.show', $(this).parents('.hierarchicalWidget')).show();
					elm.find(".facetName i").removeClass('fa-chevron-up').addClass(animation+ ' fa-chevron-down').on(endAnimationEvents,function(){
						$(this).removeClass(animation);
					});
				});
		}
		elm.find('.facetSort').append('<label></label>').find('label').append(window.i18n.msgStore['sortFacet']).append('<select></select>').find('select').append('<option><span>A/Z</span></option>').append('<option><span>Z/A</span></option>').append('<option><span>Occurences</span></option>').change(function(event) {
				switch($('option:selected', this).index()) {
				case 0:
					self.sort = 'AtoZ';
					break;
				case 1:
					self.sort = 'ZtoA';
					break;
				case 2:
					self.sort = 'occurences';
					break;
			}
			self.sortBy(self.sort);
		});
		switch(this.sort) {
			case 'AtoZ':
				$(this.elm).find('.facetSort option:eq(0)').attr('selected', 'selected');
				break;
			case 'ZtoA':
				$(this.elm).find('.facetSort option:eq(1)').attr('selected', 'selected');
				break;
			case 'occurences':
				$(this.elm).find('.facetSort option:eq(2)').attr('selected', 'selected');
				break;
		}
		if (this.pagination === true) {
			this.pagination = new AjaxFranceLabs.PagerModule({
				elm : this.elm,
				updateList : function() {
					if (this.nbPage > 1) {
						$(this.source).children().css('display', 'none').slice(this.pageSelected * this.nbElmToDisplay, (this.pageSelected + 1) * this.nbElmToDisplay).css('display', this.display);
						AjaxFranceLabs.clearMultiElementClasses($('li', this.source));
						AjaxFranceLabs.addMultiElementClasses($('li:visible', this.source));
					}
				}
			});
			this.pagination.manager = this.manager;
		}
		if (this.pagination)
			this.pagination.init();
	},

	update : function() {
		var self = this, data = this.assocTags(this.manager.response.facet_counts.facet_fields[this.field]), max = (data.length > this.maxDisplay) ? this.maxDisplay : data.length, elm = $(this.elm);
		if(data.length == 0) { // if no data to display, hide the widget
			elm.hide();
		} else {
			elm.show(); // show the widget
			elm.find('ul').empty(); // empty the widget content
			var ul = elm.find('ul:last-child'); // select the list element
			
			var realMaxDepth = this.rootLevel + this.maxDepth;
			
			// Creates the hierarchy as an object 
			for(var i = this.rootLevel; i < realMaxDepth; i++) {
				this.LevelsHashMap["level" + i] = {}; // initializes the depth levels 
			}
			
			for (var i = 0; i < max; i++) { // For each facet result, determines the level depth, creates the corresponding level object and stock it in the LevelsHashMap 
				var levelDepth = data[i].name.substring(0,1); // determines the depth of the current level
				var levelName = data[i].name.substring(1); // determines the level name
				if(levelDepth == this.rootLevel) { // it is a root level (it's depth corresponds to the defined rootLevel depth)
					this.LevelsHashMap["level" + levelDepth][levelName] = {"array":[], "nb":data[i].nb, "original":data[i].name};
				} else if (this.rootLevel < levelDepth && levelDepth < realMaxDepth){ // check that the depth is allowed (between the rootLevel depth and the maximum depth defined), otherwise, the current level is ignored
					var parentLevel = levelName.substring(0, levelName.lastIndexOf(this.separator)); // determines the parent level
					this.LevelsHashMap["level" + levelDepth][levelName] = {"array":[], "nb":data[i].nb, "original":data[i].name};
					var parentLevelChildrenArray = this.LevelsHashMap["level" + (levelDepth - 1)][parentLevel].array; // retrieve the parent level children list
					parentLevelChildrenArray[parentLevelChildrenArray.length] = levelName; // add the name of the current level in the parent level children list as a reference
				}
			}
			
			for (var levelName in this.LevelsHashMap["level" + this.rootLevel]) { // for each root level, constructs the hierarchy html list
				this.displayLevel(this.LevelsHashMap["level" + this.rootLevel][levelName], levelName, this.rootLevel, self, ul);
			}
			
		}
		if (this.pagination) {
			this.pagination.source = $('ul', this.elm);
			this.pagination.updatePages();
		}
		this.sortBy(this.sort);
	},

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

	sortBy : function(sort) {
		var elm = $(this.elm);
		switch(sort) {
			case 'AtoZ':
				if (this.checkedOnTop === true) {
					elm.find('ul').prepend($(this.elm).find('ul li .filterFacetCheck input:checked').parents('li'));
					elm.find('ul li .filterFacetCheck input:checked').parents('li').each(function() {
						var $this = this;
						$(this).nextAll().each(function() {
							if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() > $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase() && $('.filterFacetCheck input', this).attr('checked') == 'checked')
								$(this).after($($this).detach());
						});
					});
					elm.find('ul li .filterFacetCheck input:not(:checked)').parents('li').each(function() {
						var $this = this;
						$(this).nextAll().each(function() {
							if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() > $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase())
								$(this).after($($this).detach());
						});
					});
				} else {
					elm.find('ul li').each(function() {
						var $this = this;
						$(this).nextAll().each(function() {
							if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() > $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase())
								$(this).after($($this).detach());
						});
					});
				}
				break;
			case 'ZtoA':			
					elm.find('ul li').each(function() {
						var $this = this;
						$(this).nextAll().each(function() {
							if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() < $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase())
								$(this).after($($this).detach());
						});
					});
				
				break;
			case 'occurences':
				elm.find('ul li').each(function() {
					var $this = this;
					$(this).nextAll().each(function() {
						if (parseInt($('.filterFacetCheck .filterFacetLinkCount span', $this).text()) < parseInt($('.filterFacetCheck .filterFacetLinkCount span', this).text()))
							$(this).after($($this).detach());
					});
				});
				break;
		}
		if (this.pagination)
			this.pagination.updateList();
		AjaxFranceLabs.clearMultiElementClasses($(this.elm).find('ul li'));
		AjaxFranceLabs.addMultiElementClasses($(this.elm).find('ul li:visible'));
	},

	clickHandler : function() {
	},

	afterRequest : function() {
		this.update();
	},
	
	selectNoRequest : function(value) {
		var self = this;
		if (self[(self.selectionType === 'ONE' ? 'set' : 'add')](value)) {
			self.manager.store.remove('start');
		}
	},

	unselectNoRequest : function(value) {
		var self = this;
		if (self.remove(value)) {
			self.manager.store.remove('start');
		}
	},
	
	// Constructs the provided level as a line in the provided html list
	//
	// level : the level object to display
	// levelName : the level name
	// levelDepth : the level depth
	// widget : this widget
	// currentUl : the HTML list where to add the provided level as a new line
	displayLevel : function(level, levelName, levelDepth, widget, currentUl) {
		var checkboxValue = levelName;
		var count = level.nb;
		if(levelDepth > widget.rootLevel) { // trick to keep the entire name of root levels but only the last part for non root levels (matter of label display) 
			checkboxValue = checkboxValue.substring(checkboxValue.lastIndexOf(widget.separator));
		}
		currentUl.append('<li></li>');
		var currentLi = currentUl.find('li:lastchild');
		// Constructs the checkbox and its  attached label
		currentLi.append('<label></label>');
		currentLi.find('label').append('<span class="filterFacetCheck"></span>');
		currentLi.find('.filterFacetCheck').append('<input type="checkbox" value="' + level.original + '"/>');
		currentLi.find('.filterFacetCheck input').attr('id',checkboxValue);
		if (this.manager.store.find('fq', new RegExp(widget.field + ':' + AjaxFranceLabs.Parameter.escapeValue(level.original.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&").replace(/\\/g,"\\\\")) + '[ )]'))) {
			// The checkbox value is used in the search query, so the checked attribute of the checkbox is set to 'checked'
			currentLi.find('.filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');   
		}
		if(currentUl.prev('label').find('.filterFacetCheck input').attr('checked') == 'checked') { // check if the parent level checkbox is checked
			// the parent level checkbox is checked so, as a child, this checkbox must be checked too
			currentLi.find('.filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
		}
		currentLi.find('.filterFacetCheck input').change(function() { // sets the onChange function of the checkbox
			if ($(this).attr('checked') == 'checked') {
				// the checkbox is checked so the value must be added as a filter of the search query
				$(this).closest('li').children('ul').children('li').each(function() { // if the current checkbox has children, removes all the children filters from the search query as the parent filter covers the children ones  
					widget.unselectNoRequest($(this).find('.filterFacetCheck input').val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
				});
				widget.clickHandler();
				widget.selectHandler($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&")); // add the checkbox value as a filter for the search query
			} else {
				// the checkbox is not checked so the value must be removed from the search query filters
				if(currentUl.prev('label').find('.filterFacetCheck input').attr('checked') == 'checked') { // if the parent of the checkbox is checked, removes the parent value from the search query filters and adds the other children values as filter to fit to the behavior expected by the user 
					widget.unselectNoRequest(currentUl.prev('label').find('.filterFacetCheck input').val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&")); // removes the parent value from the search query filters
					currentUl.children('li').each(function() {
						// for each checked children, adds their values to the search query filters 
						if($(this).find('.filterFacetCheck input').attr('checked') == 'checked') {
							widget.selectNoRequest($(this).find('.filterFacetCheck input').val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
						}
					});
				} 
				widget.clickHandler();
				widget.unselectNoRequest($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&")); // removes the value of the checkbox from the search query filters
				widget.manager.makeRequest(); // forces the request to update the results and facets
			}
		});
		currentLi.find('.filterFacetCheck').append('<label></label>');
		// Apply the correct checkbox image regarding the checkbox state (checked or not checked)
		if (currentLi.find('.filterFacetCheck input').attr('checked')== 'checked' ) {
			currentLi.find('.filterFacetCheck label').attr('for', checkboxValue).append('<span class="checkboxIcon fa fa-check-square-o">&nbsp;</span>'+'<span class="filterFacetLinkValue">'+AjaxFranceLabs.tinyString(checkboxValue, 39)+'</span>').append('&nbsp;<span class="filterFacetLinkCount">(<span>' + count + '</span>)</span>');
		} else {
			currentLi.find('.filterFacetCheck label').attr('for', checkboxValue).append('<span class="checkboxIcon fa fa-square-o">&nbsp;</span>'+'<span class="filterFacetLinkValue">'+AjaxFranceLabs.tinyString(checkboxValue, 39)+'</span>').append('&nbsp;<span class="filterFacetLinkCount">(<span>' + count + '</span>)</span>');
		}
		
		// If the current level has children, makes a recursive call to this method in order to display the children levels
		if(level.array.length > 0) {
			// add a "button" to expand/collapse the current level and show/hide the children
			currentLi.prepend('<span class="collapse">\></span>');
			currentLi.children('span').click(function() { // add the click function to the expand/collapse span 
				var endAnimationEvents= 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
				if($(this).attr('class') == "collapse") {
					$(this).removeClass('collapse').addClass('expand collapse2expand').on(endAnimationEvents,function(){
						$(this).removeClass('collapse2expand');
					});
				} else {
					$(this).removeClass('expand').addClass('collapse expand2collapse').on(endAnimationEvents,function(){
						$(this).removeClass('expand2collapse');
					});
				}
		        $(this).parent().find('ul').toggle();
		    });
			
			// creates the new ul element and recursive call this method on it in order to fill it with the children levels
			currentLi.append('<ul></ul>');
			var newUl = currentLi.find('ul:last-child');
			for(var index = 0; index < level.array.length; index++) {
				var childLevelName = level.array[index];
				var childLevel = widget.LevelsHashMap["level" + (levelDepth + 1)];
				this.displayLevel(childLevel[childLevelName], childLevelName, levelDepth + 1, widget, newUl);
			}
		}
	}
});
