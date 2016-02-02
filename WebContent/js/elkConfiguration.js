$(document).ready(function() {
	var SERVERALLOK = 0;
	var SERVERGENERALERROR = -1;
	var PROBLEMSERVERLDAPCONNECTION = -6;
	
	//Internationalize content
	$("#topbar1").text(window.i18n.msgStore['home']);
	$("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
	$("#topbar3").text(window.i18n.msgStore['adminUI-ELKConf']);
	$("#submit").text(window.i18n.msgStore['save']);
	$("#title").text(window.i18n.msgStore['adminUI-ELKConf']);
	$("#kibanaURILabel").html(window.i18n.msgStore['kibanaURI']);
	var input = $("#elk_activation input");
	var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
	$.get("../SearchAdministrator/checkELKAvailability",function(data){
		inputActivation(data, input);
		fillFields(data);
	},"json");
	
	function inputActivation(data){
		if (data.code == 0 ){
			if (data.ELKactivation=="true"){
				var bool = true;
				$("input").prop('disabled', false);
				$("button").prop('disabled',false);
				
			}else{
				var bool = false;
				$("input").prop('disabled', true);
				$("button").prop('disabled',true);
				
			}
			input.prop('checked',bool);		
		}else{
			$("#message").html('<i class="fa fa-times"></i> An error occured, Please try again')
				.addClass("error").removeClass("success").show();
		}
	}
	
	function fillFields(data){
		if (data.code == 0 ){
			$("#elasticsearchPort").val(data.ElasticsearchPort);
			$("#kibanaURI").val(data.KibanaURI);		
		}else{
			$("#message").html('<i class="fa fa-times"></i> An error occured, Please try again')
				.addClass("error").removeClass("success").show();
		}
	}
	
	
	$("#elk_activation").click(function(e){
		e.preventDefault();
		if (!input.is(':checked')){
			var bool="true";             
		}else{
			var bool="false";
		}
		$.post("../SearchAdministrator/checkELKAvailability",{
			ELKactivation : bool
		},function(data){
			inputActivation(data,input);
		},"json");
	});
	

	$("form").submit(function(e){
		e.preventDefault();
		if ($("#kibanaURI").val()!="" && $("#kibanaURI").val() != undefined){ 
			$.post("../SearchAdministrator/changeELKConf",{
				KibanaURI :  $("#kibanaURI").val() 
			},function(data){
				if (data!=undefined && data.code!= undefined){
					if (data.code==0){
						$("#message").html('<i class="fa fa-check"></i> Well Saved').addClass("success").removeClass("error").show();
						retrieveUsers();
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