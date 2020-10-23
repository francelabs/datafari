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

AjaxFranceLabs.LanguageSelectorWidget = AjaxFranceLabs.AbstractWidget.extend({

  // Variables

  type : 'languageSelector',

  languages : [ 'en' ],

  // Methods

  /**
   * Implement the interface method buildWidget, called by init method of AbstractWidget
   */
  buildWidget : function() {
    $(this.elm).addClass('languageSelectorWidget widget nav-item dropdown');

    // Build the selector language HTML elements
    $(this.elm).append(
        '<a ' + 'id="dropdown-lang" class="nav-link dropdown-toggle" ' + 'data-target="#" ' + 'href="#" ' + 'role="button" ' + 'id="dropdownMenuLink" ' + 'data-toggle="dropdown" '
            + 'aria-haspopup="true" ' + 'aria-expanded="false">' + '<i class="fas fa-globe-europe"></i>' + '</a>'
            + '<div id="dropdown-lang-menu" class="dropdown-menu" aria-labelledby="dropdown-lang"></div>');

    // Using proxy to bind this into the each callback function, else this is mapped to the current item being processed.
    $.each(this.languages, $.proxy(function(index, value) {
      $('<a class="dropdown-item" href="#" id="lang-' + value + '">' + window.i18n.msgStore[value + '_locale'] + '</a>').appendTo($("#dropdown-lang-menu")).click($.proxy(function() {
        this.applyLang(value);
      }, this));
    }, this));

    this.applyLang = function(selectedLang) {
      $.post('./applyLang', {
        "lang" : selectedLang
      }, function() {
        // Function executed every time the user changes the language of
        // Datafari
        window.i18n.userSelected(selectedLang);
      }, "json");
    }
  }
});
