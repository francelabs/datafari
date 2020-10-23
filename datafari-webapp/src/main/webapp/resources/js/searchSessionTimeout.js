var lastActiveTime = new Date().getTime();
var sessionDefaultTimeout = -1;
var logged = null; // logged username
var idleThreshold = 10 * 1000; // idle time under which the session is refreshed.
var checkInterval = 15 * 1000; // 15s
var shownError = false; // var that indicates if the user has already been informed that the connection with the server has been lost
var checkIntervalID = null; // stocks the id of the interval thread that checks the idle time
var retryCountdownIntervalID = null; // stocks the id of the interval thread that updates the countdown
var retryCptBaseVal = 15; // default retry cpt, expressed in seconds
var retryCpt = retryCptBaseVal; // current retry cpt
var retryNb = 0;
var samlEnabled = false;

$(document).ready(function() {

  // check the session once the page is loaded to retrieve the logged username and the sessionTiemout defined by the server
  $.ajax({
    url : './RefreshSession',
    dataType : "json",
    timeout : 1000,
    success : function(resp) {
      if (resp.code == 0) {
        logged = resp.user;
      }
      if (resp.sessionTimeout != undefined && resp.sessionTimeout != null) {
        sessionDefaultTimeout = resp.sessionTimeout;
      }
      if (resp.samlEnabled != undefined && resp.samlEnabled != null) {
        samlEnabled = resp.samlEnabled;
      }
    }
  });

  // on every user action on the page (click, scroll, mouse move, resize) update the lastActiveTime var
  $('html').bind('click mousemove keypress scroll resize', function() {
    // before updating the lastActiveTime, check how many milliseconds passed since the previous lastActiveTime value
    var diff = new Date().getTime() - lastActiveTime;
    lastActiveTime = new Date().getTime();
    // If the number of milliseconds passed since the previous lastActiveTime value is greater than the idleThreshold then force a session
    // refresh
    if (diff > idleThreshold && checkIntervalID !== null) {
      checkIdleTime();
    }
  });

  // initialize an interval thread to check the idle time (and resfresh the session if needed) every idleInterval milliseconds
  checkIntervalID = setInterval(checkIdleTime, checkInterval);
});

// Function that displays a countdown when the connection with the server has been lost
function retryCountdown() {
  // decrease the retry countdown cpt by 1
  retryCpt -= 1;
  $("#connection-info-header").html(
      "<i class='fas fa-exclamation-triangle'></i> " + window.i18n.msgStore['interrupted-connection'] + " " + window.i18n.msgStore['retry-in'] + " " + retryCpt
          + "s <span class='retry-button' onclick='retryNow()'>" + window.i18n.msgStore['retry-now'] + "</span>").show();
  $("#header-menus").addClass("info");
  // Once the countdown reaches 0, try to connect to the server and refresh the session
  if (retryCpt == 0) {
    retryNow();
  }
}

// Tries to refresh the session
function retryNow() {
  $("#connection-info-header").html(window.i18n.msgStore['retrying']);
  $("#header-menus").addClass("info");
  // Stop the interval thread that updates the countdown
  clearInterval(retryCountdownIntervalID);
  retryCountdownIntervalID = null;
  checkIdleTime();
}

function connectionSucceededReinit() {
  // connection succeeded so reset the shownError var so that if the connection broke again, the user will be re-noticed
  shownError = false;
  // connection succeeded so reset the retryNb var
  retryNb = 0;
  // connection succeeded so in case the check thread was stopped because of a broken connection, re-init it
  if (checkIntervalID == null) {
    checkIntervalID = setInterval(checkIdleTime, checkInterval);
  }
  // connection succeeded so in case a re-connection countdown was displayed to the user, set it to "Connection OK" and fade it out
  $("#connection-info-header").html(window.i18n.msgStore['connection-ok']).fadeOut(1000, function() {
    $("#header-menus").removeClass("info");
  });
}

function checkIdleTime() {
  var diff = new Date().getTime() - lastActiveTime;
  // if last activity was less than idleThreshold ago, or the sessionTimeout has been exceeded, or there is no check interval thread, then
  // send a refresh session request
  if (diff <= idleThreshold || checkIntervalID == null) {
    $.ajax({
      url : './RefreshSession',
      dataType : "json",
      timeout : 1000,
      success : function(resp) {
        connectionSucceededReinit();
        if (resp.code !== 0 && logged != null) {
          // resp code !== 0 means not logged, so if the logged var is not null , it means we currently think we are logged, so need to
          // reload the page to be unlogged
          window.location.reload();
        } else {
          // resp code == 0 means logged, so if the logged var null , it means we currently think we are not logged, so need to
          // reload the page to be logged
          // if the logged var is not equals to the resp.user it means we are not the user what we think we are, so reload the page
          if (resp.code == 0 && logged == null || resp.code == 0 && logged !== resp.user) {
            window.location.reload();
          }
          lastActiveTime = new Date().getTime();
        }
        if (resp.sessionTimeout != undefined && resp.sessionTimeout != null) {
          sessionDefaultTimeout = resp.sessionTimeout;
        }
        if (resp.samlEnabled != undefined && resp.samlEnabled != null) {
          samlEnabled = resp.samlEnabled;
        }
      },
      error : function(request, status, err) {
        if (status !== "timeout") {
          if (request.status === 401 || (request.responseText != undefined && request.responseText.indexOf("session has been expired") !== -1) || (request.status === 0 && samlEnabled)) {
            window.location.reload();
          }
          retryNb++;
          retryCpt = retryCptBaseVal * retryNb;
          // Error is not due to a timeout (which indicates the server is restarting), so it means the connection with the server has been
          // lost
          // if the user has not already been informed, display a modal to inform him that the connection has been lost
          if (!shownError) {
            // If the infoModal div is present then display it
            if ($("#infoModal").length) {
              $("#infoModalLabel").html("<i class='fas fa-exclamation-triangle'></i> " + window.i18n.msgStore['interrupted-connection']);
              $("#infoModal .modal-body").html(window.i18n.msgStore['interrupted-connection-description']);
              $("#infoModal").modal();
              // User has been informed so set the shownError to true so the modal will not be displayed again until the connection with the
              // server is OK
              shownError = true;
            }
          }

          // Broken connection so stop the check interval thread as it is useless
          if (checkIntervalID !== null) {
            clearInterval(checkIntervalID);
            checkIntervalID = null;
          }

          // Set a thread to show a retry countdown to the user
          retryCountdownIntervalID = setInterval(retryCountdown, 1000);
        } else {
          // if there is no check interval thread it means that there was a timeout during a countdown retry, so re-set a countdown thread
          if (checkIntervalID == null) {
            retryNb++;
            retryCpt = retryCptBaseVal * retryNb;
            // Set a thread to show a retry countdown to the user
            retryCountdownIntervalID = setInterval(retryCountdown, 1000);
          }
        }
      }
    });
  }
}
