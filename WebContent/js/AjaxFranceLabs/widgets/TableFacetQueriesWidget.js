/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.TableFacetQueriesWidget
 * @extends	AjaxFranceLabs.AbstractFacetWidget
 *
 *
 */
AjaxFranceLabs.TableFacetQueriesWidget = AjaxFranceLabs.AbstractFacetWidget.extend({

	//Variables

	name : null,

	pagination : false,

	nbElmToDisplay : 10,

	sort : 'occurences',
	

	selectionType : 'ONE',
	
	field : null,

	maxDisplay : 40,

	checkedOnTop : true,

	type : 'tableFacetQueries',

	
	queries : [],
	
	labels : [],

	//Methods

	
	
	buildWidget : function() {
		var self = this, elm = $(this.elm);
		$.each(this.queries, function(i, query){
			self.manager.store.addByValue('facet.query', '{!key='+self.labels[i]+' ex=query}'+self.field+':'+query);
		});
		elm.addClass('facet').addClass('TableFacetQueriesWidget').addClass('tableWidget').addClass('widget').attr('widgetId', this.id).append('<div class="facetSort">').append('<ul>');
		if(this.name != null){
			elm.prepend('<div class="facetName">')
				.find('.facetName').append('<span class="hide_show close">').append('<span class="label">')
				.find('.label').append(this.name)
				.parent('.facetName').find('.hide_show').toggle(function() {
					$('.facetSort, ul, .PagerModule', $(this).parents('.tableWidget')).hide();
					$(this).addClass('close');
				}, function() {
					$('.facetSort, ul, .PagerModule', $(this).parents('.tableWidget')).show();
					$(this).removeClass('close');
				});
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
		var self = this, data = this.assocTags(this.manager.response.facet_counts.facet_queries), max = (data.length > this.maxDisplay) ? this.maxDisplay : data.length, elm = $(this.elm);
		elm.find('ul').empty();
		for (var i = 0; i < max; i++) {
			elm.find('ul').append('<li>');
			elm.find('ul li:last').append('<label>');
			
			
			elm.find('ul li:last label').append('<div class="filterFacetCheck">').append('<div class="filterFacetLabel">');
			elm.find('ul li:last .filterFacetCheck').append('<input type="checkbox" value="' + data[i].name + '"/>');
			elm.find('ul li:last .filterFacetCheck input').attr('id',data[i].name);
			var filter =  new RegExp(self.field + ':' + self.queries[i].replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"));
			if (this.manager.store.find('fq', filter))
				elm.find('ul li:last .filterFacetCheck input').attr('checked', 'checked').parents('li').addClass('selected');
			elm.find('ul li:last .filterFacetCheck input').change(function() {
				
				
				if ($(this).attr('checked') == 'checked') {

					
					if(self.selectionType === 'ONE' && elm.find('ul li .filterFacetCheck input:checked').not(this).length)
						self.remove(elm.find('ul li .filterFacetCheck input:checked').not(this).val());
					

					var checked = this;
					elm.find('input').each(function() {
						if (checked != this){
							$(this).attr('checked', false);
						}
					});
					
					
					self.clickHandler();
					self.selectHandler($(this).val());
				} else {
					self.clickHandler();
					self.unselectHandler($(this).val());
				}
			});
			
			
			elm.find('ul li:last .filterFacetCheck').append('<label>');
			elm.find('ul li:last .filterFacetCheck label').attr('for', data[i].name).append('<span class="checkboxIcon">&nbsp;</span>'+AjaxFranceLabs.tinyString(decodeURIComponent(data[i].name))).append('&nbsp;(' + data[i].nb + ')');
		}
		if (this.pagination) {
			this.pagination.source = $('ul', this.elm);
			this.pagination.updatePages();
		}
		this.sortBy(this.sort);
	},

	assocTags : function(data) {
		var self = this, tempData = Array();
		$.each(this.labels, function(i, label){
			if(data[label] !== undefined)
				tempData.push({
					name: label,
					nb: data[label]
				});
		});
		return tempData;
	},

	sortBy : function(sort) {
	},

	

	set : function(value) {
		this.manager.store.removeByValue('fq', new RegExp('^-?' + this.field + ':'));

		var self = this;
		var query;
		$.each(self.labels, function(i, label){
			if (label === value){
				query = self.queries[i];
			}
		});
		var valueToAdd = '{!tag=query}' + this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(query);
		return this.manager.store.addByValue('fq', valueToAdd);
	},
	
	

	remove : function(value) {
		var self = this;
		var query;
		$.each(self.labels, function(i, label){
			if (label === value){
				query = self.queries[i];
			}
		});
		var searchedValue = this.field + ':' + AjaxFranceLabs.Parameter.escapeValue(query);
		return this.manager.store.removeByValue('fq', '{!tag=query}'+searchedValue);
	},
    clickHandler: function () {},
	afterRequest: function() {
		this.update();
	}
});

