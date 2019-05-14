AjaxFranceLabs.PreviewDisplayer = AjaxFranceLabs.Class.extend({

  init : function(request) {
    var self = this;
    var isMobile = $(window).width() < 800;
    if (!isMobile) {
      $(".doc-details").each(function() {
        var docId = $(this).parents(".doc").attr("id");
        if (docId.startsWith("http")) {
          docId = encodeURIComponent(docId);
        }
        var docPos = $(this).parents(".doc").attr("pos");
        var link = "Preview?docPos=" + docPos + "&" + request + "&lang=" + window.i18n.language;
        $(this).prepend("<a class='preview-link' target='_blank' href='" + link + "' title='" + window.i18n.msgStore['preview-link'] + "'><i class='fa fa-eye'></i><a></div>");
      });
    }
  }
})