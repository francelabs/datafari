//# sourceURL=/Datafari/ajax/js/fieldWeightAPI.js


$(document).ready(function() {
	//Internationalize content
	document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-FieldWeight'];
	document.getElementById("labelth").innerHTML = window.i18n.msgStore['qf']+" : ";
	document.getElementById("labelth2").innerHTML = window.i18n.msgStore['pf']+" : ";
	document.getElementById("submitth").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("submittab").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("addRow").innerHTML = window.i18n.msgStore['addField'];
	document.getElementById("name").innerHTML = window.i18n.msgStore['field'];
	document.getElementById("type").innerHTML = window.i18n.msgStore['fieldWeight'];
	$('#submitth').attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);
	document.getElementById("thname").innerHTML = window.i18n.msgStore['fieldWeightExpert'];
	document.getElementById("thname2").innerHTML = window.i18n.msgStore['adminUI-FieldWeight'];
	//Disable the input and submit
	$('#submitth').attr("disabled", true);
	$('#maxth').attr("disabled", true);
	//If the semaphore was for this page and the user leaves it release the semaphores
	//On refresh
	$(window).bind('beforeunload', function(){  								
		if(document.getElementById("submitth")!==null){
			if(!document.getElementById("submitth").getAttribute('disabled')){
				cleanSem("threshold");
			}
		}
	});
	//If the user loads an other page
	$("a").click(function(e){
		if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
			if(document.getElementById("submitth")!==null){
				if(!document.getElementById("submitth").getAttribute('disabled')){
					cleanSem("threshold");
				}
			}
		}
	});
	//Get threshold value
	$.get('./FieldWeightAPI', function(data){
		if(data.code == 0) { 
			document.getElementById("qfAPI").value = data.qfAPI;
			document.getElementById("pfAPI").value = data.pfAPI;
			$('#submitth').attr("disabled", false);
			$('#qfAPI').attr("disabled", false);
			var qfAPI = data.qfAPI;
			qfAPI = qfAPI.trim();
			var words = qfAPI.split(' ');
			for(var i = 0 ; i < qfAPI.length ; i++){
				if((words[i]!=undefined) && (!words[i].startsWith("code"))) {
					var splitWord = words[i].split('^');


					$("#tbody").append("<tr id='hello"+i+"'><th>"+splitWord[0]+"</th><th><input type='text'  id='qf"+splitWord[0]+"' name='qfAPI' value='"+splitWord[1]+"'></th>");

				}
				$("#tbody").append("</tr>");
			}




		} else {
			document.getElementById("globalAnswer").innerHTML = data;
			$('#submitth').attr("disabled", true);
			$('#autoCompleteThreshold').attr("disabled", true);
		}
	}, "json");
	//Sert the button to call the function set with the threshold parameter
	$("#submitth").click(function(e){
		e.preventDefault();

		$.post('./FieldWeightAPI', {qfAPI : document.getElementById("qfAPI").value, pfAPI : document.getElementById("pfAPI").value }, function(data) {
			if(data.code == 0) {
				document.getElementById("answerth").innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
				var qfAPI = document.getElementById("qfAPI").value;
				qfAPI = qfAPI.trim();
				var words = qfAPI.split(' ');
				$("#tbody").empty();
				for(var i = 0 ; i < qfAPI.length ; i++){
					if((words[i]!=undefined) && (!words[i].startsWith("code"))) {
						var splitWord = words[i].split('^');


						$("#tbody").append("<tr id='hello"+i+"'><th>"+splitWord[0]+"</th><th><input type='text'  id='qf"+splitWord[0]+"' name='qfAPI' value='"+splitWord[1]+"'></th>");

					}
					$("#tbody").append("</tr>");
				}
				$("#answerth").addClass("success");
				$("#answerth").fadeOut(3000,function(){
					$("#answerth").removeClass("success");
					$("#answerth").html("");
					$("#answerth").show();
				});
			} else {
				document.getElementById("globalAnswer").innerHTML = data;
				$('#submitth').attr("disabled", true);
				$('#autocompleteThreshold').attr("disabled", true);
			}
		}, "json");
	});



	$("#submittab").click(function(e){


		var newQF ="";
		var table = document.getElementById("tbody");
		for (var i = 0, row; row = table.rows[i]; i++) {
			var tempQF=""
				//iterate through rows
				// console.log(row.innerHTML);
				//rows would be accessed using the "row" variable assigned in the for loop
				//alert(row.innerHTML); 
				console.log(row.getElementsByTagName("th")[0].innerHTML);
			if (!(row.getElementsByTagName("th")[0].innerHTML.startsWith("<input"))){
				tempQF = row.getElementsByTagName("th")[0].innerHTML;
				console.log("tempQF1:"+tempQF)
			}
			currentTh = row.getElementsByTagName("th");
			// console.log(row.getElementsByTagName("input")[0].value);
			//console.log(currentTh.length);
			for(j=0; j<currentTh.length; j++) {
				//console.log(currentTh[j]);
				if (currentTh[j].getElementsByTagName("input")[0]) {
					console.log(currentTh[j].getElementsByTagName("input")[0].value);
					if (tempQF != ""){
						tempQF= tempQF+"^"+currentTh[j].getElementsByTagName("input")[0].value;
					}
					else {
						tempQF= currentTh[j].getElementsByTagName("input")[0].value;
					}
					console.log("tempQF"+tempQF);
				}
			}
			newQF= newQF+" "+tempQF;
			newQF = newQF.trim();

		}
		console.log(newQF);

		$.post('./FieldWeightAPI', {qfAPI : newQF, pfAPI : document.getElementById("pfAPI").value  }, function(data) {
			if(data.code == 0) {
				document.getElementById("answerth").innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
				document.getElementById("qfAPI").value = newQF;

				$("#answerth").addClass("success");
				$("#answerth").fadeOut(3000,function(){
					$("#answerth").removeClass("success");
					$("#answerth").html("");
					$("#answerth").show();
				});
			} else {
				document.getElementById("globalAnswer").innerHTML = data;
				$('#submitth').attr("disabled", true);
				$('#autocompleteThreshold').attr("disabled", true);
			}
		}, "json");
	});


	$("#submitth").click(function(e){
		e.preventDefault();

		$.post('./FieldWeightAPI', {qfAPI : document.getElementById("qfAPI").value, pfAPI : document.getElementById("pfAPI").value }, function(data) {
			if(data.code == 0) {
				document.getElementById("answerth").innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
				var qfAPI = document.getElementById("qfAPI").value;
				qfAPI = qfAPI.trim();
				var words = qfAPI.split(' ');
				$("#tbody").empty();
				for(var i = 0 ; i < qfAPI.length ; i++){
					if((words[i]!=undefined) && (!words[i].startsWith("code"))) {
						var splitWord = words[i].split('^');


						$("#tbody").append("<tr id='hello"+i+"'><th>"+splitWord[0]+"</th><th><input type='text'  id='qf"+splitWord[0]+"' name='qfAPI' value='"+splitWord[1]+"'></th>");

					}
					$("#tbody").append("</tr>");
				}
				$("#answerth").addClass("success");
				$("#answerth").fadeOut(3000,function(){
					$("#answerth").removeClass("success");
					$("#answerth").html("");
					$("#answerth").show();
				});
			} else {
				document.getElementById("globalAnswer").innerHTML = data;
				$('#submitth').attr("disabled", true);
				$('#autocompleteThreshold').attr("disabled", true);
			}
		}, "json");
	});



	$("#addRow").click(function(e){

		var tabLength = document.getElementById("tableau").rows.length - 1;
		console.log(tabLength);

		$("#tbody").append("<tr id='hello"+tabLength+"'><th><input type='text' name='newRowField"+tabLength+"' id='newRowField"+tabLength+"' value=''></th><th><input type='text' id='newRowValue"+tabLength+"'  name='newRowValue"+tabLength+"'></th>");
	});



});




