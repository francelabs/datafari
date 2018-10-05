


//# sourceURL=/Datafari/ajax/js/sizeLimitations.js


$(document).ready(function() {
	//Internationalize content
	document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-SizeLimitation'];
	document.getElementById("labelhl").innerHTML = window.i18n.msgStore['labelhl']+" : ";
	document.getElementById("submithl").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("hlname").innerHTML = window.i18n.msgStore['limitHL'];
	
	//Disable the input and submit
	$('#submithl').attr("disabled", true);
	$('#maxhl').attr("disabled", true);
	//If the semaphore was for this page and the user leaves it release the semaphores
	//On refresh
	$(window).bind('beforeunload', function(){  								
		if(document.getElementById("submithl")!==null){
			if(!document.getElementById("submithl").getAttribute('disabled')){
				  cleanSem("hl.maxAnalyzedChars");
			}
		}
	 });
	
	//If the user loads an other page
	$("a").click(function(e){
		if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
			if(document.getElementById("submithl")!==null){
				if(!document.getElementById("submithl").getAttribute('disabled')){
					  cleanSem("hl.maxAnalyzedChars");
				}
			}
		}
	});
	
	//Get hl.maxAnalyzedChars value
	$.get('../GetHighlightInfos', function(data){
		if(data.code == 0) { 
			document.getElementById("maxhl").value = data.maxAnalyzedChars;    
    		$('#submithl').attr("disabled", false);
    		$('#maxhl').attr("disabled", false);
		} else {
			document.getElementById("globalAnswer").innerHTML = data;
    		$('#submithl').attr("disabled", true);
    		$('#maxhl').attr("disabled", true);
		}
	}, "json");
	
	//Sert the button to call the function set with the hl.maxAnalyzedChars parameter
	$("#submithl").click(function(e){
		e.preventDefault();
		$.post('./SetHighlightInfos', {maxAnalyzedChars : document.getElementById("maxhl").value }, function(data) {
			if(data.code == 0) {
				document.getElementById("answerhl").innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
		     	$("#answerhl").addClass("success");
	     		$("#answerhl").fadeOut(3000,function(){
	     			$("#answerhl").removeClass("success");
	     			$("#answerhl").html("");
	     			$("#answerhl").show();
	     		});
			} else {
				document.getElementById("globalAnswer").innerHTML = data;
    			$('#submithl').attr("disabled", true);
	    		$('#maxhl').attr("disabled", true);
			}
		}, "json");
	});
	
	//If the user loads an other page
	$("a").click(function(e){
		if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
			if(document.getElementById("submitindexhl")!==null){
				if(!document.getElementById("submitindexhl").getAttribute('disabled')){
					  cleanSem("maxLength");
				}
			}
		}
	});
});




function cleanSem(type){
	$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet to release the semaphore
        type: "GET",
        url: "./../admin/ModifyNodeContent",
        data : "sem=sem&type="+type
	 });
}
