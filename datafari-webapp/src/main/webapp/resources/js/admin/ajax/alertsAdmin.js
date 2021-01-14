//# sourceURL=/Datafari/resources/js/admin/ajax//alertsAdmin.js

$(document).ready(function() {

  // Init toggle buttons
  $('#activated').bootstrapToggle();

  doGet();
  var icons = {
    header: "ui-icon-circle-arrow-e",
    activeHeader: "ui-icon-circle-arrow-s"
  };
  var totalHeight = document.getElementById("box").scrollHeight;
  // document.getElementById("hints").setAttribute("style","height:"+totalHeight+"px;");
  // document.getElementById("hint1").setAttribute("style","border:1px solid #ccc; margin-top : "+totalHeight/6+"px;");
  document.getElementById("labelHint1").innerHTML = window.i18n.msgStore['hint1'];
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-AlertAdmin'];
  document.getElementById("boxname").innerHTML = "\r" + window.i18n.msgStore['alertAdmin'];
  document.getElementById("HourlyLabel").innerHTML = window.i18n.msgStore['hourly'];
  document.getElementById("DailyLabel").innerHTML = window.i18n.msgStore['daily'];
  document.getElementById("WeeklyLabel").innerHTML = window.i18n.msgStore['weekly'];
  document.getElementById("delayLegend").innerHTML = window.i18n.msgStore['alertsDelay'];
  document.getElementById("paramRegtext").innerHTML = window.i18n.msgStore['paramReg'];
  document.getElementById("documentation-alerstadmin").innerHTML = window.i18n.msgStore['documentation-alerstadmin'];
  $('#paramRegLabel').html(window.i18n.msgStore['paramRegText']);
  $('#paramRegEmail').html(window.i18n.msgStore['paramRegEmail']);

  //SMTP i18n
  $("#mailLegend").html(window.i18n.msgStore["mailConf"]);
  $("#SMTPPortLabel").html(window.i18n.msgStore["smtp-port"]);
  $("#SMTPSecurityLabel").html(window.i18n.msgStore["security"]);
  $("#smtp-security-none").html(window.i18n.msgStore["none"]);
  $("#AddressLabel").html(window.i18n.msgStore["address"]);
  $("#UserLabel").html(window.i18n.msgStore["username"]);
  $("#PassLabel").html(window.i18n.msgStore["pass"]);
  $("#ConfirmPassLabel").html(window.i18n.msgStore["confirmPassword"]);
  
  $("#smtp-tip").attr("title", window.i18n.msgStore["smtp-tip"]);
  $("#smtp-port-tip").attr("title", window.i18n.msgStore["smtp-port"]);
  $("#smtp-security-tip").attr("title", window.i18n.msgStore["smtp-security-tip"]);
  $("#smtp-address-tip").attr("title", window.i18n.msgStore["address-tooltip"]);
  $("#username-tip").attr("title", window.i18n.msgStore["user-tooltip"]);
  $("#password-tip").attr("title", window.i18n.msgStore["smtp-password-tip"]);
  $("#password-confirmation-tip").attr("title", window.i18n.msgStore["smtp-password-tip"]);

  // Creates date pickers et get current dates
  $('#HourlyDelay').datetimepicker({
    dateFormat: 'dd/mm/yy/ '
  });
  $('#DailyDelay').datetimepicker({
    dateFormat: 'dd/mm/yy/ '
  });
  $('#WeeklyDelay').datetimepicker({
    dateFormat: 'dd/mm/yy/ '
  });
  // Remove the div of selection, useful on second load of date pickers
  $("a").click(function() {
    if (document.getElementById("ui-datepicker-div") !== null) {
      var element = document.getElementById("ui-datepicker-div");
      element.parentNode.removeChild(element);
    }
  });

  $("#sendMail").click(function() {
    $.ajax({ // Ajax request to the doGet of the Alerts servlet
      type: "POST",
      url: "./../admin/alertsAdmin",
      data: { "test_mail": $("#testAddress").val() },
      // if received a response from the server
      success: function(data, textStatus, jqXHR) {
        if (data.code == 0) {
          $("#smtpTestResult").addClass("success");
          $("#smtpTestResult").html("No error were triggered. Check your mail box to confirm you received the test mail");
        } else {
          $("#smtpTestResult").addClass("fail");
          $("#smtpTestResult").html(data.status);
        }

      },
      error: function(jqXHR, textStatus, errorThrown) {
        $("#smtpTestResult").addClass("fail");
        $("#smtpTestResult").html("Send process failed, check the logs to see the detailed exceptions");
      },
      // capture the request before it was sent to server
      beforeSend: function(jqXHR, settings) {
        $("#sendMail").loading("loading");
        $("#smtpTestResult").empty();
        $("#smtpTestResult").removeClass("success");
        $("#smtpTestResult").removeClass("fail");
      },
      // this is called after the response or error functions are finished
      complete: function(jqXHR, textStatus) {
        // enable the button
        $("#sendMail").loading("reset");
      }
    });
  });
});

