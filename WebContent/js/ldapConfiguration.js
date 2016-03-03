$(document).ready(function() {
	var SERVERALLOK = 0;
	var SERVERGENERALERROR = -1;
	var PROBLEMSERVERLDAPCONNECTION = -6;
	var listRoles = ["ConnectedSearchUser","SearchAdministrator","SearchExpert"];
	
	//Internationalize content
	$("#topbar1").text(window.i18n.msgStore['home']);
	$("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
	$("#topbar3").text(window.i18n.msgStore['adminUI-LdapConfig']);
	$("#submit").text(window.i18n.msgStore['save']);
	$("#title").text(window.i18n.msgStore['ldapConfig']);
	$("#ad_roles").html(window.i18n.msgStore['adRoles']);
	$("#ad_note").html(window.i18n.msgStore['adNote']);
	$("#connectionURLLabel").html(window.i18n.msgStore['adURLLabel']);
	$("#connectionNameLabel").html(window.i18n.msgStore['adUsernameLabel']);
	$("#connectionPasswordLabel").html(window.i18n.msgStore['adPasswordLabel']);
	$("#thUsername").html(window.i18n.msgStore['username']);
	$("#thRoles").html(window.i18n.msgStore['roles']);
	var input = $("#ldap_activation input");
	var regexConnectionURL = /ldap:\/\/.+\:[0-9]+/;
	var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
	$.get("../SearchAdministrator/isLdapConfig",function(data){
		inputActivation(data,input);
	},"json");
	
	$("#crawl_users").click(function() {retrieveUsers()});
	
	$('.warning').hide();
	
	function htmlRole(role,username){
		return '<div class="inline_block '+username+'" data-user="'+username+'"><div class="input-group role">'+
		  '<span class="input-group-addon delete notyet"><i class="fa fa-times"></i></span>'+
		  '<input class="form-control '+role+'" value="'+role+'" type="text" disabled/>'+
		  '</div></div>';
	}
	
	function deleteRoleListener (e){
		var element = $(e.target);
		while(!element.hasClass("inline_block")){
			element = element.parent();
		}
		var role = element.find("input").val();
		$.post("../SearchAdministrator/deleteRole",{
			username : element.data("user"),
			role:role
		},function(data){
			if (data.code == SERVERALLOK){
				element.remove();
			}else{
				showError(data.code);
			}
		},"json");
	}
	
	function retrieveUsers() {
		
		$('#tableResult tbody tr').remove();
		
		$.get("../SearchAdministrator/getAllLDAPUsersAndRoles",function(data){
			
			$('.warning').hide();
			$('.warning').html('');
			if(data.code == SERVERALLOK) {
				$('.warning').hide();
				window.globalVariableUser = data.statut;
				var html = "";
				$.each(window.globalVariableUser,function(index,element){
					 html+= '<tr class="root" data-user="'+index+'">'+
							"<th>"+index+"</th>"+
							'<th>';
					for (var i = 0; i<element.length; i++){
						html+=htmlRole(element[i],index);
					}
					
					html+='<input data-user="'+index+'" class="add_role" placeHolder="Add a Role"/></th>'+
							'</tr>';
				});
				$("tbody").append(html);
					
				$(".add_role").autocomplete({
					source : listRoles,
					select : function(event,ui){
						var role = ui.item.value;
						//console.log(role);
						var element = $(event.target);
						element.val("");
						if ($("."+element.data("user")+" ."+role).length==0){
							$.post("../SearchAdministrator/addRole",{username:element.data("user"),role:role},function(data){
								if (data.code==SERVERALLOK){
									var html = $(htmlRole(role,element.data("user")));
									element.before(html);
									$(".inline_block .delete.notyet").click(deleteRoleListener).removeClass("notyet");
								}else{
									showError(data.code);
								}
								element.val("");
							});
						}
						return false;
					},
					autoFocus:1
				});
				$(".inline_block .delete.notyet").click(deleteRoleListener).removeClass("notyet");
			} else {
				if(data.code == PROBLEMSERVERLDAPCONNECTION) {
					$('.warning').html('Unable to connect to the AD user base, check the AD configuration !');
				} else if (data.code == SERVERGENERALERROR) {
					$('.warning').html('Unable to find AD users !');
				}
				$('.warning').show();
			}
		},"json");
	}
	
	function inputActivation(data){
		if (data.code == 0 ){
			if (data.isActivated=="true"){
				var bool = true;
				$("input").prop('disabled', false);
				$("button").prop('disabled',false);
				
				$('.assign_role').show();
				
			}else{
				var bool = false;
				$("input").prop('disabled', true);
				$("button").prop('disabled',true);
				
				$('.assign_role').hide();
			}
			input.prop('checked',bool);		
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
	$("#ldap_activation").click(function(e){
		e.preventDefault();
		if (!input.is(':checked')){
			var bool="true";             
		}else{
			var bool="false";
		}
		$.post("../SearchAdministrator/isLdapConfig",{
			isLdapActivated : bool
		},function(data){
			inputActivation(data,input);
		},"json");
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
					}else if (data.code == PROBLEMSERVERLDAPCONNECTION){
						$("#message").html('<i class="fa fa-times"></i> '+data.statut).addClass("error").removeClass("success").show();
					}else{
						$("#message").html('<i class="fa fa-times"></i> An error occured, Please try again').addClass("error").removeClass("success").show();
					}
				}else{
					$("#message").html('<i class="fa fa-times"></i> An error occured, Please try again').addClass("error").removeClass("success").show();
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