var roles = [];
var timeouts = [];

var clearTimeouts = function() {
	for (var i = 0; i < timeouts.length; i++) {
	    clearTimeout(timeouts[i]);
	}
	
	//quick reset of the timer array you just cleared
	timeouts = [];
	
	$('#main').unbind('DOMNodeRemoved');
}

$(document).ready(function(){
	$('#main').bind('DOMNodeRemoved', clearTimeouts);
	var PROBLEMCONNECTIONAD = -6;
	var ADUSERNOTEXISTS = -900;
	var NOFAVORITESFOUND = 101;
	var SERVERALREADYPERFORMED = 1;
	var SERVERALLOK = 0;
	var SERVERGENERALERROR = -1;
	var SERVERNOTCONNECTED = -2;
	var SERVERPROBLEMCONNECTIONDB = -3;
	var PROBLEMECONNECTIONSERVER = -404;
	var USERALREADYINBASE = -403;
	var CONFIRMPASSWORDNOTCORRECT = -69;
	var USERALREADYINBASE = -800;
	var FIELDNOTFILLED = -77;
	var admin_messageDiv = $("#Message");
	var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
	var listRoles = ["SearchAdministrator","SearchExpert","ConnectedSearchUser"];
	
	var username;
	var password;
	var bLDAPUser = false;
	var error=[];
	var sourceError;
	timeouts.push(setTimeout(function(){$('input[type="text"],input[type="password"]').val("");},200));
	
	setupLanguage();
	
	$("#ldapUserLabel").click(function() {
		if($("#ldapUserLabel input").is(':checked')) {
			bLDAPUser = true;
			$("#password").hide();
			$("#confirmPassword").hide();
			$("#password").val('');
			$("#confirmPassword").val('');
		} else {
			bLDAPUser = false;
			$("#password").show();
			$("#confirmPassword").show();
		}
	});
 	
 	function setupLanguage(){
 		$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
 		$("#topbar1").text(window.i18n.msgStore['home']);
 		$("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
 	 	$("#topbar3").text(window.i18n.msgStore['adminUI-AddUser']);
 	}
 	
 	
	function htmlRole(role){
		return '<div class="inline_block"><div class="input-group role">'+
		  '<span class="input-group-addon delete"><i class="fa fa-times"></i></span>'+
		  '<input class="form-control '+role+'" value="'+role+'" type="text" disabled/>'+
		  '</div></div>';
	}
	function showError(code,source){
			var message;
			var danger = true;
			var hide = true;
			switch(code){
				case NOFAVORITESFOUND:
					danger = false;
					message = window.i18n.msgStore["NOFAVORITESFOUND"];
					break;
				case SERVERNOTCONNECTED:	
					message = window.i18n.msgStore["SERVERNOTCONNECTED"];
					break;
				case SERVERPROBLEMCONNECTIONDB:
					message = window.i18n.msgStore["SERVERPROBLEMCONNECTIONDB"];
					break;
				case PROBLEMECONNECTIONSERVER:
					message = window.i18n.msgStore["PROBLEMECONNECTIONSERVER"];
					break;
				case CONFIRMPASSWORDNOTCORRECT:
					hide = false;
					message = "Password isn't the same";
					break;
				case USERALREADYINBASE:
					hide = false;
					message = "username already used";
					break;
				case FIELDNOTFILLED : 
					hide = false;
					message = "Fields not all filled";
					break;
				case PROBLEMCONNECTIONAD: 
					hide = false;
					message = window.i18n.msgStore["PROBLEMCONNECTIONAD"];
					break;
				case ADUSERNOTEXISTS:
					hide = false;
					message = "username does not exist in the Active Directory";
					break;
				default :
					message = window.i18n.msgStore["SERVERGENERALERROR"];
					break;
			}
			error[source]={};
			error[source].isError=true;
			error[source].message = message;
			if (hide){
				$("form").hide();
			}
			sourceError = source;
			admin_messageDiv.text(message).show();
			if (danger){
					admin_messageDiv.addClass("danger").prepend('<i class="fa fa-exclamation-triangle"></i> ');
			}else{
				admin_messageDiv.removeClass("danger");
			}
	}
	
	$(".input").blur(function(e){
		var element = $(e.target);
		var attribute = element.attr("name");
		
		switch(attribute){
			case "username":
				if (element.val()!=""){
					$.post("../SearchAdministrator/isUserInBase",{username:element.val()},function(data){
						if (data.code == 0){
							element.next().hide();
							if (data.statut == "true"){
								element.next().show();
								showError(USERALREADYINBASE,attribute);
							}else{
								error["username"]={};
								error["username"].isError = false;
								username = element.val();
								if (sourceError == attribute){
									admin_messageDiv.hide();
								}
							}
							
						}else{
							element.next().show();
							showError(data.code,"username");
						}
					},"json");
					username = element.val();
				}
				break;
			case "password":
				if (element.val()!=""){
					element.next().hide();
					error["password"]={};
					error["password"].isError = false;
					if (sourceError == attribute){
						admin_messageDiv.hide();
					}
					var confirmPassword;
					if ((confirmPassword = $('input[name="confirmPassword"]').val()) !="" && confirmPassword!=element.val()){
						$('input[name="confirmPassword"]').next().show();
						showError(CONFIRMPASSWORDNOTCORRECT,"confirmPassword");
					}else{
						$('input[name="confirmPassword"]').next().hide();
						error["confirmPassword"]={};
						error["confirmPassword"].isError = false;
						password = element.val();
						if (sourceError == "confirmPassword"){
							admin_messageDiv.hide();
						}
					}
					password = element.val();
				}
				break;
			case "confirmPassword":
				if(element.val()!=""){
					var passwordtmp;
					if ( ( passwordtmp=$('input[name="password"]').val())!= element.val()){
						element.next().show();
						showError(CONFIRMPASSWORDNOTCORRECT,"confirmPassword");
					}else{
						if (sourceError == attribute){
							admin_messageDiv.hide();
						}
						element.next().hide();
						error["confirmPassword"]={};
						error["confirmPassword"].isError = false;
						password = passwordtmp;
					}
				}
				break;
			case "role":
				element.val("");
				break;
			default:
				break;	
		}
	});
	
	$('form').submit(function(e){
		e.preventDefault();
		if (username==null || username=="" || (password==null && !bLDAPUser) || (password=="" && !bLDAPUser) || roles==null || roles.length==0){
			showError(FIELDNOTFILLED,"all");
			return false;
		}
		for (var index in error){
			if (error[index].isError){
				admin_messageDiv.text(error[index].message).addClass("danger").prepend('<i class="fa fa-exclamation-triangle"></i>').show();
				return false;
			}
		}
		
		
		$.post("../SearchAdministrator/addUser",{
			username : username,
			password : password,
			ldap : bLDAPUser,
			'role[]'    : roles
		},function(data){
			if (data.code == SERVERALLOK){
				$("#MessageSuccess").show().removeClass("animated fadeOut");
				timeouts.push(setTimeout(function(){
					$("#MessageSuccess").addClass("animated fadeOut");
					$('input[type="text"],input[type="password"]').val("");
					$("#roles").empty();
					roles = [];
					username = null;
					password = null;
					
				},1500));		
			}else{
				showError(data.code,"all");
			}
		},"json");
		return false;
	});
	
	$('.input_roles').autocomplete({
		source : listRoles,
		select : function(event,ui){
			var role = ui.item.value;
			timeouts.push(setTimeout(function(){
				$('.input_roles').val("");
			},500));
			console.log(role);
			console.log(roles.indexOf(role));
			if (roles.indexOf(role)==-1){
				var html = $(htmlRole(role));
				html.find(".delete").click(function(e){
					var element = $(e.target);
					while(!element.hasClass("inline_block")){
						element = element.parent();
					}
					element.remove();
					var index = roles.indexOf(role);
					if (index > -1) {
					    roles.splice(index, 1);
					}
				});
				$("#roles").append(html);
				roles.push(role);
			}
		}
	});
});