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
	
	rootLevel : 0,
	
	maxDepth : 3,
	
	separator : '/',

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
		if(data.length == 0) {
			elm.hide();
		} else {
			elm.show();
			elm.find('ul').empty();
			var ul = elm.find('ul:last-child');
			
			var realMaxDepth = this.rootLevel + this.maxDepth;
			var LevelsHashMap = {};
			for(var i = this.rootLevel; i < realMaxDepth; i++) {
				LevelsHashMap["level" + i] = {};
			}
			
			for (var i = 0; i < max; i++) {
				
				var level = data[i].name.substring(0,1);
				var levelName = data[i].name.substring(1);
				if(level == this.rootLevel) {
					LevelsHashMap["level" + level][levelName] = {"array":[], "nb":data[i].nb, "original":data[i].name};
				} else if (this.rootLevel < level && level < realMaxDepth){
					var previousLevel = levelName.substring(0, levelName.lastIndexOf(this.separator));
					LevelsHashMap["level" + level][levelName] = {"array":[], "nb":data[i].nb, "original":data[i].name};
					var previousLevelArray = LevelsHashMap["level" + (level - 1)][previousLevel].array;
					previousLevelArray[previousLevelArray.length] = levelName;
				}
			}
			
			for (var level in LevelsHashMap["level" + this.rootLevel]) {
				this.displayLevel(LevelsHashMap["level" + this.rootLevel][level], level, this.rootLevel, LevelsHashMap, elm, self, ul);
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
	
	displayLevel : function(level, levelName, levelNum, levelsHashMap, elm, widget, currentUl) {
		var checkboxValue = levelName;
		var count = level.nb;
		if(levelNum > widget.rootLevel) {
			checkboxValue = checkboxValue.substring(checkboxValue.lastIndexOf(widget.separator));
		}
		currentUl.append('<li></li>');
		var currentLi = currentUl.find('li:lastchild');
		currentLi.append('<label></label>');
		currentLi.find('label').append('<span class="filterFacetCheck"></span>');
		currentLi.find('.filterFacetCheck').append('<input type="checkbox" value="' + level.original + '"/>');
		currentLi.find('.filterFacetCheck input').attr('id',checkboxValue);
		if (this.manager.store.find('fq', new RegExp(widget.field + ':' + AjaxFranceLabs.Parameter.escapeValue(level.original.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&").replace(/\\/g,"\\\\")) + '[ )]'))) {
			currentLi.find('.filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
		}
		if(currentUl.prev('label').find('.filterFacetCheck input').attr('checked') == 'checked') {
			currentLi.find('.filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
		}
		currentLi.find('.filterFacetCheck input').change(function() {
			if ($(this).attr('checked') == 'checked') {	
				$(this).closest('li').children('ul').children('li').each(function() {
					widget.unselectNoRequest($(this).find('.filterFacetCheck input').val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
				});
				widget.clickHandler();
				widget.selectHandler($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
			} else {
				if(currentUl.prev('label').find('.filterFacetCheck input').attr('checked') == 'checked') {
					widget.unselectNoRequest(currentUl.prev('label').find('.filterFacetCheck input').val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
					currentUl.children('li').each(function() {
						if($(this).find('.filterFacetCheck input').attr('checked') == 'checked') {
							widget.selectNoRequest($(this).find('.filterFacetCheck input').val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
						}
					});
				} 
				widget.clickHandler();
				widget.unselectNoRequest($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
				widget.manager.makeRequest();
			}
		});
		currentLi.find('.filterFacetCheck').append('<label></label>');
		if (currentLi.find('.filterFacetCheck input').attr('checked')== 'checked' ) {
			currentLi.find('.filterFacetCheck label').attr('for', checkboxValue).append('<span class="checkboxIcon fa fa-check-square-o">&nbsp;</span>'+'<span class="filterFacetLinkValue">'+AjaxFranceLabs.tinyString(checkboxValue, 39)+'</span>').append('&nbsp;<span class="filterFacetLinkCount">(<span>' + count + '</span>)</span>');
		} else {
			currentLi.find('.filterFacetCheck label').attr('for', checkboxValue).append('<span class="checkboxIcon fa fa-square-o">&nbsp;</span>'+'<span class="filterFacetLinkValue">'+AjaxFranceLabs.tinyString(checkboxValue, 39)+'</span>').append('&nbsp;<span class="filterFacetLinkCount">(<span>' + count + '</span>)</span>');
		}
		
		if(level.array.length > 0) {
			currentLi.prepend('<span class="collapse">\></span>');
			currentLi.children('span').click(function() {
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
			currentLi.append('<ul></ul>');
			var newUl = currentLi.find('ul:last-child');
			for(var index = 0; index < level.array.length; index++) {
				var subLevelName = level.array[index];
				var nextLevel = levelsHashMap["level" + (levelNum + 1)];
				this.displayLevel(nextLevel[subLevelName], subLevelName, levelNum + 1, levelsHashMap, elm, widget, newUl);
			}
		}
	}
});
