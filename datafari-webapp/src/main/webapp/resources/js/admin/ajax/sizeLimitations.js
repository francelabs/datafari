//# sourceURL=/Datafari/resources/js/admin/ajax/sizeLimitations.js

$(document).ready(function() {
  // Internationalize content
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore['home'];
  document.getElementById("topbar2").innerHTML = window.i18n.msgStore['adminUI-SearchEngineAdmin'];
  document.getElementById("topbar3").innerHTML = window.i18n.msgStore['adminUI-SizeLimitation'];
  document.getElementById("labelhl").innerHTML = window.i18n.msgStore['labelhl'] + " : ";
  document.getElementById("submithl").innerHTML = window.i18n.msgStore['confirm'];
  document.getElementById("hlname").innerHTML = window.i18n.msgStore['limitHL'];
  document.getElementById("documentation-sizelimitations").innerHTML = window.i18n.msgStore['documentation-sizelimitations'];
  document.getElementById("size-limitations-confirm").innerHTML = window.i18n.msgStore['adminUI-SizeLimitationText'];
  // Disable the input and submit
  $('#submithl').attr("disabled", true);
  $('#maxhl').attr("disabled", true);
  // If the semaphore was for this page and the user leaves it release the semaphores
  // On refresh
  $(window).bind('beforeunload', function() {
    if (document.getElementById("submithl") !== null) {
      if (!document.getElementById("submithl").getAttribute('disabled')) {
        cleanSem("hl.maxAnalyzedChars");
      }
    }
  });

  // If the user loads an other page
  $("a").click(function(e) {
    if (e.target.className === "ajax-link" || e.target.className === "ajax-link active-parent active") {
      if (document.getElementById("submithl") !== null) {
        if (!document.getElementById("submithl").getAttribute('disabled')) {
          cleanSem("hl.maxAnalyzedChars");
        }
      }
    }
  });

  // Get hl.maxAnalyzedChars value
  $.get('../GetHighlightInfos', function(data) {
    if (data.code == 0) {
      document.getElementById("maxhl").value = data.maxAnalyzedChars;
      $('#submithl').attr("disabled", false);
      $('#maxhl').attr("disabled", false);
    } else {
      document.getElementById("globalAnswer").innerHTML = data;
      $('#submithl').attr("disabled", true);
      $('#maxhl').attr("disabled", true);
    }
  }, "json");

  // Sert the button to call the function set with the hl.maxAnalyzedChars parameter
  $("#submithl").click(function(e) {
    e.preventDefault();
    if (checkLimitation()) {
      $("#submithl").loading("loading");
      $.post('./SetHighlightInfos', {
        maxAnalyzedChars : document.getElementById("maxhl").value
      }, function(data) {
        if (data.code == 0) {
          document.getElementById("answerhl").innerHTML = window.i18n.msgStore['modifDoneImmediateEffect'];
          $("#answerhl").addClass("success");
          $("#answerhl").fadeOut(8000, function() {
            clearStatus($("#maxhl"));
            $("#answerhl").removeClass("success");
            $("#answerhl").html("");
            $("#answerhl").show();
          });
        } else {
          document.getElementById("globalAnswer").innerHTML = data;
          $('#submithl').attr("disabled", true);
          $('#maxhl').attr("disabled", true);
        }
        $("#submithl").loading("reset");
      }, "json");
    }
  });

  // If the user loads an other page
  $("a").click(function(e) {
    if (e.target.className === "ajax-link" || e.target.className === "ajax-link active-parent active") {
      if (document.getElementById("submitindexhl") !== null) {
        if (!document.getElementById("submitindexhl").getAttribute('disabled')) {
          cleanSem("maxLength");
        }
      }
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

function checkLimitation() {
  var element = $("#maxhl");
  clearStatus(element);
  if (!$(element).val()) {
    setErrorStatus(element, window.i18n.msgStore['provide-limitation-threshold']);
    return false;
  } else {
    let limitationThreshold = element.val();
    if (limitationThreshold >= 0) {
      setOkStatus(element);
      return true;
    } else {
      setErrorStatus(element, window.i18n.msgStore['limitation-threshold-must-be-positive']);
      return false;
    }
  }
}

function cleanSem(type) {
  $.ajax({ // Ajax request to the doGet of the ModifyNodeContent servlet to release the semaphore
    type : "GET",
    url : "./../admin/ModifyNodeContent",
    data : "sem=sem&type=" + type
  });
}
