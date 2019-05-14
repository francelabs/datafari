//# sourceURL=/Datafari/admin/ajax/js/facetConfig.js

var list;
var i = 1;
var dragSrcEl = null;
$(document).ready(function() {
	//Get the table with the facets
	getExistingFacets();
	//Disable all the submit/confirm buttons
	$('#addsubmit').attr("disabled", true);
	$('#queryaddsubmit').attr("disabled", true);
	$('#changeOrder').attr("disabled", true);
	//If the user click on a link
	$("a").click(function(e){
		if(e.target.className==="ajax-link" || e.target.className==="ajax-link active-parent active"){
			if(document.getElementById("fileAlready")!==null){
				if(document.getElementById("fileAlready").innerHTML===""){
					cleanSem();
				}
			}
		}
	});
	//If the user refresh the page  
	$(window).bind('beforeunload', function(){  								
		if(document.getElementById("fileAlready").innerHTML===""){
			  cleanSem();
		}
	  });
	//Internationalize the text
	document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
	document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineConfig'];
	document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-FacetConfig'];
	document.getElementById("addfacetname").innerHTML = window.i18n.msgStore['addFacet'];
	document.getElementById("addsubmit").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("order").innerHTML = window.i18n.msgStore['order'];
	document.getElementById("field").innerHTML = window.i18n.msgStore['field'];
	document.getElementById("tablename").innerHTML = window.i18n.msgStore['listFacet'];
	document.getElementById("paginLegend").innerHTML = window.i18n.msgStore['pagination']+" : ";
	document.getElementById("selectTypeLegend").innerHTML = window.i18n.msgStore['selectionType']+" : ";
	document.getElementById("labelSelect").innerHTML = window.i18n.msgStore['field']+" : ";
	document.getElementById("frNameLegend").innerHTML = window.i18n.msgStore['facetName']+" (fr) : ";
	document.getElementById("enNameLegend").innerHTML = window.i18n.msgStore['facetName']+" (en) : ";
	document.getElementById("addqueryfacetname").innerHTML = window.i18n.msgStore['addQueryFacet'];
	document.getElementById("queryaddsubmit").innerHTML = window.i18n.msgStore['confirm'];
	document.getElementById("queryPaginLegend").innerHTML = window.i18n.msgStore['pagination']+" : ";
	document.getElementById("querySelectTypeLegend").innerHTML = window.i18n.msgStore['selectionType']+" : ";
	document.getElementById("querylabelSelect").innerHTML = window.i18n.msgStore['field']+" : ";
	document.getElementById("queryfrNameLegend").innerHTML = window.i18n.msgStore['facetName']+" (fr) : ";
	document.getElementById("queryenNameLegend").innerHTML = window.i18n.msgStore['facetName']+" (en) : ";
	document.getElementById("query1Legend").innerHTML = window.i18n.msgStore['queryNumber']+"1 :";
	document.getElementById("query1LabelLegendEn").innerHTML = window.i18n.msgStore['queryLabel']+" (en) :";
	document.getElementById("query1LabelLegendFr").innerHTML = window.i18n.msgStore['queryLabel']+" (fr) :";
	document.getElementById("addqueryLegend").innerHTML = window.i18n.msgStore['addQuerySlot']+" :";
	//Custom behaviour for buttons
	$("#addsubmit").click(function(e){
		e.preventDefault();
	    createSimpleFacet();
	});
	$("#changeOrder").click(function(e){
		e.preventDefault();
		switchOrder();
	});
	$("#addquery").click(function(e){
		e.preventDefault();
		addQuerySlot();
	});
	$("#queryaddsubmit").click(function(e){
		e.preventDefault();
	    createQueryFacet();
	});
});
//Get the table with the facets
function getExistingFacets(){
	$("#tbody").empty();		//Empty the table, useful if it's not the first call
	$.ajax({					//Ajax request to the doGet of the FacetConfig servlet
	        type: "GET",
	        url: "./../admin/FacetConfig",
	        //if received a response from the server
	        success: function( data, textStatus, jqXHR) {
	        	//If they're was an error print the error
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		//print it and disable the the button to change order
	        		document.getElementById("globalAnswer").innerHTML += "\n"+data.toString();
	        		$('#changeOrder').attr("disabled", true);
	        	}else if(data==="File already in use"){
	        		document.getElementById("fileAlready").innerHTML= window.i18n.msgStore["usedFile"];
	       		}else{
	        		//Add the lines to the table, and allow to click on the button to change the order of the facet.
	       			list=data;
	        		$('#changeOrder').attr("disabled", false);
	        		for(var i = 0; i < data.length ; i++){
	        			$("#tbody").append("<tr draggable=\"true\" ondrop=\"javascript : drop(event)\" ondragover=\"allowDrop(event)\" ondragstart=\"javascript : drag(event)\" \" id="+i+"><td id=\"id"+i+"\">"+(i+1)+"</td><td id=\"field"+i+"\">"+data.facet[i].field+"</td><td id=\"btn"+i+"\" class=\"btn-danger text-center\"style=\"background-color : #d9534f; width : 25px; position : relative;\"><a href=\"javascript: remove("+i+")\" style=\"color: #FFFFFF; position: absolute;top: 50%;left: 50%; text-decoration: inherit; -ms-transform: translate(-50%,-50%); -webkit-transform: translate(-50%,-50%); transform: translate(-50%,-50%);\"><i class=\"far fa-trash-alt\" ></i></a></td></tr>");
	        		}
	        		//Get the fields
	        		getFields();
	        	}
	        }
	 });
}
function getFields(){										
	$("#addselect").empty(); 
	$("#addqueryselect").empty();
	$.ajax({			//Ajax request to the doGet of the FieldWeight servlet
	        type: "GET",
	        url: "./../admin/FieldWeight",
	        //if received a response from the server
	        success: function( data, textStatus, jqXHR) {	
	        	//If they're was an error print the error
	        	if(data.toString().indexOf("Error code : ")!==-1){
	        		//print it and disable the selection
	        		document.getElementById("globalAnswer").innerHTML = data.toString();
	        		$('#addselect').attr("disabled", true);
	        		$('#addqueryselect').attr("disabled", true);
	        	}else{		//else add the empty option to the selects
	        		$("#addselect").append("<OPTION></OPTION>");
    				$("#addqueryselect").append("<OPTION></OPTION>");
    				//Comparing the fields with the exisiting facets
	        		for(var i = 0 ; i < data.field.length ; i++){
	        			var already = false;
	        			for(var j = 0 ; j < list.length ; j++){
	        				if(""+data.field[i].name+"" === ""+list.facet[j].field+""){
								already = true;
								break;
							}
	        			}
	        			//If this field is not already faceted, then add it to the selects
	        			if(!already){
	        				$("#addselect").append("<OPTION>"+data.field[i].name+"</OPTION>");
	        				$("#addqueryselect").append("<OPTION>"+data.field[i].name+"</OPTION>");
	        			}
	        		}
	        	}
	        }
	 });
}
//On select of a value, in the add select unlock the correct submit button
function activateSubmit(){
	if(document.getElementById("addselect").value!==""){
		$('#addsubmit').attr("disabled", false);
	}else{
		$('#addsubmit').attr("disabled", true);
	}
}
//On select of a value, in the query add select unlock the correct submit button
function activatequerySubmit(){
	if(document.getElementById("addqueryselect").value!==""){
		$('#queryaddsubmit').attr("disabled", false);
	}else{
		$('#queryaddsubmit').attr("disabled", true);
	}
}
//Create a Facet
function createSimpleFacet(){
	//Get the field, the name, if the facet has to be paginated and if it allows multiple selection.
	var datastring = "field="+document.getElementById("addselect").value+"&pagination="+document.getElementById("pagination").checked+"&selectType="+document.getElementById("selectType").checked;
	datastring = datastring+"&enName="+document.getElementById("enName").value+"&frName="+document.getElementById("frName").value;
	$.ajax({			//Ajax request to the doPost of the FacetConfig servlet
        type: "POST",
        url: "./../admin/FacetConfig",
        data: datastring,  
        //if received a response from the server
        success: function( data, textStatus, jqXHR) {
        	if(data.toString().indexOf("Error code : ")!==-1){
        		//print it and disable the selection
	        	document.getElementById("globalAnswer").innerHTML += "\n"+data.toString();
        		$('#addselect').attr("disabled", true);
        		$('#addsubmit').attr("disabled", true);
        	}else{
        		//Update the table, empty the input fields
        		getExistingFacets();
        		$('#addsubmit').attr("disabled", true);
        		document.getElementById("addselect").value="";
        		document.getElementById("enName").value="";
        		document.getElementById("frName").value="";
        		document.getElementById("frName").value="";
        		document.getElementById("pagination").checked=false;
        		document.getElementById("selectType").checked=false;
        	}
        }
	});
}
function createQueryFacet(){
	//Get basic parameters
	var datastring = "field="+document.getElementById("addqueryselect").value+"&pagination="+document.getElementById("queryPagination").checked+"&selectType="+document.getElementById("querySelectType").checked;
	datastring = datastring+"&enName="+document.getElementById("queryEnName").value+"&frName="+document.getElementById("queryFrName").value;
	var allElements = document.getElementsByTagName("*");
	var patt = /^query[0-9]*\b/;
	var pattbis = /^query[0-9]*Label[A-Za-z]{2}\b/; 
	//for each query slot and queryLabel slot
	for(var i = 0 ; i < allElements.length ; i ++){
		if(patt.test(allElements[i].id) || pattbis.test(allElements[i].id)){
			//Get the value
			datastring = datastring +"&"+allElements[i].id+"="+allElements[i].value
		}
	}
	$.ajax({			//Ajax request to the doPost of the FacetConfig servlet
        type: "POST",
        url: "./../admin/FacetConfig",
        data: datastring,  
        //if received a response from the server
        success: function( data, textStatus, jqXHR) {
        	if(data.toString().indexOf("Error code : ")!==-1){
        		//print it and disable the selection
	        	document.getElementById("globalAnswer").innerHTML += "\n"+data.toString();
        		$('#addselect').attr("disabled", true);
        		$('#addsubmit').attr("disabled", true);
        		$('#addqueryselect').attr("disabled", true);
        		$('#queryaddsubmit').attr("disabled", true);
        	}else if(data.toString()==="Not enough queries"){
        		document.getElementById("globalAnswer").innerHTML += "\n"+data.toString();
        		$('#queryaddsubmit').attr("disabled", true);
        		document.getElementById("addqueryselect").value="";
        		document.getElementById("enName").value="";
        		document.getElementById("frName").value="";
        	}else{
        		//Update the table, empty the input fields
        		getExistingFacets();
        		$('#queryaddsubmit').attr("disabled", true);
        		document.getElementById("addqueryselect").value="";
        		document.getElementById("enName").value="";
        		document.getElementById("frName").value="";
        		for(var i = 0 ; i < allElements.length ; i ++){
        			if(patt.test(allElements[i].id) || pattbis.test(allElements[i].id)){
        				allElements[i].value="";
        			}
        		}
        		document.getElementById("queryPagination").checked=false;
        		document.getElementById("querySelectType").checked=false;
        	}
        }
	});
}
//Add a query field and the fields for the i18n
function addQuerySlot(){
	i++;
	$("#queries").append("<div class=\"form-group\" id="+i+">");
	$("#"+i).append("<div class=\"col-sm-4\"><label class=\"control-label\" id=\"query"+i+"Legend\"></label><input type=\"text\" id=\"query"+i+"\"></div>");
	$("#"+i).append("<div class=\"col-sm-4\"><label class=\"control-label\" id=\"query"+i+"LabelLegendEn\"></label><input type=\"text\" id=\"query"+i+"LabelEn\"></div>");
	$("#"+i).append("<div class=\"col-sm-4\"><label class=\"control-label\" id=\"query"+i+"LabelLegendFr\"></label><input type=\"text\" id=\"query"+i+"LabelFr\"></div>");
	$("#"+i).append("</div>");
	document.getElementById("query"+i+"Legend").innerHTML = window.i18n.msgStore['queryNumber']+i+" :";
	document.getElementById("query"+i+"LabelLegendEn").innerHTML = window.i18n.msgStore['queryLabel']+" (en) :";
	document.getElementById("query"+i+"LabelLegendFr").innerHTML = window.i18n.msgStore['queryLabel']+" (fr) :";
}
//Remove a facet
function remove(i){
	$.ajax({			//Ajax request to the doPost of the FacetConfig servlet
        type: "POST",
        url: "./../admin/FacetConfig",
        data : "divName="+list.facet[i].div,
        //if received a response from the server
        success: function( data, textStatus, jqXHR) {
        	if(data.toString().indexOf("Error code : ")!==-1){
        		//print it and disable the selection
        		document.getElementById("globalAnswer").innerHTML = data.toString();
        		$('#addselect').attr("disabled", true);
        		$('#addsubmit').attr("disabled", true);
        		$('#addqueryselect').attr("disabled", true);
        		$('#queryaddsubmit').attr("disabled", true);
        	}else{
				//Get the new List of facet	
        		getExistingFacets();
        	}        
        }
	});
}
//managing drag and drop to modify the order of the facets
function allowDrop(ev) {
    ev.preventDefault();
}

