$(document).ready(function() {

	$('backupDir-label').text(window.i18n.msgStore["adminUI-MCFBackupRestore-backupDir-label"]);
	
	// Set the breadcrumbs
	document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-Connectors'];
	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-Connectors-BackupRestore'];
	
	// Set the i18n for page elements
	document.getElementById("box-title").innerHTML = window.i18n.msgStore['adminUI-Connectors-BackupRestore'];
	
	document.getElementById("backupDir-label").innerHTML = window.i18n.msgStore['adminUI-MCFBackupRestore-backupDir-label'];
	document.getElementById("backupDir-label-default").innerHTML = window.i18n.msgStore['adminUI-MCFBackupRestore-backupDir-label-default'];
	document.getElementById("doSave-btn").innerHTML = window.i18n.msgStore['adminUI-MCFBackupRestore-doSave-btn'];
	document.getElementById("doRestore-btn").innerHTML = window.i18n.msgStore['adminUI-MCFBackupRestore-doRestore-btn'];
});

function doSave() {
	
	// Reset message
	$("doRestoreReturnStatus-label").html('');
	
	var data = "action=save";
	
	if ($('#backupDir-input').val()) {
		data = data + "&backup_dir=" + $('#backupDir-input').val();
	} 
	
	$.ajax({ //Ajax request to the doPost of the MCF Backup Restore servlet
		type : "POST",
		url : "./../admin/MCFBackupRestore",
		data : data,
		//if received a response from the server
		success : function(data, textStatus, jqXHR) {
			if(data.toString().indexOf("Error code : ")!==-1){
				$("doRestoreReturnStatus-label").html(window.i18n.msgStore["adminUI-MCFBackupRestore-saveError"]).addClass('backupRestoreError');
				
			} else {
				$("doRestoreReturnStatus-label").html(window.i18n.msgStore["adminUI-MCFBackupRestore-saveOK"]).addClass('backupRestoreOK');
			}			
		},
		error : function(jqXHR, textStatus, errorThrown) {
			console.log("Something really bad happened " + textStatus);
			$("#doSaveReturnStatus-label").html(jqXHR.responseText).addClass('backupRestoreError');
		},
		//capture the request before it was sent to server
		beforeSend : function(jqXHR, settings) {
			//disable the buttons until we get the response
			$('#doRestore-btn').prop("disabled", true);
			$('#doSave-btn').prop("disabled", true);
		},
		//this is called after the response or error functions are finished
		complete : function(jqXHR, textStatus) {
			//enable the buttons
			$('#doRestore-btn').prop("disabled", false);
			$('#doSave-btn').prop("disabled", false);
		}
	});
}

function doRestore() {
	
	//Reset message
	$("doRestoreReturnStatus-label").html('');
	
	var data = "action=restore";
	
	if ($('#backupDir-input').val()) {
		data = data + "&backup_dir=" + $('#backupDir-input').val();
	} 
	
	$.ajax({ //Ajax request to the doPost of the MCF Backup Restore servlet
		type : "POST",
		url : "./../admin/MCFBackupRestore",
		data : data,
		//if received a response from the server
		success : function(data, textStatus, jqXHR) {
			if(data.toString().indexOf("Error code : ")!==-1){
				$("doRestoreReturnStatus-label").html(window.i18n.msgStore["adminUI-MCFBackupRestore-RestoreError"]).addClass('backupRestoreError');
			} else {
				$("doRestoreReturnStatus-label").html(window.i18n.msgStore["adminUI-MCFBackupRestore-restoreOK"]).addClass('backupRestoreOK');
			}			
		},
		error : function(jqXHR, textStatus, errorThrown) {
			console.log("Something really bad happened " + textStatus);
			$("#doRestoreReturnStatus-label").html(jqXHR.responseText).addClass('backupRestoreError');
		},
		//capture the request before it was sent to server
		beforeSend : function(jqXHR, settings) {
			//disable the buttons until we get the response
			$('#doRestore-btn').prop("disabled", true);
			$('#doSave-btn').prop("disabled", true);
		},
		//this is called after the response or error functions are finished
		complete : function(jqXHR, textStatus) {
			//enable the buttons 
			$('#doRestore-btn').prop("disabled", false);
			$('#doSave-btn').prop("disabled", false);
		}
	});
}

// Pragma needed to be able to debug JS in the browser
//# sourceURL=../ajax/js/mcfBackupRestore.js