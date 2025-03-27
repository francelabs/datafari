function setupLinks() {
  var mainMenuDOMElement = $("#header-menus").find("#loginDatafariLinks");
  var searchToolsSubMenuDOMElement = $("#header-menus").find('#search-tools-sub-menu');
  var adminToolsSubMenuDOMElement = $("#header-menus").find('#sub-menu-admin');
  var accountToolsSubMenuDOMElement = $("#header-menus").find('#account-sub-menu-item');

  // Any cases
  var linkDOMElement = searchToolsSubMenuDOMElement.find('a#advancedSearchLink');
  if (linkDOMElement.length > 0) {
    let text = window.i18n.msgStore['advancedSearchLink'];
    if (text) {
      linkDOMElement.html(text);
    }
    linkDOMElement.prop("href", "/@WEBAPPNAME@/Search?lang=" + window.i18n.language + "#advancedsearch");
  }

  // If user is already connected
  linkDOMElement = mainMenuDOMElement.find('a#dropdown-search-tools');
  if (linkDOMElement.length > 0) {
    let text = window.i18n.msgStore['searchToolsLink'];
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
    linkDOMElement.prop("href", "/@WEBAPPNAME@/Search?lang=" + window.i18n.language + "#savedsearch");
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
    linkDOMElement.prop("href", "/@WEBAPPNAME@/Search?lang=" + window.i18n.language + "#alert");
  }

  // If user is already connected
  linkDOMElement = searchToolsSubMenuDOMElement.find('a#save_search_label');
  if (linkDOMElement.length > 0) {
    let text = window.i18n.msgStore['save_search_button'];
    if (text) {
      linkDOMElement.html(text);
    }
    linkDOMElement.prop("href", "/@WEBAPPNAME@/Search?lang=" + window.i18n.language + "#save_search_button");
  }

  // If user is already connected
  linkDOMElement = searchToolsSubMenuDOMElement.find('a#userFavoritesLink');
  if (linkDOMElement.length > 0) {
    let text = window.i18n.msgStore['favorites'];
    if (text) {
      linkDOMElement.html(text);
    }
    linkDOMElement.prop("href", "/@WEBAPPNAME@/Search?lang=" + window.i18n.language + "#favorites");
  }

  // If user is already connected
  linkDOMElement = mainMenuDOMElement.find('a#admin-sub-menu-item');
  if (linkDOMElement.length > 0) {
    let text = window.i18n.msgStore['adminUI-Admin'];
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
    linkDOMElement.prop('href', '/@WEBAPPNAME@/admin/?lang=' + window.i18n.language);
  }

  // If user is already connected.
  linkDOMElement = adminToolsSubMenuDOMElement.find('a#adminMCFLink');
  if (linkDOMElement.length > 0) {
    let text = window.i18n.msgStore['adminUI-Connectors-Admin'];
    if (text) {
      linkDOMElement.html(text);
    }

    var getUrl = window.location;
    var mcfUrl = "@GET-MCF-IP@";
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
    let text = window.i18n.msgStore['adminUI-MyAccount'];
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
    linkDOMElement.prop("href", "/@WEBAPPNAME@/Search?lang=" + window.i18n.language + "#externalSources");
  }

  // If user is already connected
  linkDOMElement = mainMenuDOMElement.find('a#adminGoldenQueriesLink');
  if (linkDOMElement.length > 0) {
    let text = window.i18n.msgStore['adminGoldenQueriesLink'];
    if (text) {
      linkDOMElement.html(text);
    }
    linkDOMElement.prop('href', '/@WEBAPPNAME@/admin/?lang=' + window.i18n.language);
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
    linkDOMElement.prop('href', '/@WEBAPPNAME@/login?lang=' + window.i18n.language + "&redirect=" + encodeURIComponent(window.location.href));
  }

  // Help link
  linkDOMElement = mainMenuDOMElement.find('a#helpLink');
  if (linkDOMElement.length > 0) {
    let text = window.i18n.msgStore['helpLink'];
    if (text) {
      linkDOMElement.html(text);
    }
    linkDOMElement.prop('href', '/@WEBAPPNAME@/help?lang=' + window.i18n.language);
  }
}

$(document).ready(
    function() {

      setupLinks();

      // Disable <a> buttons to trigger page reload
      $("#loginDatafariLinks ul > li > a").click(
          function(event) {
            if ($(this).attr("id") !== "logout" && $(this).attr("id") !== "loginLink" && $(this).attr("id") !== "helpLink" && $(this).attr("id") !== "adminConsoleLink"
                && $(this).attr("id") !== "adminMCFLink") {
              event.preventDefault();
            }
          });

      // Each time a link of a sub-menu is clicked, add the active-header-link css class to it
      $("#loginDatafariLinks > li > ul > li > a").click(function() {
        if ($(this).attr("id") !== "adminConsoleLink" && $(this).attr("id") !== "adminMCFLink") {
          $(".active-header-link").removeClass("active-header-link");
          $(this).addClass("active-header-link");
        }
      });

      // Prevents the glitch where you have to wait for relevancy search to show up
      setTimeout(function() {
        $("#relevancy-div").trigger('click');
      }, 10);

      if ($(window).width() < 769) {
        // Prevents the drag on right/left side while in mobile view
        $('#container-viewport-switch').removeClass('container-fluid');
        $('#container-viewport-switch').addClass('container');

      } else {
        $('#search-tools-sub-menu-mobile').empty();
        $('#admin-sub-menu-mobile').empty();
        $('#account-sub-menu-mobile').empty();
      }

      // On a .dropdown-item click, inside a .header-item, remove .active css class from non active .dropdown-item, unless the clicked
      // .dropdown-item is an elm of the sub-menu-admin
      $(".header-item .dropdown-item").click(function() {
        if ($(this).closest("ul").attr("id") !== "sub-menu-admin") {
          $(".dropdown-toggle").not($(this).closest(".dropdown").find(".dropdown-toggle:first")).removeClass("active");
        }
      });

      $("#basicSearchLink").click(function() {
        $(".dropdown-toggle:not(#basicSearchLink)").removeClass("active");
        $(".active-header-link").removeClass("active-header-link");
      });

      // Keep open dropdown submenus (that belong to the keep-down css class) when clicking inside
      $(document).on('click.bs.dropdown.data-api', '.keep-down', function(e) {
        // e.stopPropagation();
      });

      // Keep open sub-menu holding the save_search_label and create-relevancy buttons when clicking them
      $("#save_search_label, #create-relevancy").click(function(e) {
        e.stopPropagation();
      });

      // Add Listeners to every sub-menu items to hide"Save Search" when they are clicked.
      $('#externalSourcesLink').click(function() {
        $("#save_search_label").hide("fast");
        $("#create-relevancy").hide("fast");
      });
      $("#alertsDropdownMenu").click(function() {
        $("#save_search_label").hide("fast");
        $("#create-relevancy").hide("fast");
      });
      $("#savedSearchDropdownMenu").click(function() {
        $("#save_search_label").hide("fast");
        $("#create-relevancy").hide("fast");
      });
      $("#advancedSearchDropdownMenu").click(function() {
        $("#save_search_label").hide("fast");
        $("#create-relevancy").hide("fast");
      });
      $("#favoritesDropdownMenu").click(function() {
        $("#save_search_label").hide("fast");
        $("#create-relevancy").hide("fast");
      });
      $("#basicSearchLink").click(function() {
        $("#save_search_label").show();
        $("#create-relevancy").show();
      });
    });