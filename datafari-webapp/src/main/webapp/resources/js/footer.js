$(document).ready(function() {

  var backgroundImg = "url('../../images/bg-header-960.png')";

  if (window.i18n.msgStore['privacy-policy']) {
    $("#gdpr-link").html(window.i18n.msgStore['privacy-policy']);
  }

});