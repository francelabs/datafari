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
AjaxFranceLabs.StatsQueryWidget = AjaxFranceLabs.AbstractWidget.extend({

	//Variables

	autocomplete : false,

	autocompleteOptions : {
		optionsExtend : true,
		render : null,
		field : null,
		valueSelectFormat : function(value) {
			return value;
		},
		singleValue : false,
		openOnFieldFocus : false
	},
	
	
	removeContentButton : false,

	type : 'searchBar',

	//Methods
	
	clean : function() {
		this.manager.store.remove('start');
		this.manager.store.remove('fq');
		$.each(this.manager.widgets, function(index, widget) {
			if (typeof widget.pagination !== "undefined"){
				widget.pagination.pageSelected = 0;
			}
		});
		
	},
	
	buildWidget : function() {
		var self = this, elm = $(this.elm);
		elm.addClass('searchBarWidget').addClass('widget').append('<div class="searchType"></div>').append('<div class="filterBar"></div>').append('<div class="searchDate"></div>').append('<input class="search" type="button" value="'+window.i18n.msgStore['processStats']+'"/>');
		elm.find('.filterBar').append('Filter by keyword : <input type="text" />');
		if(this.removeContentButton)
			elm.find('.filterBar').append('<span class="removeContent"></span>').find('.removeContent').css('display', 'none').append('<span>X</span>').click(function(){
				elm.find('.filterBar input[type=text]').val('');
				$(this).css('display', 'none');
				self.clean();
				self.manager.makeRequest();
			});
		

		
		elm.find('.searchType').append('Type : ').append('<div></div>').append('<div></div>').append('<div></div>').append('<div></div>');
		elm.find('.searchType div').attr('style','display: inline').append('<input type="radio" name="searchType" class="radio" />').append('<label></label>');
		elm.find('.searchType div:eq(0)').find('input').attr('value', 'topQueries').attr('checked','true').attr('id', 'topQueries').parent().find('label').attr('for', 'topQueries').append('<span>&nbsp;</span>').append(window.i18n.msgStore['topQueries']);
		elm.find('.searchType div:eq(1)').find('input').attr('value', 'notHitsQueries').attr('id', 'notHitsQueries').parent().find('label').attr('for', 'notHitsQueries').append('<span>&nbsp;</span>').append(window.i18n.msgStore['notHitsQueries']);
		elm.find('.searchType div:eq(2)').find('input').attr('value', 'noClicksQueries').attr('id', 'noClicksQueries').parent().find('label').attr('for', 'noClicksQueries').append('<span>&nbsp;</span>').append(window.i18n.msgStore['noClicksQueries']);
		elm.find('.searchType div:eq(3)').find('input').attr('value', 'abnormalQueryTime').attr('id', 'abnormalQueryTime').parent().find('label').attr('for', 'abnormalQueryTime').append('<span>&nbsp;</span>').append(window.i18n.msgStore['abnormalQueryTime']);
		
		

		elm.find('.searchDate').append('Filter by date : ').append('<div></div>').append('<div></div>').append('<div></div>').append('<div></div>');
		elm.find('.searchDate div').attr('style','display: inline').append('<input type="radio" name="dateRange" class="radio" />').append('<label></label>');
		elm.find('.searchDate div:eq(0)').find('input').attr('value', 'all').attr('checked','true').attr('id', 'all').parent().find('label').attr('for', 'all').append('<span>&nbsp;</span>').append(window.i18n.msgStore['all']);
		elm.find('.searchDate div:eq(1)').find('input').attr('value', 'last12months').attr('id', 'last12months').parent().find('label').attr('for', 'last12months').append('<span>&nbsp;</span>').append(window.i18n.msgStore['last12months']);
		elm.find('.searchDate div:eq(2)').find('input').attr('value', 'last6months').attr('id', 'last6months').parent().find('label').attr('for', 'last6months').append('<span>&nbsp;</span>').append(window.i18n.msgStore['last6months']);
		elm.find('.searchDate div:eq(3)').find('input').attr('value', 'last30days').attr('id', 'last30days').parent().find('label').attr('for', 'last30days').append('<span>&nbsp;</span>').append(window.i18n.msgStore['last30days']);
		elm.find('.searchDate div:eq(4)').find('input').attr('value', 'last24hours').attr('id', 'last24hours').parent().find('label').attr('for', 'last24hours').append('<span>&nbsp;</span>').append(window.i18n.msgStore['last24hours']);
		elm.find('input[type=button].search').click(function() {
			self.makeNewRequest();
		});
	},
	
	makeNewRequest : function(){

		this.clean();
		this.manager.makeRequest();
	},

	beforeRequest : function() {
		var search = (AjaxFranceLabs.empty(AjaxFranceLabs.trim($(this.elm).find('.filterBar input').val()))) ? '*:*' : AjaxFranceLabs.trim($(this.elm).find('.filterBar input').val());
		if (this.autocomplete)
			search = search.replace(/\u200c/g, '');
		switch($(this.elm).find('input[name=searchType]:checked').val()){
            case "notHitsQueries":
            	Manager.store.addByValue("fq", '{!join from=q to=q}noHits:1');
                break;
            // TODO : modify this : horrible complexity!!!
			case 'noClicksQueries':	
            	Manager.store.addByValue("fq", '-{!join from=q to=q}click:1');
				break;
			case 'abnormalQueryTime':	
            	Manager.store.addByValue("fq", 'QTime:[1000 TO *]');
				break;
            default:
                break;
		}
		
		switch($(this.elm).find('input[name=dateRange]:checked').val()){
		
		
			case "last24hours":
            	Manager.store.addByValue("fq", 'date:[NOW-24HOUR TO NOW]');
                break;
            case "last30days":
            	Manager.store.addByValue("fq", 'date:[NOW-30DAY TO NOW]');
                break;
			case 'last6months':	
            	Manager.store.addByValue("fq", 'date:[NOW-6MONTH TO NOW]');
				break;
			case 'last12months':	
            	Manager.store.addByValue("fq", 'date:[NOW-12MONTH TO NOW]');
				break;
            default:
                break;
		}
		
		
		this.manager.store.get('q').val(search);
	},
	
	afterRequest : function(){
		
	}
	
});
