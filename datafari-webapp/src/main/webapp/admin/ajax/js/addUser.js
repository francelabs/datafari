
//# sourceURL=/Datafari/admin/ajax/js/addUser.js


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

 	function setupLanguage(){
 		$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
 		$("#topbar1").text(window.i18n.msgStore['home']);
 		$("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
 	 	$("#topbar3").text(window.i18n.msgStore['adminUI-AddUser']);
 	 	$("#title").text(window.i18n.msgStore['adminUI-AddUser']);
 	 	$("#username-label").text(window.i18n.msgStore['username']);
 	 	$("#username-input").attr("placeholder", window.i18n.msgStore['username']);
 	 	$("#username-error-text").text(window.i18n.msgStore['adminUI-UsernameAlreadyUsed']);
 	 	$("#username-warning-text").text(window.i18n.msgStore['adminUI-UsernameInvalid']);
 	 	$("#ldap-user-label").text(window.i18n.msgStore['adminUI-CreateUserFromAD']);
 	 	$("#password-label").text(window.i18n.msgStore['adminUI-Password']);
 	 	$("#password-input").attr("placeholder", window.i18n.msgStore['adminUI-Password']);
 	 	$("#confirm-password-label").text(window.i18n.msgStore['confirmPassword']);
 	 	$("#confirm-password-input").attr("placeholder", window.i18n.msgStore['adminUI-Password']);
 	 	$("#password-error-text").text(window.i18n.msgStore['passProblem']);
 	 	$("#roles-label").text(window.i18n.msgStore['adminUI-AddRoles']);
 	 	$("#roles-error-text").text(window.i18n.msgStore['adminUI-RolesError']);
 	 	$("#add-button").text(window.i18n.msgStore['adminUI-AddUser']);
 	 	$("#add-button").attr('data-loading-text', "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['adminUI-AddUser']);
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
			error["all"]={};
			error["all"].isError = false;

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
	
	function clearStatus(element) {
		let parent = element.closest(".form-group"); 
		parent.removeClass("has-error");
		parent.removeClass("has-warning");
		parent.removeClass("has-success");
		parent.removeClass("has-feedback");
		let glyphElement = element.next();
		glyphElement.removeClass("glyphicon-remove");
		glyphElement.removeClass("glyphicon-warning-sign");
		glyphElement.removeClass("glyphicon-ok");
	};
	
	function setSuccess(element) {
		element.closest(".form-group").addClass("has-success");
		element.closest(".form-group").addClass("has-feedback");
		element.next().addClass("glyphicon-ok");
	};
	
	function setWarning(element) {
		element.closest(".form-group").addClass("has-warning");
		element.closest(".form-group").addClass("has-feedback");
		element.next().addClass("glyphicon-warning-sign");
	}
	
	function setError(element) {
		element.closest(".form-group").addClass("has-error");
		element.closest(".form-group").addClass("has-feedback");
		element.next().addClass("glyphicon-remove");
	};
	
	$("#username-input").blur(function(e) {
		let element = $(e.target);
		clearStatus(element);
		//$("#username-tip-text").text("")
		if (element.val()!=""){
			$.post("../SearchAdministrator/isUserInBase",{username:element.val()},function(data){
				if (data.code == 0){
					if (data.status == "true"){
						setError(element);
					}else{
						setSuccess(element);
					}
				}else{
					setWarning(element);
				}
			},"json");
			username = element.val();
		}
	});
	
	$("#ldapUser").change(function() {
		if(this.checked) {
			bLDAPUser = true;
			$("#passwordSection").hide();
			$("#confirmPasswordSection").hide();
			$("#password-input").val('');
			$("#confirm-password-input").val('');
			clearStatus($("#password-input"));
			clearStatus($("#confirm-password-input"));
		} else {
			bLDAPUser = false;
			$("#passwordSection").show();
			$("#confirmPasswordSection").show();
		}
	});
	
	$("#confirm-password-input").change(function(e) {
		checkPasswordOrLdap();
	});
	
	$("#password-input").change(function(e) {
		checkPasswordOrLdap();
	});
	
	$('#roles-input').autocomplete({
		source : listRoles,
		select : function(event,ui){
			var role = ui.item.value;
			timeouts.push(setTimeout(function(){
				$('#roles-input').val("");
			},500));
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
					checkRoles();
				});
				$("#roles").append(html);
				roles.push(role);
				checkRoles();
			}
		}
	});
	
	function checkUsername() {
		return $("#username-input").closest(".form-group").hasClass('has-success');
	}
	
	function checkPasswordOrLdap() {
		let result = $("#ldapUser").is(":checked");
		clearStatus($("#password-input"));
		clearStatus($("#confirm-password-input"));
		if (!result) {
			result = $("#password-input").val() == $("#confirm-password-input").val() && $("#password-input").val() && $("#password-input").val() != '';
			if(result) {
				setSuccess($("#password-input"));
				setSuccess($("#confirm-password-input"));
			} else {
				setError($("#password-input"));
				setError($("#confirm-password-input"));
			}
		}
		return result;
	}
	
	function checkRoles() {
		let result = !(roles==null || roles.length==0);
		clearStatus($('#roles-input'));
		if (result) {
			setSuccess($('#roles-input'));
		} else {
			setError($('#roles-input'));
		}
		return result;
	}
	
	function checkForm() {
		let result = true;
		result = checkUsername() && result;
		result = checkPasswordOrLdap()&& result;
		result = checkRoles() && result;
		return result;
	};

	$('#addForm').submit(function(e){
		e.preventDefault();
		if ($("#add-button").hasClass('disabled')) {
			return;
		}
		$("#add-button").button("loading");
		if (!checkForm()) {
			$("#add-button").button("reset");
			return false;
		}
		
		let username = $("#username-input").val();
		let password = $("#password-input").val();
		let confirmPassword = $("#confirm-password-input").val();
		let bLDAPUser = $("#ldapUser").is(":checked");
		if (username==null || username=="" || (password==null && !bLDAPUser) || (password=="" && !bLDAPUser) || roles==null || roles.length==0){
			error["all"]={};
			error["all"].isError = true;
			showError(FIELDNOTFILLED,"all");
			$("#add-button").button("reset");
			return false;
		}
		for (var index in error){
			if (error[index].isError){
				admin_messageDiv.text(error[index].message).addClass("danger").prepend('<i class="fa fa-exclamation-triangle"></i>').show();
				$("#add-button").button("reset");
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
					clearStatus($("#username-input"));
					clearStatus($("#password-input"));
					clearStatus($('#roles-input'));
				},1500));
			}else{
				showError(data.code,"all");
			}
			$("#add-button").button("reset");
		},"json");
		return false;
	});
});