// ***** Inputs verification and status management ***** //
function clearStatus(element) {
  element.removeClass("is-valid");
  element.removeClass("is-invalid");
  let parent = element.closest(".form-group");
  parent.removeClass("was-validated");
};

function setOkStatus(element) {
  element.addClass("is-valid");
};

function setErrorStatus(element, errorMsg) {
  if (errorMsg !== null) {
    element.siblings(".invalid-feedback").html(errorMsg);
  }
  element.addClass("is-invalid");
}

function checkPasswords() {
  clearStatus($("#ConfirmPass"));
  var password = $("#Pass").val();
  var confirmPassword = $("#ConfirmPass").val();
  if (password !== confirmPassword) {
    setErrorStatus($("#ConfirmPass"));
    return false;
  } else {
    return true;
  }
}

function doGet() {
  $("#errorPrint").empty();
  $
    .ajax({ // Ajax request to the doGet of the AlertsAdmin servlet
      type: "GET",
      url: "./../admin/alertsAdmin",
      // if received a response from the server
      success: function(data, textStatus, jqXHR) {
        $("#prevNext").empty();
        if (data.toString().indexOf("Error code : ") !== -1) {
          $("#errorPrint").append("<label>" + data.toString() + "</label>");
          $('#activated').bootstrapToggle('disable');
          document.getElementById("paramReg").disabled = true;
        } else {
          $("#prevNext").append("<fieldset id=\"field1\">");
          $("#field1").append(
            "<legend id=\"prevLegend\">" + window.i18n.msgStore["previousExecution"] + "</legend><div class=\"row\"><label id=prevHourlyLabel class=\"col-sm-3 col-form-label\">"
            + window.i18n.msgStore["hourly"] + "</label><input type=\"text\" class=\"col-sm-2\" id=\"prevHourly\"  style=\"min-width : 150px;\"disabled value=" + "\"" + data.hourly + "\""
            + "></div>");
          $("#field1").append(
            "<div class=\"row\"><label id=prevDailyLabel class=\"col-sm-3 col-form-label\">" + window.i18n.msgStore["daily"]
            + "</label><input type=\"text\" class=\"col-sm-2\" id=\"prevDaily\" style=\"min-width : 150px;\" disabled value=" + "\"" + data.daily + "\"" + "></div>");
          $("#field1").append(
            "<div class=\"row\"><label id=prevWeeklyLabel class=\"col-sm-3 col-form-label\">" + window.i18n.msgStore["weekly"]
            + "</label><input type=\"text\" class=\"col-sm-2\" id=\"prevWeekly\" style=\"min-width : 150px;\" disabled value=" + "\"" + data.weekly + "\"" + "></div>");
          $("#prevNext").append("</fieldset><fieldset id=\"field2\" class=\"form-group\">");
          // $("#field2").append("<legend id=\"nextLegend\">"+window.i18n.msgStore["nextExecution"]+"<span class=\"fa fa-asterisk \"
          // style=\"color : red\"></span></legend><div class=\"form-group\"><label id=nextHourlyLabel class=\"col-sm-3
          // control-label\">"+window.i18n.msgStore["hourly"]+"</label><input type=\"text\" class=\"col-sm\" id=\"nextHourly\"
          // style=\"min-width : 150px;\" disabled value="+"\""+data.nextHourly+"\""+"></div>");
          $("#field2").append(
            "<legend id=\"nextLegend\">" + window.i18n.msgStore["nextExecution"] + "</legend><div class=\"row\"><label id=nextHourlyLabel class=\"col-sm-3 col-form-label\">"
            + window.i18n.msgStore["hourly"] + "</label><input type=\"text\" class=\"col-sm-2\" id=\"nextHourly\" style=\"min-width : 150px;\" disabled value=" + "\"" + data.nextHourly + "\""
            + "></div>");
          $("#field2").append(
            "<div class=\"row\"><label id=nextDailyLabel class=\"col-sm-3 col-form-label\">" + window.i18n.msgStore["daily"]
            + "</label><input type=\"text\" class=\"col-sm-2\" id=\"nextDaily\" style=\"min-width : 150px;\" disabled value=" + "\"" + data.nextDaily + "\"" + "></div>");
          $("#field2").append(
            "<div class=\"row\"><label id=nextWeeklyLabel class=\"col-sm-3 col-form-label\">" + window.i18n.msgStore["weekly"]
            + "</label><input type=\"text\" class=\"col-sm-2\" id=\"nextWeekly\" style=\"min-width : 150px;\" disabled value=" + "\"" + data.nextWeekly + "\"" + "></div>");
          // $("#field2").append("<div class=\"control-label\"><i class=\"fa fa-asterisk\" style=\"color : red\"></i><label
          // class=\"control-label\">"+window.i18n.msgStore['takingAccount']+"</label></div>");

          if (data.on === "on") { // Set the button
            $("#activated").bootstrapToggle('on', true);
          } else {
            $("#activated").bootstrapToggle('off', true);
          }
          $("#prevNext").append("</fieldset>");

          $("#prevNext").append
          // Set the parameters
          document.getElementById("HourlyDelay").value = format(data.hourlyDate);
          document.getElementById("DailyDelay").value = format(data.dailyDate);
          document.getElementById("WeeklyDelay").value = format(data.weeklyDate);

          // SMTP conf

          $("#SMTPPort").val(data.smtp_port);
          $("#SMTPSecurity").val(data.smtp_security);
          $("#Address").val(data.from);
          $("#UserName").val(data.user);
          $("#Pass").val(data.pass);
          $("#ConfirmPass").val(data.pass);
        }
        var totalHeight = document.getElementById("box").scrollHeight;
        var margin = (totalHeight / 2);
      }
    });
}

