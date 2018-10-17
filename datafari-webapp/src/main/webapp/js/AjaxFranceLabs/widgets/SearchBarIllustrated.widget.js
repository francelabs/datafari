AjaxFranceLabs.SearchBarIllustratedWidget = AjaxFranceLabs.SearchBarWidget.extend({


	buildWidget : function() {
		var self = this, elm = $(this.elm);
		if ($(window).width()>800){
			elm.addClass('searchBarWidget').addClass('widget').append(
					'<div id="searchBarContent"></div>')
					.append('<div id="sortMode"></div>');
			elm.find('#searchBarContent').append('<div class="searchBar"></div>').append('<div class="searchMode"></div>');
		}else{
			elm.addClass('searchBarWidget').addClass('widget').append('<div class="searchBar"></div>').append('<div class="searchMode"></div>').append('<div id="sortMode"></div>');
		}
		elm.find('.searchBar').append('<input type="text" autocorrect="off" autocapitalize="off"/>').append(
				'<div class="search bc-color"><i class="fa fa-search"></i></div>');
		if (this.removeContentButton)
			elm.find('.searchBar').append('<span class="removeContent"></span>')
					.find('.removeContent').css('display', 'none').append(
							'<span>X</span>').click(function() {
						elm.find('.searchBar input[type=text]').val('');
						$(this).css('display', 'none');
						self.makeRequest();
					});

		elm.find('.searchMode').append('<div></div>').append('<div></div>').append('<div></div>')
				.append('<div></div>');
		elm.find('.searchMode div').attr('style', 'display: inline').append(
				'<input type="radio" name="searchType" class="radio" />')
				.append('<label></label>');
		elm.find('.searchMode div:eq(0)').find('input').attr('value',
				'allWords').attr('checked', 'true').attr('id', 'allWords')
				.parent().find('label').attr('for', 'allWords').append(
						'<span>&nbsp;</span>').append(
						window.i18n.msgStore['allWords']);
		elm.find('.searchMode div:eq(1)').find('input').attr('value',
				'atLeastOneWord').attr('id', 'atLeastOneWord').parent().find(
				'label').attr('for', 'atLeastOneWord').append(
				'<span>&nbsp;</span>').append(
				window.i18n.msgStore['atLeastOneWord']);
		elm.find('.searchMode div:eq(2)').find('input').attr('value',
				'exactExpression').attr('id', 'exactExpression').parent().find(
				'label').attr('for', 'exactExpression').append(
				'<span>&nbsp;</span>').append(
				window.i18n.msgStore['exactExpression']);

		elm.find('.searchBar input[type=text]').keypress(
				function(event) {
					if (event.keyCode === 13) {
						if (self.autocomplete)
							self.elm.find('.searchBar input[type=text]')
									.autocomplete("close");
						self.makeRequest();
					}
				}).keyup(
				function() {
					if (self.removeContentButton) {
						if (AjaxFranceLabs.empty(elm.find(
								'.searchBar input[type=text]').val()))
							elm.find('.searchBar .removeContent:visible').css(
									'display', 'none');
						else
							elm.find('.searchBar .removeContent:hidden').css(
									'display', 'block');
					}
				});
		elm.find('.searchBar .search').click(function() {
			self.makeRequest();

		});
		if (this.autocomplete === true) {
			this.autocompleteOptions.elm = elm
					.find('.searchBar input[type=text]');
			this.autocomplete = new AjaxFranceLabs.AutocompleteIllustratedModule(
					this.autocompleteOptions);
			this.autocomplete.manager = this.manager;
			this.autocomplete.init();
		} else if (this.autocomplete) {
			this.autocomplete.elm = elm.find('.searchBar input[type=text]');
			this.autocomplete.manager = this.manager;
			this.autocomplete.init();
		}
		var sortModeDiv = document.getElementById("sortMode");
		var textSort = document
				.createTextNode(window.i18n.msgStore['sortType']);
		sortModeDiv.appendChild(textSort);
		var selectList = document.createElement("select");
		selectList.id = "mySelect";
		selectList.onchange = function() {
			self.makeRequest();
		};
		sortModeDiv.appendChild(selectList);

		var optionScore = document.createElement("option");
		optionScore.setAttribute("value", "score");
		optionScore.setAttribute("selected", "");
		optionScore.text = window.i18n.msgStore['score'];

		selectList.appendChild(optionScore);

		var optionDate = document.createElement("option");
		optionDate.setAttribute("value", "date");
		optionDate.text = window.i18n.msgStore['date'];
		selectList.appendChild(optionDate);

	}
});
