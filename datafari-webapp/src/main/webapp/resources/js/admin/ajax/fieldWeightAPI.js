//# sourceURL=/Datafari/resources/js/admin/ajax//fieldWeightAPI.js


$(document).ready(function() {
    //Internationalize content
    document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
    document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
    document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-FieldWeight'];
    document.getElementById("documentation-fieldweightapi").innerHTML = window.i18n.msgStore['documentation-fieldweightapi'];
    document.getElementById("labelth").innerHTML = window.i18n.msgStore['qf']+" : ";
    document.getElementById("labelth2").innerHTML = window.i18n.msgStore['pf']+" : ";
    $('#labelth3').html(window.i18n.msgStore['boost']
        + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Only one parameter can be added here. If you need more that one Boost, consider editing Solr configuration.'>i</button></span> :");
    $('#labelth4').html(window.i18n.msgStore['bq']
        + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Only one parameter can be added here. If you need more that one Boost Query, consider editing Solr configuration.'>i</button></span> :");
    $('#labelth5').html(window.i18n.msgStore['bf']
        + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Only one parameter can be added here. If you need more that one Boost Function, consider editing Solr configuration.'>i</button></span> :");
    document.getElementById("submitth").innerHTML = window.i18n.msgStore['confirm'];
    document.getElementById("submittab").innerHTML = window.i18n.msgStore['confirm'];
    document.getElementById("addRow").innerHTML = window.i18n.msgStore['addField'];
    document.getElementById("name").innerHTML = window.i18n.msgStore['field'];
    document.getElementById("type").innerHTML = window.i18n.msgStore['fieldWeight'];
    $('#fieldWeightAdd').html(window.i18n.msgStore['adminUI-FieldWeightAdd']
        + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Click add allows you to add new fields to the the weighted search algorithm'>i</button></span>");
    $('#fieldWeightConfirm').html(window.i18n.msgStore['adminUI-FieldWeightConfirm']
        + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Once clicked, it is taken into account immediately'>i</button></span>");
    document.getElementById("explanationsText").innerHTML = window.i18n.msgStore['adminUI-FieldWeight-Explanations'];
    $('#submitth').attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);
    document.getElementById("thname").innerHTML = window.i18n.msgStore['fieldWeightExpert']
        + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='The form below allows you to quickly edit the pf and the qf values directly by a text field. Be careful to respect the syntax : field^weight and a blank space between the different fields for example : title_en^50 content_en10'>i</button></span>";
    document.getElementById("thname2").innerHTML = window.i18n.msgStore['adminUI-FieldWeight']
        + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='This table lets you choose all of the fields you want your query to be based on, and their associated weigh. It corresponds to the qf parameter'>i</button></span>";
    $('#fieldWeightExpertConfirm').html(window.i18n.msgStore['adminUI-FieldWeightConfirm']
        + "<span><button type='button' class='btn btn-secondary tooltips' data-toggle='tooltip' data-placement='right' title='Once clicked, it is taken into account immediately'>i</button></span>");

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
            document.getElementById("boostAPI").value = data.boostAPI;
            document.getElementById("bqAPI").value = data.bqAPI;
            document.getElementById("bfAPI").value = data.bfAPI;
            $('#submitth').attr("disabled", false);
            $('#qfAPI').attr("disabled", false);
            var qfAPI = data.qfAPI;
            qfAPI = qfAPI.trim();
            var words = qfAPI.split(' ');
            for(var i = 0 ; i < qfAPI.length ; i++){
                if((words[i]!=undefined) && (!words[i].startsWith("code"))) {
                    var splitWord = words[i].split('^');


                    $("#tbody").append("<tr id='"+i+"'><th>"+splitWord[0]+"</th><th><input type='text'  id='qf"+splitWord[0]+"' name='qfAPI' value='"+splitWord[1]+"'></th><th class=\"btn-danger text-center\"style=\"background-color : #d9534f; position : relative;\"><a href=\"javascript: remove("+i+")\" style=\"color: #FFFFFF; position: absolute;top: 50%;left: 50%; text-decoration: inherit; -ms-transform: translate(-50%,-50%); -webkit-transform: translate(-50%,-50%); transform: translate(-50%,-50%);\"><i class=\"far fa-trash-alt\" ></i></a></th>");

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

        $.post('./FieldWeightAPI', {qfAPI : document.getElementById("qfAPI").value, pfAPI : document.getElementById("pfAPI").value,
                boostAPI : document.getElementById("boostAPI").value, bqAPI : document.getElementById("bqAPI").value, bfAPI : document.getElementById("bfAPI").value}, function(data) {
            if(data.code == 0) {
                document.getElementById("answerth").innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
                var qfAPI = document.getElementById("qfAPI").value;
                qfAPI = qfAPI.trim();
                var words = qfAPI.split(' ');
                $("#tbody").empty();
                for(var i = 0 ; i < qfAPI.length ; i++){
                    if((words[i]!=undefined) && (!words[i].startsWith("code"))) {
                        var splitWord = words[i].split('^');


                        $("#tbody").append("<tr id='"+i+"'><th>"+splitWord[0]+"</th><th><input type='text'  id='qf"+splitWord[0]+"' name='qfAPI' value='"+splitWord[1]+"'></th><th class=\"btn-danger text-center\"style=\"background-color : #d9534f; position : relative;\"><a href=\"javascript: remove("+i+")\" style=\"color: #FFFFFF; position: absolute;top: 50%;left: 50%; text-decoration: inherit; -ms-transform: translate(-50%,-50%); -webkit-transform: translate(-50%,-50%); transform: translate(-50%,-50%);\"><i class=\"far fa-trash-alt\" ></i></a></th>");

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
                
                
            if ( (!(row.getElementsByTagName("th")[0].innerHTML.startsWith("<input"))) && (row.getElementsByTagName("th")[0] != "") && (row.getElementsByTagName("th")[0] != undefined))  {
                tempQF = row.getElementsByTagName("th")[0].innerHTML;
            }
            currentTh = row.getElementsByTagName("th");
            
            for(j=0; j<currentTh.length; j++) {
                
                if (currentTh[j].getElementsByTagName("input")[0]) {
                    
                    if ((currentTh[j].getElementsByTagName("input")[0].value != "")&& (currentTh[j].getElementsByTagName("input")[0].value != undefined)){
                        if (tempQF != ""){
                            tempQF= tempQF+"^"+currentTh[j].getElementsByTagName("input")[0].value;
                        }
                        else {
                            tempQF= currentTh[j].getElementsByTagName("input")[0].value;
                        }
                    }
                    
                }
            }
            
            if ( (tempQF.match("^[a-zA-Z]")) && (tempQF.match("[0-9]$"))){
                
            }
            else {
                alert(window.i18n.msgStore['adminUI-FieldWeight-Error']);
                return;
            }
            newQF= newQF+" "+tempQF;
            newQF = newQF.trim();

        }
        

        $.post('./FieldWeightAPI', {qfAPI : newQF, pfAPI : document.getElementById("pfAPI").value,
                boostAPI : document.getElementById("boostAPI").value, bqAPI : document.getElementById("bqAPI").value, bfAPI : document.getElementById("bfAPI").value}, function(data) {
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

        $.post('./FieldWeightAPI', {qfAPI : document.getElementById("qfAPI").value, pfAPI : document.getElementById("pfAPI").value,
                boostAPI : document.getElementById("boostAPI").value, bqAPI : document.getElementById("bqAPI").value, bfAPI : document.getElementById("bfAPI").value }, function(data) {
            if(data.code == 0) {
                document.getElementById("answerth").innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
                var qfAPI = document.getElementById("qfAPI").value;
                qfAPI = qfAPI.trim();
                var words = qfAPI.split(' ');
                $("#tbody").empty();
                for(var i = 0 ; i < qfAPI.length ; i++){
                    if((words[i]!=undefined) && (!words[i].startsWith("code"))) {
                        var splitWord = words[i].split('^');


                        $("#tbody").append("<tr id='"+i+"'><th>"+splitWord[0]+"</th><th><input type='text'  id='qf"+splitWord[0]+"' name='qfAPI' value='"+splitWord[1]+"'></th>");

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
        

        var  tab=document.getElementById("tableau");

        rws=tab.getElementsByTagName('TR');
        var tabLastId = rws[rws.length-1].id;
        var tabid = Number(tabLastId)+1;

        

        $("#tbody").append("<tr id='"+tabid+"'><th><input type='text' name='newRowField"+tabid+"' id='newRowField"+tabLength+"' value=''></th><th><input type='text' id='newRowValue"+tabid+"'  name='newRowValue"+tabid+"' value='0'></th><th class=\"btn-danger text-center\"style=\"background-color : #d9534f; position : relative;\"><a href=\"javascript: remove("+tabid+")\" style=\"color: #FFFFFF; position: absolute;top: 50%;left: 50%; text-decoration: inherit; -ms-transform: translate(-50%,-50%); -webkit-transform: translate(-50%,-50%); transform: translate(-50%,-50%);\"><i class=\"far fa-trash-alt\" ></i></a></th>");
    });





});

function myFunction(b) {
    document.getElementById("myText").value = "black";
}

function remove(i){
    var line = document.getElementById(i);
    line.parentNode.removeChild(line);
}


function get(type){
    var typ = type.substring(0,2);
    document.getElementById("max"+typ).value = "";
    $.ajax({            //Ajax request to the doGet of the ModifyNodeContent servlet
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
            }else{      //else add the options to the select
                document.getElementById("max"+typ).value = data;    
                $('#submit'+typ).attr("disabled", false);
                $('#max'+typ).attr("disabled", false);
            }
        }
    });
}
function set(type){
    $("#submitth").loading("loading");
    var typ = type.substring(0,2);
    var value = document.getElementById("max"+typ).value;
    if(value<=1 && value>=0){
        $.ajax({            //Ajax request to the doGet of the ModifyNodeContent servlet to modify the solrconfig file
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
                }else{      //else add the options to the select
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
                $("#submitth").loading("reset");
            }
        });
    }else{
        document.getElementById("answer"+typ).innerHTML = window.i18n.msgStore['inputMust'];
    }
}
function cleanSem(type){
    $.ajax({            //Ajax request to the doGet of the ModifyNodeContent servlet to release the semaphore
        type: "GET",
        url: "./../admin/ModifyNodeContent",
        data : "sem=sem&type="+type
    });
}