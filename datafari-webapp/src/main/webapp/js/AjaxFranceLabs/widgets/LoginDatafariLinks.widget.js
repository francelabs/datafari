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

    // Any cases
    var loginDatafariLinksDOMElement = $(this.elm).find('a#advancedSearchLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['advancedSearchLink'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#advancedsearch");
    }

    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#userPreferencesLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['userPreferencesLink'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#lang");
    }

    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#userSavedSearchLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['savedsearch'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#savedsearch");
    }

    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#userAlertsLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['alerts'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#alert");
    }

    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#userFavoritesLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['favorites'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#favorites");
    }

    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#adminConsoleLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['adminUiLink'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
    }

    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#adminMCFLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['adminConnectorsFrameworkLink'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop('href', '/datafari-mcf-crawler-ui/');
      loginDatafariLinksDOMElement.prop('target', 'blank');
    }

    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#adminGoldenQueriesLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['adminGoldenQueriesLink'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
    }

    /* Those will need a rework of the admin UI to work
    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#adminDocBoostLink');
    if (loginDatafariLinksDOMElement.length > 0){
      loginDatafariLinksDOMElement.html(window.i18n.msgStore['adminUiLink']);
      loginDatafariLinksDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
    }

    // If user is already connected
    var loginDatafariLinksDOMElement = $(this.elm).find('a#adminAnalyticsLink');
    if (loginDatafariLinksDOMElement.length > 0){
      loginDatafariLinksDOMElement.html(window.i18n.msgStore['adminUiLink']);
      loginDatafariLinksDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
    }
    //*/

    // If user is already connected
    loginDatafariLinksDOMElement = $(this.elm).find('a#logout');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['logoutAdminUiLink'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
    }

    // If user is not yet connected
    loginDatafariLinksDOMElement = $(this.elm).find('a#loginLink');
    if (loginDatafariLinksDOMElement.length > 0){
      let text = window.i18n.msgStore['loginAdminUiLink'];
      if (text) {
        loginDatafariLinksDOMElement.html(text);
      }
      loginDatafariLinksDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
    }
  }
});
