/*******************************************************************************
 * Copyright 2016 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

AjaxFranceLabs.LanguageSelectorWidget = AjaxFranceLabs.AbstractWidget.extend({

	// Variables

	type : 'languageSelector',
	
	languages : ['en'],
	

	// Methods

	/**
	 * Implement the interface method buildWidget, called by init method of
	 * AbstractWidget
	 */
	buildWidget : function() {
		$(this.elm).addClass('languageSelectorWidget widget dropdown');

		// Build the selector language HTML elements
		$(this.elm).append('<a '+
							'class="dropdown-toggle" '+
							'data-target="#" '+
							'href="#" '+
							'role="button" '+
							'id="dropdownMenuLink" '+
							'data-toggle="dropdown" '+
							'aria-haspopup="true" '+
							'aria-expanded="false">'+
								'<span class="glyphicon glyphicon-globe"></span>'+
							'</a>'+
							'<ul class="dropdown-menu"></ul>');
		
		// Using proxy to bind this into the each callback function, else this is mapped to the current item being processed.
		$.each(this.languages, $.proxy(function( index, value ) {
			$('<li><a href="#" id="lang-'+value+'">' + window.i18n.msgStore[value+'_locale'] + '</a></li>')
				.appendTo($(this.elm).find("ul"))
				.click($.proxy(function() {
					this.applyLang(value);
				}, this));
		}, this));

		this.applyLang = function(selectedLang) {
			$.post('./applyLang',{"lang":selectedLang}, function() {
				// Function executed every time the user changes the language of
				// Datafari		
				window.i18n.userSelected(selectedLang);
			});
		}
	}
});
