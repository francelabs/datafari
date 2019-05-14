//# sourceURL=/Datafari/admin/ajax/js/modifyUsers.js

var NOFAVORITESFOUND = 101;
var SERVERALREADYPERFORMED = 1;
var SERVERALLOK = 0;
var SERVERGENERALERROR = -1;
var SERVERNOTCONNECTED = -2;
var SERVERPROBLEMCONNECTIONDB = -3;
var PROBLEMECONNECTIONSERVER = -404;
var admin_messageDiv = $("#Message");
var elementDiv;
var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
var listRoles = ["ConnectedSearchUser","SearchAdministrator","SearchExpert"];

setupLanguage();

function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
$("#topbar1").text(window.i18n.msgStore['home']);
$("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
$("#topbar3").text(window.i18n.msgStore['adminUI-ModifyUsers']);
}

function showError(code){
		var message;
		var danger = true;
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
		default :
			message = window.i18n.msgStore["SERVERGENERALERROR"];
			break;
	}
	$("#tableResult").hide();
	admin_messageDiv.text(message).show();
	if (danger){
			admin_messageDiv.addClass("alert-danger").prepend('<i class="fas fa-exclamation-triangle"></i><br/>');
	}else{
		admin_messageDiv.removeClass("alert-danger");
		}
}

function htmlRole(role,username){
	return '<div class="inline_block '+username+'" data-user="'+username+'"><div class="input-group role">'+
  '<span class="input-group-addon delete notyet"><i class="fas fa-times"></i></span>'+
  '<input class="form-control '+role+'" value="'+role+'" type="text" disabled/>'+
  '</div></div>';
}

function deleteRoleListener (e){
	$("#Message").hide();
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

if (window.dialogue == undefined){
	window.dialogue = $(".dialogue").dialog({
	 autoOpen: false,
	buttons:{
		Delete: function(){
				while(!elementDiv.hasClass("root")){
					elementDiv = elementDiv.parent();
				}
				$.post("../SearchAdministrator/deleteUser",{username:elementDiv.data("user")},function(data){
					if (data.code == SERVERALLOK){
						elementDiv.remove();
					}else{
						showError(data.code);
					}
				},"json");
				$( this ).dialog( "close" );		
		},
		Cancel: function(){
			$( this ).dialog( "close" );					                                		 	                                		 
			}
		}
	});
}

$.get("../SearchAdministrator/getAllUsersAndRoles",function(data){
	if (data.code == SERVERALLOK){
		window.globalVariableUser = data.status;
		var html = "";
		$.each(window.globalVariableUser,function(index,element){
			 html+= '<tr class="root" data-user="'+index+'">'+
					"<th>"+index+"</th>"+
					'<th><input data-user="'+index+'" placeHolder="change password" type="password" class="password"/></th>'+
					'<th>';
			for (var i = 0; i<element.length; i++){
				html+=htmlRole(element[i],index);
			}
			
			html+='<input data-user="'+index+'" class="add_role" placeHolder="Add a Role"/></th>'+
					'<th><span class="delete_user"><i class="fas fa-times"></i></span></th>'+
					'</tr>';
		});
		$("tbody").append(html);
		$(".password").keydown(function(e){
			if (e.keyCode == 13){
				$("#Message").hide();
				var element = $(e.target);
				while(!element.hasClass("root")){
					element = element.parent();
				}
				$.post("../SearchAdministrator/changePassword",{username:element.data("user"),password:$(e.target).val()},function(data){
					if (data.code == SERVERALLOK){
						$(e.target).before('<i class="fas fa-check success"></i>');
						setTimeout(function(){
							$(e.target).prev().addClass("animated fadeOut").one(ENDOFANIMATION,function(){
								$(e.target).val("").prev().remove();
							});
						},1500);
					}else{
						showError(data.code);
					}
				})
				return false;
			}
		});				

		$(".delete_user").click(function(e){
			$("#Message").hide();
			elementDiv = $(e.target);
			while(!elementDiv.hasClass("root")){
				elementDiv = elementDiv.parent();
			}
			$.get("../SearchAdministrator/checkUser",{username:elementDiv.data("user"),action:"delete"}, function(data) {
				if(data.code == SERVERALLOK) {
					if(data.allowed == true) {
						window.dialogue.dialog("open");
					} else {
						$("#Message").removeClass("alert-success");
						$("#Message").addClass("alert-danger");
						$("#Message").html(window.i18n.msgStore['delete-user-unallowed']);
						$("#Message").show();
					}
				}
			});
	     });
			
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
		
	}else{
		showError(data.code);
	}
},"json");