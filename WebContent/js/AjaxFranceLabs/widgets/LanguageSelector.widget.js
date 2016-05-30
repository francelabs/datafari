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
		$(this.elm).addClass('languageSelectorWidget widget');

		// Build the selector language HTML elements
		$(this.elm).append(window.i18n.msgStore['selectLang'])
				.append('<select class="languageSelectorWidget-select"></select>')

				
		$.each(this.languages, function( index, value ) {
			$('<option value="'+value+'">' + window.i18n.msgStore[value+'_locale'] + '</option>').appendTo('.languageSelectorWidget-select');
		});
		
		// Select the language for languageSelectorWidget, based on the window.i18n language detected from the browser/system
		$(this.elm).find('select option[value="'+ window.i18n.language + '"]').prop('selected', true); 

		// Bind the onChange event of the "select" DOM element (language list box).
		// It's an anonymous function: in this way it's not called when the widget initializes.
		$(this.elm).find('select').change(function() {

			// Function executed every time the user changes the language of
			// Datafari		
			window.i18n.userSelected($(this).val());
		});
	}
});
