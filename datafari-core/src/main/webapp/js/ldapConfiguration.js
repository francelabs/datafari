$(document).ready(function() {
	var SERVERALLOK = 0;
	var SERVERGENERALERROR = -1;
	var PROBLEMSERVERADCONNECTION = -6;
	var listRoles = ["ConnectedSearchUser","SearchAdministrator","SearchExpert"];
	var cpt = null;
	var countdownID = null;
	
	//Internationalize content
	$("#topbar1").text(window.i18n.msgStore['home']);
	$("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
	$("#topbar3").text(window.i18n.msgStore['adminUI-ADConfig']);
	$("#submit").text(window.i18n.msgStore['save']);
	$("#title").text(window.i18n.msgStore['ADConfig']);
	$("#disclaimer-enterprise").html(window.i18n.msgStore['disclaimer-enterprise']);
	$("#adActivationLabel").html(window.i18n.msgStore['adActivationLabel']);
	$("#connectionURLLabel").html(window.i18n.msgStore['adURLLabel']);
	$("#connectionNameLabel").html(window.i18n.msgStore['adUsernameLabel']);
	$("#connectionPasswordLabel").html(window.i18n.msgStore['adPasswordLabel']);
	$("#tomcat-warning-title").html("<span id='warning-icon'></span> " + window.i18n.msgStore['tomcat-warning-title']);
	var regexConnectionURL = /ldap:\/\/.+\:[0-9]+/;
	var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
	
	function ADStatusOK() {
		$("#ldap_activation").removeClass("error");
		$("#ldap_activation").addClass("success");
		$("#ldap_activation").html("OK");
	}
	
	function ADStatusKO() {
		$("#ldap_activation").removeClass("success");
		$("#ldap_activation").addClass("error");
		$("#ldap_activation").html("KO");
	}
	
	$.get("../SearchAdministrator/isLdapConfig",function(data){
		if(data.code == 0) {
			ADStatusOK();
		} else {
			ADStatusKO();
		}
	},"json");
	
	function inputActivation(data){
		if (data.code == 0 ){
			if (data.isActivated=="true"){
				var bool = true;
				
				$('.assign_role').show();
				
			}else{
				var bool = false;
				
				$('.assign_role').hide();
			}
			input.prop('checked',bool);		
		} else if (data.code == PROBLEMSERVERADCONNECTION){
			$("#message").html('<i class="fa fa-times"></i> AD connection not working ! Please check your configuration')
			.addClass("error").removeClass("success").show();
		}else{
			$("#message").html('<i class="fa fa-times"></i> An error occured, Please try again')
				.addClass("error").removeClass("success").show();
		}
	}
	
	$("#connectionURL").keyup(function(){
		if (!regexConnectionURL.test($("#connectionURL").val())){
			$("#message").html('<i class="fa fa-times"></i> The connectionURL should be like this ldap://IPTOSERVER:PORT ').addClass("error").removeClass("success").show();
		}else{
			$("#message").html("").removeClass("error");
		}
	});
		
	

	$.get("../SearchAdministrator/modifyRealmLdap",function(data){
		if (data.code == 0){
			$("#connectionURL").val(data.connectionURL);
			$("#connectionName").val(data.connectionName);
			$("#connectionPassword").val(data.connectionPassword);
			$("#userBase").val(data.userBase);
			var bUserSubtree = false;
			if(data.userSubtree=="true") {
				bUserSubtree = true;
			}
			$("#userSubTree_activation input").prop('checked',bUserSubtree);
		}else{
			$("#message").html('<i class="fa fa-times"></i> An error occured, Please try again')
				.addClass("error").removeClass("success").show();
			setTimeout(function(){
				$("#message").addClass("animated fadeOut").one(ENDOFANIMATION,function(){
					$("#message").hide().removeClass("animated fadeOut");
				});
			},5000);
		}
	},"json");
	
	function countdown() {
		cpt -= 1;
		$("#tomcat-warning-message").html(window.i18n.msgStore['wait'] + "... " + cpt + "</br>(" + window.i18n.msgStore['countdown-redirect'] + ")");
		if(cpt == 0) {
			clearTimeout(countdownID);
			if (window.i18n.language !== null && window.i18n.language !== undefined){
				window.open("/Datafari/Search?lang="  + window.i18n.language,"_self");
			} else {
				window.open("/Datafari/Search","_self");
			}
		}
	}
	

	$("form").submit(function(e){
		e.preventDefault();
		if ($("#connectionURL").val() !="" && $("#connectionURL").val() != undefined && $("#connectionName").val()!="" && $("#connectionName").val() != undefined &&
				$("#connectionPassword").val()!="" && $("#connectionPassword").val() != undefined && $("#userBase").val()!="" && $("#userBase").val() != undefined){
			var userSubTreeInput = $("#userSubTree_activation input");
			var bUserSubTree  = false;
			if(userSubTreeInput.is(':checked')) {
				bUserSubTree = true;
			}
			$.post("../SearchAdministrator/modifyRealmLdap",{
				connectionURL :  $("#connectionURL").val() ,
				connectionName : $("#connectionName").val() ,
				connectionPassword : $("#connectionPassword").val() ,
				userBase : $("#userBase").val() ,
				userSubtree : bUserSubTree
			},function(data){
				if (data!=undefined && data.code!= undefined){
					if (data.code==0){
						$("#message").html('<i class="fa fa-check"></i> Well Saved').addClass("success").removeClass("error").show();
						ADStatusOK();
						cpt = 20;
						$("#tomcat-warning-message").html(window.i18n.msgStore['wait'] + "... " + cpt + "</br>(" + window.i18n.msgStore['countdown-redirect'] + ")");
						$("#tomcat-restart-overlay").show();
						countdownID = setInterval(countdown, 1000);
					}else if (data.code == PROBLEMSERVERADCONNECTION){
						$("#message").html('<i class="fa fa-times"></i> '+data.status).addClass("error").removeClass("success").show();
						ADStatusKO();
					}else{
						$("#message").html('<i class="fa fa-times"></i> An error occured, Please try again').addClass("error").removeClass("success").show();
						ADStatusKO();
					}
				}else{
					$("#message").html('<i class="fa fa-times"></i> An error occured, Please try again').addClass("error").removeClass("success").show();
					ADStatusKO();
				}
				setTimeout(function(){
					$("#message").addClass("animated fadeOut").one(ENDOFANIMATION,function(){
						$("#message").hide().removeClass("animated fadeOut");
					});
				},2000);
			},"json");
		}else{
			$("#message").html('<i class="fa fa-times"></i> Please fill in all the fields').addClass("error").removeClass("success").show();
			setTimeout(function(){
				$("#message").addClass("animated fadeOut").one(ENDOFANIMATION,function(){
					$("#message").hide().removeClass("animated fadeOut");
				});
			},2000);
		}
		return false;
	});
	
});