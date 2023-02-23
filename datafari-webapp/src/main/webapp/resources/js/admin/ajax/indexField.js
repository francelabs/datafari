//# sourceURL=/Datafari/resources/js/admin/ajax//indexField.js



$(document).ready(function() {
	//Get the fields from the schema
	getFields();
	document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-IndexField'];
	document.getElementById("tablename").innerHTML = window.i18n.msgStore['fieldList'];
	document.getElementById("name").innerHTML = window.i18n.msgStore['name'];
	document.getElementById("type").innerHTML = window.i18n.msgStore['type'];
	document.getElementById("required").innerHTML = window.i18n.msgStore['required'];
	document.getElementById("stored").innerHTML = window.i18n.msgStore['stored'];
	document.getElementById("multivalued").innerHTML = window.i18n.msgStore['multivalued'];
	document.getElementById("indexed").innerHTML = window.i18n.msgStore['indexed'];
});
function getFields(){										//Get the fields from the schema.xml
	 $.ajax({			//Ajax request to the doGet of the FieldWeight servlet
	        type: "GET",
	        url: "./../admin/FieldWeight",
	        //if received a response from the server
	        success: function( data, textStatus, jqXHR) {	
	        	
	        	//If they're was an error print the error
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		document.getElementById("globalAnswer").innerHTML = data;
	        	}else{		//else add the options to the select
	        		list = data;
	        		//add a table to print all the info
	        		for(var i = 0 ; i < data.field.length ; i++){
	        			$("#tbody").append("<tr id="+i+"><th>"+data.field[i].name+"</th><th>"+data.field[i].type+"</th>");
	        			if(data.field[i].indexed===undefined){
	        				$("#"+i).append("<th>true</th>");
	        			}else{
	        				$("#"+i).append("<th>"+data.field[i].indexed+"</th>");
	        			}
	        			if(data.field[i].stored===undefined){
	        				$("#"+i).append("<th>true</th>");
	        			}else{
	        				$("#"+i).append("<th>"+data.field[i].stored+"</th>");
	        			}
	        			if(data.field[i].required===undefined){
	        				$("#"+i).append("<th>false</th>");
	        			}else{
	        				$("#"+i).append("<th>"+data.field[i].required+"</th>");
	        			}
	        			if(data.field[i].multiValued===undefined){
	        				$("#"+i).append("<th>false</th>");
	        			}else{
	        				$("#"+i).append("<th>"+data.field[i].multiValued+"</th>");
	        			}
	        			$("#tbody").append("</tr>");
	        		}
	        	}
	        }
	 });
}