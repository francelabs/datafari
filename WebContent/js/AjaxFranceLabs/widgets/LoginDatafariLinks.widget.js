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

AjaxFranceLabs.LoginDatafariLinksWidget = AjaxFranceLabs.AbstractWidget.extend({

	// Variables

	type : 'loginDatafariLinks',

	// Methods

	/**
	 * Implement the interface method buildWidget, called by init method of
	 * AbstractWidget
	 */
	buildWidget : function() {
		
		// If user is already connected
		var loginDatafariLinksDOMElement = $(this.elm).find('a#adminLink');
		if (loginDatafariLinksDOMElement.length > 0){
			loginDatafariLinksDOMElement.append(window.i18n.msgStore['adminUiLink']);
			loginDatafariLinksDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
		}
		
		// If user is already connected
		loginDatafariLinksDOMElement = $(this.elm).find('a#logout');
		if (loginDatafariLinksDOMElement.length > 0){
			loginDatafariLinksDOMElement.append(window.i18n.msgStore['logoutAdminUiLink']);
		}
		
		// If user is not yet connected
		loginDatafariLinksDOMElement = $(this.elm).find('a#loginLink');
		if (loginDatafariLinksDOMElement.length > 0){
			loginDatafariLinksDOMElement.append(window.i18n.msgStore['loginAdminUiLink']);
			loginDatafariLinksDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
		}
	}
});
