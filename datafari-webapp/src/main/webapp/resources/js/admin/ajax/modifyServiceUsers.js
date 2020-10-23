//# sourceURL=/Datafari/resources/js/admin/ajax//modifyServiceUsers.js

var NOFAVORITESFOUND = 101;
var SERVERALREADYPERFORMED = 1;
var SERVERALLOK = 0;
var SERVERGENERALERROR = -1;
var SERVERNOTCONNECTED = -2;
var SERVERPROBLEMCONNECTIONDB = -3;
var PROBLEMECONNECTIONSERVER = -404;
var admin_messageDiv = $("#Message");
var adminKibana_messageDiv = $("#MessageKibana");
var elementDiv;
var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
var listRoles = ["ConnectedSearchUser","SearchAdministrator","SearchExpert"];

setupLanguage();

function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
$("#topbar1").text(window.i18n.msgStore['home']);
$("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
$("#topbar3").text(window.i18n.msgStore['adminUI-ModifyUsers']);
$("#documentation-modifyusersapache").text(window.i18n.msgStore['documentation-modifyusersapache']);
$("#Message").hide();
$("#MessageKibana").hide();
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

function showKibanaError(message){
  adminKibana_messageDiv.text(message).show();
  adminKibana_messageDiv.addClass("alert-danger").prepend('<i class="fas fa-exclamation-triangle"></i>');
}



$.get("../SearchAdministrator/getAllUsersAndRolesApache",function(data){
	if (data.code == SERVERALLOK){
	
      
      window.globalVariableUser = data.status;
      var html = "";
      $.each(window.globalVariableUser,function(index,element){
			 html+= '<tr class="root" data-user="'+element+'">'+
					"<th>"+element+"</th>"+
					'<th><input data-user="'+element+'" placeHolder="change password" type="password" class="passwordApache"/></th>'+
					'<th>';
			
		});
		$("#apacheBody").append(html);
		$(".passwordApache").keydown(function(e){
			if (e.keyCode == 13){
				$("#Message").show();
				var element = $(e.target);
				while(!element.hasClass("root")){
					element = element.parent();
				}
				$.post("../SearchAdministrator/changePasswordApache",{username:element.data("user"),password:$(e.target).val()},function(data){
					if (data.code == SERVERALLOK){
						$(e.target).before('<i class="fas fa-check success"></i>');
						setTimeout(function(){
							$(e.target).prev().addClass("animated fadeOut").one(ENDOFANIMATION,function(){
								$(e.target).val("").prev().remove();
							});
						},1500);
						$("#Message").hide();
					}else{
						showError(data.code);
					}
				})
				return false;
			}
		});				

		
		
		
	}else{
		showError(data.code);
	}
},"json");

$.get("../SearchAdministrator/changePasswordKibana",function(data){
  if (data.code == SERVERALLOK){
  
      
      var kibanaUsers = data.users;
      var html = "";
      $.each(kibanaUsers,function(index,element){
       html+= '<tr class="root" data-user="'+element+'">'+
          "<th>"+element+"</th>"+
          '<th><input data-user="'+element+'" placeHolder="change password" type="password" class="passwordKibana"/></th>'+
          '<th>';
      
    });
    $("#kibanaBody").append(html);
    $(".passwordKibana").keydown(function(e){
      if (e.keyCode == 13){
        adminKibana_messageDiv.removeClass("alert-danger");
        $("#MessageKibana").html("<i class='fa fa-spinner fa-spin'></i> Please wait");
        $("#MessageKibana").show();
        var element = $(e.target);
        while(!element.hasClass("root")){
          element = element.parent();
        }
        $.post("../SearchAdministrator/changePasswordKibana",{username:element.data("user"),password:$(e.target).val()},function(data){
          if (data.code == SERVERALLOK){
            $(e.target).before('<i class="fas fa-check success"></i>');
            setTimeout(function(){
              $(e.target).prev().addClass("animated fadeOut").one(ENDOFANIMATION,function(){
                $(e.target).val("").prev().remove();
              });
            },1500);
            $("#MessageKibana").hide();
          }else{
            showKibanaError(data.status);
          }
        })
        return false;
      }
    });       

    
    
    
  }else{
    showError(data.code);
  }
},"json");
