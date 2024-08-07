//# sourceURL=/Datafari/resources/js/admin/ajax//autocompleteConfiguration.js

$(document).ready(function() {
  // Internationalize content
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-AutocompleteConfig'];
  document.getElementById("labelth").innerHTML = window.i18n.msgStore['labelth'] + " : ";
  document.getElementById("submitth").innerHTML = window.i18n.msgStore['confirm'];
  document.getElementById("documentation-autocompleteconfiguration").innerHTML = window.i18n.msgStore['documentation-autocompleteconfiguration'];
  $('#submitth').attr("data-loading-text", "<i class='fa fa-spinner fa-spin'></i> " + window.i18n.msgStore['confirm']);
  document.getElementById("thname").innerHTML = window.i18n.msgStore['limitTH'];
  $('#autocompleteConfirm').html(window.i18n.msgStore['adminUI-Confirm']);
  // Disable the input and submit
  $('#submitth').attr("disabled", true);
  $('#maxth').attr("disabled", true);
  // If the semaphore was for this page and the user leaves it release the semaphores
  // On refresh
  $(window).bind('beforeunload', function() {
    if (document.getElementById("submitth") !== null) {
      if (!document.getElementById("submitth").getAttribute('disabled')) {
        cleanSem("threshold");
      }
    }
  });
  // If the user loads an other page
  $("a").click(function(e) {
    if (e.target.className === "ajax-link" || e.target.className === "ajax-link active-parent active") {
      if (document.getElementById("submitth") !== null) {
        if (!document.getElementById("submitth").getAttribute('disabled')) {
          cleanSem("threshold");
        }
      }
    }
  });
  // Get threshold value
  $.get('../GetAutocompleteThreshold', function(data) {
    if (data.code == 0) {
      document.getElementById("autocompleteThreshold").value = data.autoCompleteThreshold;
      $('#submitth').attr("disabled", false);
      $('#autoCompleteThreshold').attr("disabled", false);
    } else {
      document.getElementById("globalAnswer").innerHTML = data;
      $('#submitth').attr("disabled", true);
      $('#autoCompleteThreshold').attr("disabled", true);
    }
  }, "json");
  // Sert the button to call the function set with the threshold parameter
  $("#submitth").click(function(e) {
    e.preventDefault();
    if (checkThreshold()) {
      $("#submitth").loading("loading");
      $.post('./SetAutocompleteThreshold', {
        autocompleteThreshold : document.getElementById("autocompleteThreshold").value
      }, function(data) {
        if (data.code == 0) {
          document.getElementById("answerth").innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
          $("#answerth").addClass("success");
          $("#answerth").fadeOut(8000, function() {
            clearStatus($("#autocompleteThreshold"));
            $("#answerth").removeClass("success");
            $("#answerth").html("");
            $("#answerth").show();
          });
        } else {
          document.getElementById("globalAnswer").innerHTML = data;
          $('#submitth').attr("disabled", true);
          $('#autocompleteThreshold').attr("disabled", true);
        }
        $("#submitth").loading("reset");
      }, "json");
    }
  });
});

function clearStatus(element) {
  element.removeClass("is-valid");
  element.removeClass("is-invalid");
  let parent = element.closest(".form-group");
  parent.removeClass("was-validated");
};

function setOkStatus(element, validMsg) {
  element.addClass("is-valid");
  if (validMsg !== undefined && validMsg !== null) {
    element.siblings(".valid-feedback").html(validMsg);
  }
};

function setErrorStatus(element, errorMsg) {
  if (errorMsg !== null && errorMsg !== undefined) {
    element.siblings(".invalid-feedback").html(errorMsg);
  }
  element.addClass("is-invalid");
}

function checkThreshold() {
  var element = $("#autocompleteThreshold");
  clearStatus(element);
  if (!$(element).val()) {
    setErrorStatus(element, window.i18n.msgStore['provide-threshold']);
    return false;
  } else {
    let autocompleteThreshold = element.val();
    if (autocompleteThreshold >= 0 && autocompleteThreshold <= 1) {
      setOkStatus(element);
      return true;
    } else {
      if (autocompleteThreshold < 0) {
        setErrorStatus(element, window.i18n.msgStore['autocomplete-threshold-must-be-positive']);
      } else {
        setErrorStatus(element, window.i18n.msgStore['autocomplete-threshold-must-be-lower']);
      }
      return false;
    }
  }
}

function get(type) {
  var typ = type.substring(0, 2);
  document.getElementById("max" + typ).value = "";
  $.ajax({ // Ajax request to the doGet of the ModifyNodeContent servlet
    type : "GET",
    url : "./../admin/ModifyNodeContent",
    data : "type=" + type + "&attr=name",
    // if received a response from the server
    success : function(data, textStatus, jqXHR) {
      // If the semaphore was already acquired
      if (data === "File already in use") {
        // Print it and disable the input and submit
        document.getElementById("answer" + typ).innerHTML = window.i18n.msgStore['usedFile'];
        $('#submit' + typ).attr("disabled", true);
        $('#max' + typ).attr("disabled", true);
      }// If they're was an error
      else if (data.toString().indexOf("Error code : ") !== -1) {
        // print it and disable the input and submit
        document.getElementById("globalAnswer").innerHTML = data;
        $('#submit' + typ).attr("disabled", true);
        $('#max' + typ).attr("disabled", true);
      } else { // else add the options to the select
        document.getElementById("max" + typ).value = data;
        $('#submit' + typ).attr("disabled", false);
        $('#max' + typ).attr("disabled", false);
      }
    }
  });
}
function set(type) {
  $("#submitth").loading("loading");
  var typ = type.substring(0, 2);
  var value = document.getElementById("max" + typ).value;
  if (value <= 1 && value >= 0) {
    $.ajax({ // Ajax request to the doGet of the ModifyNodeContent servlet to modify the solrconfig file
      type : "POST",
      url : "./../admin/ModifyNodeContent",
      data : "type=" + type + "&value=" + value + "&attr=name",
      // if received a response from the server
      success : function(data, textStatus, jqXHR) {
        // If the semaphore was already acquired
        if (data === "File already in use") {
          // Print it and disable the input and submit
          document.getElementById("answer" + typ).innerHTML = window.i18n.msgStore['usedFile'];
          $('#submit' + typ).attr("disabled", true);
          $('#max' + typ).attr("disabled", true);
        }// If they're was an error
        else if (data.toString().indexOf("Error code : ") !== -1) {
          // print it and disable the input and submit
          document.getElementById("globalAnswer").innerHTML = data;
          $('#submit' + typ).attr("disabled", true);
          $('#max' + typ).attr("disabled", true);
        } else { // else add the options to the select
          document.getElementById("answer" + typ).innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
          $("#answer" + typ).addClass("success");
          $("#answer" + typ).fadeOut(8000, function() {
            $("#answer" + typ).removeClass("success");
            $("#answer" + typ).html("");
            $("#answer" + typ).show();
          });
        }
      },
      // this is called after the response or error functions are finsihed
      complete : function(jqXHR, textStatus) {
        // enable the button
        $("#submitth").loading("reset");
      }
    });
  } else {
    document.getElementById("answer" + typ).innerHTML = window.i18n.msgStore['inputMust'];
  }
}
function cleanSem(type) {
  $.ajax({ // Ajax request to the doGet of the ModifyNodeContent servlet to release the semaphore
    type : "GET",
    url : "./../admin/ModifyNodeContent",
    data : "sem=sem&type=" + type
  });
}