//# sourceURL=/Datafari/resources/js/admin/ajax/config_zookeeper.js

$(document).ready(function() {

  // Internationalize content
  $("#topbar1").text(window.i18n.msgStore['home']);
  $("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineConfig']);
  $("#topbar3").text(window.i18n.msgStore['adminUI-Zookeeper']);
  $("#downZKconf").text(window.i18n.msgStore['downZKconf']);
  $("#downZKconfLabel").text(window.i18n.msgStore['downZKconfLabel']);
  $("#download").text(window.i18n.msgStore['downloadButton']);
  $("#download").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['downloadButton']);
  $("#documentation-configzookeeper").text(window.i18n.msgStore['documentation-configzookeeper']);
  $("#upldZKconf").text(window.i18n.msgStore['upldZKconf']);
  $("#upldZKconfLabel").text(window.i18n.msgStore['upldZKconfLabel']);
  $("#upload").text(window.i18n.msgStore['uploadButton']);
  $("#upload").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['uploadButton']);

  $("#rldZKconf").text(window.i18n.msgStore['rldZKconf']);
  $("#rldZKconfLabel").text(window.i18n.msgStore['rldZKconfLabel']);
  $("#reload").text(window.i18n.msgStore['reloadButton']);
  $("#reload").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['reloadButton']);

  $("#download").click(function(e) {
    e.preventDefault();
    // Disable the button until the called servlet responds
    $("#download").loading("loading");
    $.get("../SearchExpert/zookeeperConf?action=download", function(data) {
      // Re-enable the button
      $("#download").loading("reset");
      if (data.code == 0) {
        $("#downloadResult").text(window.i18n.msgStore['zkDwnSuccess']);
        $("#downloadResult").addClass("success");
        $("#downloadResult").show();
        $("#downloadResult").fadeOut(3000);
      } else {
        $("#downloadResult").text(window.i18n.msgStore['zkDwnFail']);
        $("#downloadResult").addClass("error");
        $("#downloadResult").show();
      }
    });
  });

  $("#upload").click(function(e) {
    e.preventDefault();
    // Disable the button until the called servlet responds
    $("#upload").loading("loading");
    $.get("../SearchExpert/zookeeperConf?action=upload", function(data) {
      // Re-enable the button
      $("#upload").loading("reset");
      if (data.code == 0) {
        $("#uploadResult").text(window.i18n.msgStore['zkUplSuccess']);
        $("#uploadResult").addClass("success");
        $("#uploadResult").show();
        $("#uploadResult").fadeOut(3000);
      } else {
        $("#uploadResult").text(window.i18n.msgStore['zkUplFail']);
        $("#uploadResult").addClass("error");
        $("#uploadResult").show();
      }
    });
  });

  $("#reload").click(function(e) {
    e.preventDefault();
    // Disable the button until the called servlet responds
    $("#reload").loading("loading");
    $.get("../SearchExpert/zookeeperConf?action=reload", function(data) {
      // Re-enable the button
      $("#reload").loading("reset");
      if (data.code == 0) {
        $("#reloadResult").text(window.i18n.msgStore['zkRldSuccess']);
        $("#reloadResult").addClass("success");
        $("#reloadResult").show();
        $("#reloadResult").fadeOut(3000);
      } else {
        $("#reloadResult").text(window.i18n.msgStore['zkRldFail']);
        $("#reloadResult").addClass("error");
        $("#reloadResult").show();
      }
    });
  });
});