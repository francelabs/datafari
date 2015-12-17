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
AjaxFranceLabs.TableWidget = AjaxFranceLabs.AbstractFacetWidget.extend({

	//Variables

	name : null,

	pagination : false,

	nbElmToDisplay : 10,

	sort : 'occurences',

	maxDisplay : 40,

	checkedOnTop : true,

	type : 'table',

	'facet.field' : true,

	//Methods

	buildWidget : function() {
		var endAnimationEvents= 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
		var animation = 'animated rotateIn';
		var self = this, elm = $(this.elm);
		elm.hide();
		elm.addClass('facet').addClass('tableWidget').addClass('widget').attr('widgetId', this.id).append('<ul></ul>');
		if(this.name != null){
			elm.prepend('<div class="facetName">')
				.find('.facetName').append('<i class="label fa fa-chevron-down"></i>').append('<span class="label la"></span>')
				.find('.label.la').append(this.name);
			elm.find('.facetName').toggle(function() {
					$('.facetSort, ul, .pagerModule.show', $(this).parents('.tableWidget')).hide();
					elm.find(".facetName i").removeClass('fa-chevron-down').addClass('fa-chevron-up '+animation).on(endAnimationEvents,function(){
						$(this).removeClass(animation);
					});
				}, function() {
					$('.facetSort, ul, .pagerModule.show', $(this).parents('.tableWidget')).show();
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
			for (var i = 0; i < max; i++) {
				if (data[i].name !== ''){
				elm.find('ul').append('<li></li>');
				elm.find('ul li:last').append('<label></label>');
				
				
				elm.find('ul li:last label').append('<div class="filterFacetCheck"></div>').append('<div class="filterFacetLabel"></div>');
				elm.find('ul li:last .filterFacetCheck').append('<input type="checkbox" value="' + data[i].name + '"/>');
				elm.find('ul li:last .filterFacetCheck input').attr('id',data[i].name);
				if (this.manager.store.find('fq', new RegExp(self.field + ':' + AjaxFranceLabs.Parameter.escapeValue(data[i].name.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&").replace(/\\/g,"\\\\")) + '[ )]')))
					elm.find('ul li:last .filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
				elm.find('ul li:last .filterFacetCheck input').change(function() {
					if ($(this).attr('checked') == 'checked') {
						if(self.selectionType === 'ONE' && elm.find('ul li .filterFacetCheck input:checked').not(this).length)
							self.remove(elm.find('ul li .filterFacetCheck input:checked').not(this).val());
						self.clickHandler();
						self.selectHandler($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
					} else {
						self.clickHandler();
						self.unselectHandler($(this).val().replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
					}
				});
				elm.find('ul li:last .filterFacetCheck').append('<label></label>');
				if (elm.find('ul li:last .filterFacetCheck input').attr('checked')== 'checked' )
					elm.find('ul li:last .filterFacetCheck label').attr('for', data[i].name).append('<span class="checkboxIcon fa fa-check-square-o">&nbsp;</span>'+'<span class="filterFacetLinkValue">'+AjaxFranceLabs.tinyString(data[i].name, 19)+'</span>').append('&nbsp;<span class="filterFacetLinkCount">(<span>' + data[i].nb + '</span>)</span>');
				else 
					elm.find('ul li:last .filterFacetCheck label').attr('for', data[i].name).append('<span class="checkboxIcon fa fa-square-o">&nbsp;</span>'+'<span class="filterFacetLinkValue">'+AjaxFranceLabs.tinyString(data[i].name, 19)+'</span>').append('&nbsp;<span class="filterFacetLinkCount">(<span>' + data[i].nb + '</span>)</span>');
				}
			}
	
			if (this.pagination) {
				this.pagination.source = $('ul', this.elm);
				this.pagination.updatePages();
			}
			this.sortBy(this.sort);
		}
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
				/*
					elm.find('ul').prepend($('ul li .filterFacetCheck input:checked', elm).parents('li'));
					elm.find('ul li .filterFacetCheck input:checked').parents('li').each(function() {
						var $this = this;
						$(this).nextAll().each(function() {
							if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() < $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase() && $('.filterFacetCheck input', this).attr('checked') == 'checked')
								$(this).after($($this).detach());
						});
					});
					elm.find('ul li .filterFacetCheck input:not(:checked)').parents('li').each(function() {
						var $this = this;
						$(this).nextAll().each(function() {
							if ($('.filterFacetCheck .filterFacetLinkValue', $this).text().toLowerCase() < $('.filterFacetCheck .filterFacetLinkValue', this).text().toLowerCase())
								$(this).after($($this).detach());
						});
					});
				*/
				
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
	}
});
