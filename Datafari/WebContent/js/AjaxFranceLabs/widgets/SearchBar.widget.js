/**
 *
 * Copyright France Labs Licensed under the Apache License, V2.0 | http://www.francelabs.com/en/AjaxFranceLabs/licence
 * @class	AjaxFranceLabs.SearchBarWidget
 * @extends	AjaxFranceLabs.AbstractWidget
 *
 * @documentation http://www.francelabs.com/en/AjaxFranceLabs/widget-module-documentation#searchBar_widget
 *
 */
AjaxFranceLabs.SearchBarWidget = AjaxFranceLabs.AbstractWidget.extend({

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
	
	updateBrowserAddressBar : true,
	
	removeContentButton : false,

	type : 'searchBar',

	//Methods
	
	updateAddressBar : function(){
		if (this.updateBrowserAddressBar == true){
			var searchType = 'allWords';
			var radios = $('#searchBar').find('.searchMode input[type=radio]');
			$.each(radios, function(key, radio) {
				if (radio.checked) {
					searchType = radio.value;
				}
			});
			

			try {
					var query = $('.searchBar input[type=text]').val();
					window.history.pushState('Object', 'Title', window.location.pathname+'?searchType='+searchType+'&query='+query);
				}
				catch (e) {
				}

			
		}
	},
	
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
		elm.addClass('searchBarWidget').addClass('widget').append('<div class="searchBar">').append('<div class="searchMode">');
		elm.find('.searchBar').append('<input type="text" />').append('<input class="search" type="button" />');
		if(this.removeContentButton)
			elm.find('.searchBar').append('<span class="removeContent" />').find('.removeContent').css('display', 'none').append('<span>X</span>').click(function(){
				elm.find('.searchBar input[type=text]').val('');
				$(this).css('display', 'none');
				self.clean();
				self.manager.makeRequest();
			});
		

		
		elm.find('.searchMode').append('<div>').append('<div>').append('<div>');
		elm.find('.searchMode div').attr('style','display: inline').append('<input type="radio" name="searchType" class="radio" />').append('<label>');
		elm.find('.searchMode div:eq(0)').find('input').attr('value', 'allWords').attr('checked','true').attr('id', 'allWords').parent().find('label').attr('for', 'allWords').append('<span>&nbsp;</span>').append(window.i18n.msgStore['allWords']);
		elm.find('.searchMode div:eq(1)').find('input').attr('value', 'atLeastOneWord').attr('id', 'atLeastOneWord').parent().find('label').attr('for', 'atLeastOneWord').append('<span>&nbsp;</span>').append(window.i18n.msgStore['atLeastOneWord']);
		elm.find('.searchMode div:eq(2)').find('input').attr('value', 'exactExpression').attr('id', 'exactExpression').parent().find('label').attr('for', 'exactExpression').append('<span>&nbsp;</span>').append(window.i18n.msgStore['exactExpression']);
		
		elm.find('.searchBar input[type=text]').keypress(function(event) {
				if (event.keyCode === 13) {
					if(self.autocomplete){
						try{
						self.elm.find('.searchBar input[type=text]').autocomplete("close");
						} catch (e){
							
						}
						}
					self.makeNewRequest();
				}
		}).keyup(function(){
			if(self.removeContentButton){
				if(AjaxFranceLabs.empty(elm.find('.searchBar input[type=text]').val()))
					elm.find('.searchBar .removeContent:visible').css('display', 'none');
				else
					elm.find('.searchBar .removeContent:hidden').css('display', 'block');
			}
		});
		elm.find('.searchBar input[type=button]').click(function() {
			self.makeNewRequest();
		});
		if (this.autocomplete === true) {
			this.autocompleteOptions.elm = elm.find('.searchBar input[type=text]');
			this.autocomplete = new AjaxFranceLabs.AutocompleteModule(this.autocompleteOptions);
			this.autocomplete.manager = this.manager;
			this.autocomplete.init();
		}
		else if(this.autocomplete){
			this.autocomplete.elm = elm.find('.searchBar input[type=text]');
			this.autocomplete.manager = this.manager;
			this.autocomplete.init();
		}
	},
	
	makeNewRequest : function(){

		this.clean();
		this.manager.generateAndSetQueryID();
		this.manager.makeRequest();
	},

	beforeRequest : function() {
		var search = (AjaxFranceLabs.empty(AjaxFranceLabs.trim($(this.elm).find('.searchBar input').val()))) ? '*:*' : AjaxFranceLabs.trim($(this.elm).find('.searchBar input').val());
		if (this.autocomplete)
			search = search.replace(/\u200c/g, '');
		switch($(this.elm).find('input[name=searchType]:checked').val()){
			case "allWords":
            	Manager.store.addByValue("q.op", 'AND');
                break;
            case "atLeastOneWord":
            	Manager.store.addByValue("q.op", 'OR');
                break;
			case 'exactExpression':	
				search = '"'+search+'"';
				break;
            default:
            	Manager.store.addByValue("q.op", 'AND');
                break
		}
		this.manager.store.get('q').val(search);
	},
	
	afterRequest : function(){
		
		this.updateAddressBar();
		/*
		 * It appeared that sometimes when making a request, the autocomplete was displayed,
		 * this part of the code has been made to counter this little glitch, after a request,
		 * we close the autocomplete
		 */
		if(this.autocomplete){
			try{
				this.elm.find('.searchBar input[type=text]').autocomplete("close");
			} catch(e){
			}
		}
	}
	
});