function get(type){
	var typ = type.substring(0,2);
	document.getElementById("max"+typ).value = "";
	$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet
		type: "GET",
		url: "./../admin/ModifyNodeContent",
		data : "type="+type+"&attr=name",
		//if received a response from the server
		success: function( data, textStatus, jqXHR) {	
			//If the semaphore was already acquired
			if(data === "File already in use"){
				//Print it and disable the input and submit
				document.getElementById("answer"+typ).innerHTML = window.i18n.msgStore['usedFile'];
				$('#submit'+typ).attr("disabled", true);
				$('#max'+typ).attr("disabled", true);
			}//If they're was an error
			else if(data.toString().indexOf("Error code : ")!==-1){
				//print it and disable the input and submit
				document.getElementById("globalAnswer").innerHTML = data;
				$('#submit'+typ).attr("disabled", true);
				$('#max'+typ).attr("disabled", true);
			}else{		//else add the options to the select
				document.getElementById("max"+typ).value = data;    
				$('#submit'+typ).attr("disabled", false);
				$('#max'+typ).attr("disabled", false);
			}
		}
	});
}
function set(type){
	$("#submitth").button("loading");
	var typ = type.substring(0,2);
	var value = document.getElementById("max"+typ).value;
	if(value<=1 && value>=0){
		$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet to modify the solrconfig file
			type: "POST",
			url: "./../admin/ModifyNodeContent",
			data : "type="+type+"&value="+value+"&attr=name",
			//if received a response from the server
			success: function( data, textStatus, jqXHR) {	
				//If the semaphore was already acquired
				if(data === "File already in use"){
					//Print it and disable the input and submit
					document.getElementById("answer"+typ).innerHTML = window.i18n.msgStore['usedFile'];
					$('#submit'+typ).attr("disabled", true);
					$('#max'+typ).attr("disabled", true);
				}//If they're was an error
				else if(data.toString().indexOf("Error code : ")!==-1){
					//print it and disable the input and submit
					document.getElementById("globalAnswer").innerHTML = data;
					$('#submit'+typ).attr("disabled", true);
					$('#max'+typ).attr("disabled", true);
				}else{		//else add the options to the select
					document.getElementById("answer"+typ).innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
					$("#answer"+typ).addClass("success");
					$("#answer"+typ).fadeOut(3000,function(){
						$("#answer"+typ).removeClass("success");
						$("#answer"+typ).html("");
						$("#answer"+typ).show();
					});
				}
			},
			//this is called after the response or error functions are finsihed
			complete: function(jqXHR, textStatus){
				//enable the button
				$("#submitth").button("reset");
			}
		});
	}else{
		document.getElementById("answer"+typ).innerHTML = window.i18n.msgStore['inputMust'];
	}
}
function cleanSem(type){
	$.ajax({			//Ajax request to the doGet of the ModifyNodeContent servlet to release the semaphore
		type: "GET",
		url: "./../admin/ModifyNodeContent",
		data : "sem=sem&type="+type
	});
}