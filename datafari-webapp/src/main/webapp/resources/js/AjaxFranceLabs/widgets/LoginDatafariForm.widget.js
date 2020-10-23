/*******************************************************************************************************************************************
 * Copyright 2016 France Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************************************************************************/

AjaxFranceLabs.LoginDatafariFormWidget = AjaxFranceLabs.AbstractWidget.extend({

  // Variables

  type : 'loginDatafariForm',

  // Methods

  /**
   * Implement the interface method buildWidget, called by init method of AbstractWidget
   */
  buildWidget : function() {

    $(this.elm).find('#loginFormAdminUiLabel').append(window.i18n.msgStore['labelFormUi']);
    $(this.elm).find('#loginAdminUiLabel').append(window.i18n.msgStore['loginAdminUi']);
    $(this.elm).find('#passwordAdminUiLabel').append(window.i18n.msgStore['passwordAdminUi']);

    // If user has just mistaken login and/or pw
    loginDatafariErrorDOMElement = $(this.elm).find('#invalidLoginAdminUiLabel');
    if (loginDatafariErrorDOMElement.length > 0) {
      loginDatafariErrorDOMElement.append(window.i18n.msgStore['loginErrorAdminUi']);
    }

    // If user's session has expired
    loginDatafariErrorDOMElement = $(this.elm).find('#expiredSessionLabel');
    if (loginDatafariErrorDOMElement.length > 0) {
      loginDatafariErrorDOMElement.append(window.i18n.msgStore['expiredSessionLabel']);
    }

    // If user has logged out
    loginDatafariErrorDOMElement = $(this.elm).find('#loggedOutLabel');
    if (loginDatafariErrorDOMElement.length > 0) {
      loginDatafariErrorDOMElement.append(window.i18n.msgStore['loggedOutLabel']);
    }

  }
});

// Function executed when the JS is loaded
$(function($) {

  Manager.addWidget(new AjaxFranceLabs.LoginDatafariFormWidget({
    // Take the loginDatafariForm element by ID.
    elm : $('#loginDatafariForm'),
    id : 'loginDatafariForm'
  }));

  Manager.init();
});