function format(Date) {
  return Date.substring(0, 10) + "/  " + Date.substring(11, 16);
}

function onOff() {
  var activated;
  if ($("#activated").is(':checked')) {
    activated = "on";
  } else {
    activated = "off";
  }
  // var data = "activated=" + document.getElementById("activated").innerHTML
  $.post("./../admin/alertsAdmin", {
    activated: activated
  }, function(data, textStatus, jqXHR) {
    doGet();
  });

}

function parameters() {
  if (!checkPasswords()) {
    return;
  }
  var data = "HOURLYDELAY=" + document.getElementById("HourlyDelay").value + "&DAILYDELAY=" + document.getElementById("DailyDelay").value + "&WEEKLYDELAY="
    + document.getElementById("WeeklyDelay").value + "&smtp=" + document.getElementById("SMTP").value + "&smtp_port=" + document.getElementById("SMTPPort").value + "&smtp_security=" + $("#SMTPSecurity").val() + "&from="
    + document.getElementById("Address").value + "&user=" + document.getElementById("UserName").value + "&pass=" + document.getElementById("Pass").value;
  if (data.indexOf("=&") != -1) {
    alert(window.i18n.msgStore['missingParameter']);
  }
  $.ajax({ // Ajax request to the doGet of the Alerts servlet
    type: "POST",
    url: "./../admin/alertsAdmin",
    data: data,
    // if received a response from the server
    success: function(data, textStatus, jqXHR) {
      if (data.toString().indexOf("Error code : ") !== -1) {
        $("#errorPrint").append("<label>" + data.toString() + "</label>");
        $('#activated').bootstrapToggle('disable');
        document.getElementById("paramReg").disabled = true;
      }
      document.getElementById("parameterSaved").innerHTML = window.i18n.msgStore["parameterSaved"];
      doGet();
    },
    error: function(jqXHR, textStatus, errorThrown) {
      console.log("Something really bad happened " + textStatus);
      $("#ajaxResponse").html(jqXHR.responseText);
    },
    // capture the request before it was sent to server
    beforeSend: function(jqXHR, settings) {
      // disable the button until we get the response
      $('#add').attr("disabled", true);
      $("#parameterSaved").empty();
    },
    // this is called after the response or error functions are finished
    complete: function(jqXHR, textStatus) {
      // enable the button
      $('#add').attr("disabled", false);
    }
  });
}
