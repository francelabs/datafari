/*******************************************************************************************************************************************
 * Copyright 2019 France Labs
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
AjaxFranceLabs.HeaderMenusWidget = AjaxFranceLabs.AbstractWidget.extend({
    // Variables

    type : 'HeaderMenus',

    // Methods

    /**
     * Implement the interface method buildWidget, called by init method of AbstractWidget
     */
    buildWidget : function() {
        var caret = ' <span class="caret">';
        var mainMenuDOMElement = $(this.elm).find("#loginDatafariLinks");
        var searchToolsSubMenuDOMElement = $(this.elm).find('#search-tools-sub-menu');
        var adminToolsSubMenuDOMElement = $(this.elm).find('#admin-sub-menu-item');
        var accountToolsSubMenuDOMElement = $(this.elm).find('#account-sub-menu-item');


        // Any cases
        var linkDOMElement = searchToolsSubMenuDOMElement.find('a#advancedSearchLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['advancedSearchLink'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#advancedsearch");
        }

        // If user is already connected
        linkDOMElement = mainMenuDOMElement.find('a#dropdown-search-tools');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['searchToolsLink'] + caret;
            if (text) {
                linkDOMElement.html(text);
            }
        }

        // If user is already connected
        linkDOMElement = searchToolsSubMenuDOMElement.find('a#userSavedSearchLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['savedsearch'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#savedsearch");
        }

        // If user is already connected
        linkDOMElement = searchToolsSubMenuDOMElement.find('a#exportResultsLink');
         if (linkDOMElement.length > 0) {
             let text = window.i18n.msgStore['exportResults-label'];
             if (text) {
                 linkDOMElement.html(text);
             }
         }

        // If user is already connected
        linkDOMElement = searchToolsSubMenuDOMElement.find('a#userAlertsLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['alerts'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#alert");
        }

        // If user is already connected
        linkDOMElement = searchToolsSubMenuDOMElement.find('a#save_search_label');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['save_search_button'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#save_search_button");
        }

        // If user is already connected
        linkDOMElement = searchToolsSubMenuDOMElement.find('a#userFavoritesLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['favorites'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#favorites");
        }

        // If user is already connected
        linkDOMElement = mainMenuDOMElement.find('a#admin-sub-menu-item');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['adminUI-Admin'] + caret;
            if (text) {
                linkDOMElement.html(text);
            }
        }

        // If user is already connected
        linkDOMElement = adminToolsSubMenuDOMElement.find('a#adminConsoleLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['adminUI-Admin-Main'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
        }

        // If user is already connected.
        linkDOMElement = adminToolsSubMenuDOMElement.find('a#adminMCFLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['adminUI-Connectors-Admin'];
            if (text) {
                linkDOMElement.html(text);
            }

            var getUrl = window.location;
            var mcfUrl = getUrl.protocol + "//" + getUrl.hostname + ":9080" + "/" + "datafari-mcf-crawler-ui/";
            linkDOMElement.prop('href', mcfUrl);
            linkDOMElement.prop('target', 'blank');
        }

        // If user is already connected
        linkDOMElement = adminToolsSubMenuDOMElement.find('a#create-relevancy');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['relevancy_button'];
            if (text) {
                linkDOMElement.html(text);
            }
        }


        // If user is already connected
        linkDOMElement = mainMenuDOMElement.find('a#dropdown-my-account');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['adminUI-MyAccount'] + caret;
            if (text) {
                linkDOMElement.html(text);
            }
        }

        // If user is already connected
        linkDOMElement = accountToolsSubMenuDOMElement.find('a#externalSourcesLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['external-sources-label'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop("href", "/Datafari/Search?lang=" + window.i18n.language + "#externalSources");
        }
        
        // If user is already connected
        linkDOMElement = mainMenuDOMElement.find('a#adminGoldenQueriesLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['adminGoldenQueriesLink'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language);
        }

        // If user is already connected
        linkDOMElement = mainMenuDOMElement.find('a#logout');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['logoutAdminUiLink'];
            if (text) {
                linkDOMElement.html(text);
            }
        }

        // If user is not yet connected
        linkDOMElement = mainMenuDOMElement.find('a#loginLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['loginAdminUiLink'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop('href', '/Datafari/admin/?lang=' + window.i18n.language + "&redirect=" + window.location.href);
        }

        // Help link
        linkDOMElement = mainMenuDOMElement.find('a#helpLink');
        if (linkDOMElement.length > 0) {
            let text = window.i18n.msgStore['helpLink'];
            if (text) {
                linkDOMElement.html(text);
            }
            linkDOMElement.prop('href', '/Datafari/help?lang=' + window.i18n.language);
        }
    }
});