function drag(ev) {
    dragSrcEl = ev.target;
    //ev.dataTransfer.effectAllowed = 'move';
	ev.dataTransfer.setData("text/html", ev.target.innerHTML);
}
function drop(ev) {
    ev.preventDefault();
    if (dragSrcEl !== ev.target.parentNode) {
        // Set the source column's HTML to the HTML of the column we dropped on.
        document.getElementById(dragSrcEl.id).innerHTML = ev.target.parentNode.innerHTML;
       	ev.target.parentNode.innerHTML = ev.dataTransfer.getData('text/html');       
    }
}
//modify the order of the divs in the jsp
function switchOrder(){
	//Get the current order
	var data="";
	var newOrder = document.getElementById("tbody").childNodes;
	for(var i = 0 ; i < newOrder.length ; i++){
		if(i!==0)
			data+="&";
		data += i+"="+newOrder[i].cells[1].innerText;
	}
	$.ajax({			//Ajax request to the doPost of the FacetConfig servlet
        type: "POST",
        url: "./../admin/FacetConfig",
        data : data,
        //if received a response from the server
        success: function( data, textStatus, jqXHR) {
        	if(data.toString().indexOf("Error code : ")!==-1){
        		//print it and disable the selection
        		document.getElementById("globalAnswer").innerHTML += "\n"+data.toString();
        		$('#addselect').attr("disabled", true);
        		$('#addsubmit').attr("disabled", true);
        		$('#addqueryselect').attr("disabled", true);
        		$('#queryaddsubmit').attr("disabled", true);
        	}else{
            	getExistingFacets();
        	}        
        }
	});
}
//Clean the semaphore
function cleanSem(){
	$.ajax({			//Ajax request to the doGet of the FacetConfig servlet
        type: "GET",
        url: "./../admin/FacetConfig",
        data : "sem=sem",
        //if received a response from the server
        success: function( data, textStatus, jqXHR) {
        	//If they're was an error print the error
        	if(data.toString().indexOf("Error code : ")!==-1){
        		//print it and disable the selection
        		document.getElementById("globalAnswer").innerHTML += "\n"+data.toString();
        	}
        }
	});
}