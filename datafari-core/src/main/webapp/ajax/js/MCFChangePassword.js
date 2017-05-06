$(document).ready(function() {
		setupLanguage();
		
		
		
		
		
		//Set the onClick function of the saveElevateConf button
		$("#savePasswordButton").click(function() {
			$("#message").hide();
			var password ="" ;
			var confirm_password ="" ;
			
			if ((document.getElementById('password').value) != null) {
				password = document.getElementById('password').value;
			}
			if ((document.getElementById('confirm_password').value) != null) {
				confirm_password = document.getElementById('confirm_password').value;
			}
			console.log("password : " +password);
			console.log("confirm_password" +confirm_password);
			
			if ( (password !="") && (confirm_password !="") && (password == confirm_password)){
			console.log("OK");
			$.post("../SearchAdministrator/MCFChangePassword",{
				password : password
			
			},function(data){
				if(data.code == 0) {
						$("#message").html(window.i18n.msgStore["passSaved"]);
						$("#message").addClass("success");
						$("#message").show();						
					
				} else {
					$("#message").html(window.i18n.msgStore["passSaveError"]);
					$("#message").addClass("error");
					$("#message").show();
				}
			
			},"json");
			}
			else {
				$("#message").html(window.i18n.msgStore["passProblem"]);
			}
		});
		
});


	
function setupLanguage(){
	$(document).find('a.indexAdminUIBreadcrumbLink').prop('href', "./index.jsp?lang=" + window.i18n.language);
	$("#topbar1").text(window.i18n.msgStore['home']);
	$("#topbar2").text(window.i18n.msgStore['adminUI-Connectors']);
	$("#topbar3").text(window.i18n.msgStore['adminUI-Connectors-MCFPassword']);
	 document.getElementById("changePasswordLabel").innerHTML = window.i18n.msgStore['MCFPasswordAdmin'];
	 document.getElementById("enterPasswordLabel").innerHTML = window.i18n.msgStore['enterPassword'];
	 document.getElementById("confirmPasswordLabel").innerHTML = window.i18n.msgStore['confirmPassword'];
	 document.getElementById("selectPassword").innerHTML = window.i18n.msgStore['changeMCFPassword'];
	 document.getElementById("savePasswordButton").innerHTML = window.i18n.msgStore['confirmPass'];
	 
	
}

function checkPassword() {
	  if (document.getElementById('password').value ==
	    document.getElementById('confirm_password').value) {
	    document.getElementById('messagePassword').style.color = 'green';
	    document.getElementById('messagePassword').innerHTML = window.i18n.msgStore['match'];
	  } else {
	    document.getElementById('messagePassword').style.color = 'red';
	    document.getElementById('messagePassword').innerHTML = window.i18n.msgStore['notmatch'];
	  }
	}



