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

AjaxFranceLabs.LoginSettingsWidget = AjaxFranceLabs.AbstractWidget.extend({

	// Variables

	type : 'loginSettings',

	// Methods

	/**
	 * Implement the interface method buildWidget, called by init method of
	 * AbstractWidget
	 */
	buildWidget : function() {
		
		// If user is already connected
		var loginSettingsDOMElement = $(this.elm).find('a#adminLink');
		if (loginSettingsDOMElement.length > 0){
			loginSettingsDOMElement.append(window.i18n.msgStore['adminUiLink']);
		}
		
		// If user is already connected
		loginSettingsDOMElement = $(this.elm).find('a#logout');
		if (loginSettingsDOMElement.length > 0){
			loginSettingsDOMElement.append(window.i18n.msgStore['logoutAdminUiLink']);
		}
		
		// If user is not yet connected
		loginSettingsDOMElement = $(this.elm).find('a#loginLink');
		if (loginSettingsDOMElement.length > 0){
			loginSettingsDOMElement.append(window.i18n.msgStore['loginAdminUiLink']);
		}
	}
});
