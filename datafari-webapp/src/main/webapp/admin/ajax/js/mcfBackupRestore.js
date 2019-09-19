//# sourceURL=/Datafari/admin/ajax/js/mcfBackupRestore.js

$(document).ready(function() {

  $('backupDir-label').text(window.i18n.msgStore["adminUI-MCFBackupRestore-backupDir-label"]);

  // Set the breadcrumbs
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Connectors'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-Connectors-BackupRestore'];

  // Set the i18n for page elements
  document.getElementById("box-title").innerHTML = window.i18n.msgStore['adminUI-Connectors-BackupRestore'];
  document.getElementById("documentation-mcfbackuprestore").innerHTML = window.i18n.msgStore['documentation-mcfbackuprestore'];
  document.getElementById("backupDir-label").innerHTML = window.i18n.msgStore['adminUI-MCFBackupRestore-backupDir-label'];
  document.getElementById("backupDir-label-default").innerHTML = window.i18n.msgStore['adminUI-MCFBackupRestore-backupDir-label-default'];
  document.getElementById("doSave-btn").innerHTML = window.i18n.msgStore['adminUI-MCFBackupRestore-doSave-btn'];
  $("#doSave-btn").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['adminUI-MCFBackupRestore-doSave-btn']);
  document.getElementById("doRestore-btn").innerHTML = window.i18n.msgStore['adminUI-MCFBackupRestore-doRestore-btn'];
  $("#doRestore-btn").attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['adminUI-MCFBackupRestore-doRestore-btn']);
});

function doSave() {

  // Reset message
  $("doRestoreReturnStatus-label").html('');

  var data = "action=save";

  if ($('#backupDir-input').val()) {
    data = data + "&backupDir=" + $('#backupDir-input').val();
  }

  $.ajax({ // Ajax request to the doPost of the MCF Backup Restore servlet
    type : "POST",
    url : "./../admin/MCFBackupRestore",
    data : data,
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      if (data.code !== 0) {
        $("#doRestoreReturnStatus-label").text(data.status).switchClass('backupRestoreOK', 'backupRestoreError', 100);

      } else {
        $("#doRestoreReturnStatus-label").text(window.i18n.msgStore["adminUI-MCFBackupRestore-saveOK"]).switchClass('backupRestoreError', 'backupRestoreOK', 100);
      }
    },
    error : function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#doSaveReturnStatus-label").html(jqXHR.responseText).addClass('backupRestoreError');
    },
    // capture the request before it was sent to server
    beforeSend : function(jqXHR, settings) {
      // disable the buttons until we get the response
      $("#doSave-btn").button("loading");
      $('#doRestore-btn').prop("disabled", true);
      $("#doRestoreReturnStatus-label").text('');
    },
    // this is called after the response or error functions are finished
    complete : function(jqXHR, textStatus) {
      // enable the buttons
      $('#doRestore-btn').prop("disabled", false);
      $("#doSave-btn").button("reset");
    }
  });
}

function doRestore() {

  if (confirm(window.i18n.msgStore['mcf-restore-warning'])) {
    // Reset message
    $("doRestoreReturnStatus-label").html('');

    var data = "action=restore";

    if ($('#backupDir-input').val()) {
      data = data + "&backupDir=" + $('#backupDir-input').val();
    }

    $.ajax({ // Ajax request to the doPost of the MCF Backup Restore servlet
      type : "POST",
      url : "./../admin/MCFBackupRestore",
      data : data,
      // if received a response from the server
      success : function(data, textStatus, jqXHR) {
        if (data.code !== 0) {
          $("#doRestoreReturnStatus-label").text(data.status).switchClass('backupRestoreOK', 'backupRestoreError', 100);
        } else {
          $("#doRestoreReturnStatus-label").text(window.i18n.msgStore["adminUI-MCFBackupRestore-restoreOK"]).switchClass('backupRestoreError', 'backupRestoreOK', 100);
        }
      },
      error : function(jqXHR, textStatus, errorThrown) {
        console.log("Something really bad happened " + textStatus);
        $("#doRestoreReturnStatus-label").html(jqXHR.responseText).addClass('backupRestoreError');
      },
      // capture the request before it was sent to server
      beforeSend : function(jqXHR, settings) {
        // disable the buttons until we get the response
        $("#doRestore-btn").button("loading");
        $('#doSave-btn').prop("disabled", true);
        $("#doRestoreReturnStatus-label").text('');
      },
      // this is called after the response or error functions are finished
      complete : function(jqXHR, textStatus) {
        // enable the buttons
        $("#doRestore-btn").button("reset");
        $('#doSave-btn').prop("disabled", false);
      }
    });

  }
}