$(function($) {

  window.i18n.setLanguageUrl('/Datafari/resources/js/AjaxFranceLabs/locale/');
  window.i18n.setCustomLanguageUrl('/Datafari/resources/customs/i18n/');

  window.i18n.setupLanguage('Datafari admin home page');

  // Set the language parameter for the admin home page link of the sub-pages
  // breadcrumb
  $(this).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
});