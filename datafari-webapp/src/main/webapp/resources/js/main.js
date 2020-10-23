$(function($) {

  /*
   * Absolute path instead of js/Ajax.. from the root of the context, to be able to be used for: - search.jsp (context in /Datafari) - and
   * login.jsp (context in /Datafari/admin)
   */
  window.i18n.setLanguageUrl('/Datafari/resources/js/AjaxFranceLabs/locale/');
  window.i18n.setCustomLanguageUrl('/Datafari/resources/customs/i18n/');

  window.i18n.setupLanguage('Datafari home page');

  // Force user preferred language if available and not corresponding to the
  // current one
  $.get('/Datafari/applyLang?lang=' + window.i18n.language, function(data) {
    if (data.code == 0 && data.lang != window.i18n.language) {
      window.location.replace("/Datafari/applyLang?urlRedirect=" + encodeURIComponent(window.location.href));
    }
  }, "json");

  var port = '8080';

  if (typeof portHeader !== 'undefined') {
    port = parseInt(portHeader);
  }

  Manager = new AjaxFranceLabs.Manager({
    serverUrl : '/Datafari/SearchAggregator/',
    // serverUrl : '/Datafari/SearchProxy/',
    constellio : false,
    connectionInfo : {
      autocomplete : {
        serverUrl : '',
        servlet : 'suggest',
        queryString : 'action=suggest&q='
      },
      spellcheck : {
        serverUrl : '',
        servlet : '',
        queryString : ''
      }
    }
  });

  var languages = [ 'en' ];
  if (typeof langHeader !== 'undefined') {
    languages = langHeader
  }

  Manager.addWidget(new AjaxFranceLabs.LanguageSelectorWidget({
    // Take the languageSelector element by ID.
    languages : languages,
    elm : $('#languageSelector'),
    id : 'languageSelector'
  }));

  var fieldList = 'title,url,id,extension,preview_content,last_modified,crawl_date,author,original_file_size,emptied';
  for (var i = 0; i < additionalFields.length; i++) {
    fieldList += "," + additionalFields[i];
  }
  Manager.store.addByValue("fl", fieldList);

  // add Konami code
  var k = [ 38, 38, 40, 40, 37, 39, 37, 39, 66, 65 ], n = 0;
  $(document).keydown(function(e) {
    if (e.keyCode === k[n++]) {
      if (n === k.length) {
        alert('Congratulation, you found the konami code! More surprises are coming soon!');
        n = 0;
        return false;
      }
    } else {
      n = 0;
    }
  });

});
