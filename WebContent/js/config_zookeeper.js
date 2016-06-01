$(document).ready(function() {
	
	//Internationalize content
	$("#topbar1").text(window.i18n.msgStore['home']);
	$("#topbar2").text(window.i18n.msgStore['adminUI-SearchEngineConfig']);
	$("#topbar3").text(window.i18n.msgStore['adminUI-Zookeeper']);
	$("#downZKconf").text(window.i18n.msgStore['downZKconf']);
	$("#downZKconfLabel").text(window.i18n.msgStore['downZKconfLabel']);
	$("#download").text(window.i18n.msgStore['downloadButton']);
	
	$("#upldZKconf").text(window.i18n.msgStore['upldZKconf']);
	$("#upldZKconfLabel").text(window.i18n.msgStore['upldZKconfLabel']);
	$("#upload").text(window.i18n.msgStore['uploadButton']);
	
	$("#rldZKconf").text(window.i18n.msgStore['rldZKconf']);
	$("#rldZKconfLabel").text(window.i18n.msgStore['rldZKconfLabel']);
	$("#reload").text(window.i18n.msgStore['reloadButton']);
	
	$("#download").click(function(e){
		e.preventDefault();
		$.get("../SearchAdministrator/zookeeperConf?action=download",function(data){
			if(data.code == 200) {
				$("#downloadResult").text(window.i18n.msgStore['zkDwnSuccess']);
			} else {
				$("#downloadResult").text(window.i18n.msgStore['zkDwnFail']);
			}
		});
	});
	
	$("#upload").click(function(e){
		e.preventDefault();
		$.get("../SearchAdministrator/zookeeperConf?action=upload",function(data){
			if(data.code == 200) {
				$("#uploadResult").text(window.i18n.msgStore['zkUplSuccess']);
			} else {
				$("#uploadResult").text(window.i18n.msgStore['zkUplFail']);
			}
		});
	});
	
	$("#reload").click(function(e){
		e.preventDefault();
		$.get("../SearchAdministrator/zookeeperConf?action=reload",function(data){
			if(data.code == 200) {
				$("#reloadResult").text(window.i18n.msgStore['zkRldSuccess']);
			} else {
				$("#reloadResult").text(window.i18n.msgStore['zkRldFail']);
			}
		});
	});
});