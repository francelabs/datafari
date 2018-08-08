//# sourceURL=/Datafari/ajax/js/elkConfiguration.js


$(document).ready(function() {
	var SERVERALLOK = 0;
	var SERVERGENERALERROR = -1;
	var PROBLEMSERVERLDAPCONNECTION = -6;
	var externalELK = false;
	
	//Internationalize content
	$("#topbar1").text(window.i18n.msgStore['home']);
	$("#topbar2").text(window.i18n.msgStore['adminUI-UserManagement']);
	$("#topbar3").text(window.i18n.msgStore['adminUI-ELKConf']);
	$("#submit").text(window.i18n.msgStore['save']);
	$("#title").text(window.i18n.msgStore['adminUI-ELKConf']);
	$("#elkActivationLabel").html(window.i18n.msgStore['elkActivationLabel'] + " (" + window.i18n.msgStore['no_save_needed'] + ")");
	$("#kibanaURILabel").html(window.i18n.msgStore['kibanaURI']);
	$("#authUserLabel").html(window.i18n.msgStore['authUser']);
	var input = $("#elk_activation input");
	var ENDOFANIMATION = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
	
	
	$.get("../SearchExpert/ELKAdmin",function(data){
		inputActivation(data, input);
		fillFields(data);
	},"json");
	
	function inputActivation(data){
		if (data.code == 0 ){
			if (data.ELKactivation=="true" || data.isELKUp=="true"){
				var bool = true;
			}else{
				var bool = false;
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
			$("#authUser").val(data.authUser);
			externalELK = fillExtendedFields(data);
			
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
		$.post("../SearchExpert/ELKAdmin",{
			ELKactivation : bool,
			externalELK : externalELK,
			ELKServer : $("#ELKServer").val(),
			ELKScriptsDir : $("#ELKScriptsDir").val()
		},function(data){
			inputActivation(data,input);
		},"json");
	});
	

	$("form").submit(function(e){
		e.preventDefault();
		if ($("#kibanaURI").val()!="" && $("#kibanaURI").val() != undefined){ 
			if((externalELK===true && $("#ELKServer").val()!="" && $("#ELKServer").val() != undefined && $("#ELKScriptsDir").val()!="" && $("#ELKScriptsDir").val() != undefined) || externalELK===false)
			$.post("../SearchAdministrator/changeELKConf",{
				KibanaURI :  $("#kibanaURI").val(),
				authUser :  $("#authUser").val(),
				externalELK : getExternalELK(),
				ELKServer : $("#ELKServer").val(),
				ELKScriptsDir : $("#ELKScriptsDir").val()
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