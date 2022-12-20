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
var keycloakUser = false;
var ssoEnabled = false;

function setSSOEnabled(resp) {
  // Determine if a SSO protocol is enabled
  var samlEnabled = false;
  var casEnabled = false;
  if ((resp.samlEnabled != undefined && resp.samlEnabled != null)) {
    samlEnabled = resp.samlEnabled;
  }
  if (resp.casEnabled != undefined && resp.casEnabled != null) {
    casEnabled = resp.casEnabled;
  }
  if(samlEnabled || casEnabled) {
    ssoEnabled = true;
  }
}

function setDefaultTimeout(resp) {
  if (resp.sessionTimeout != undefined && resp.sessionTimeout != null) {
    sessionDefaultTimeout = resp.sessionTimeout;
  }
}

$(document).ready(function() {

  // check the session once the page is loaded to retrieve the logged username and the sessionTiemout defined by the server
  $.ajax({
    url : '../RefreshSession',
    dataType : "json",
    timeout : 1000,
    success : function(resp) {
      if (resp.code == 0) {
        logged = resp.user;
      }
      if (resp.sessionTimeout != undefined && resp.sessionTimeout != null) {
        sessionDefaultTimeout = resp.sessionTimeout;
      }
      
      setDefaultTimeout(resp);
      setSSOEnabled(resp);
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
  $("#content-info-header").html(
      "<i class='fas fa-exclamation-triangle'></i> " + window.i18n.msgStore['interrupted-connection'] + " " + window.i18n.msgStore['retry-in'] + " " + retryCpt
          + "s <span class='retry-button' onclick='retryNow()'>" + window.i18n.msgStore['retry-now'] + "</span>").show();
  // Once the countdown reaches 0, try to connect to the server and refresh the session
  if (retryCpt == 0) {
    retryNow();
  }
}

// Tries to refresh the session
function retryNow() {
  $("#content-info-header").html(window.i18n.msgStore['retrying']);
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
  $("#content-info-header").html(window.i18n.msgStore['connection-ok']).fadeOut();
}

function checkIdleTime() {
  var diff = new Date().getTime() - lastActiveTime;
  // if last activity was less than idleThreshold ago, or the sessionTimeout has been exceeded, or there is no check interval thread, then
  // send a refresh session request
  if (diff <= idleThreshold || checkIntervalID == null) {
    $.ajax({
      url : '../RefreshSession',
      dataType : "json",
      timeout : 1000,
      success : function(resp) {
        connectionSucceededReinit();
        // Refresh session timeout param
        setDefaultTimeout(resp);
        // Refresh ssoEnabled param
        setSSOEnabled(resp);
        if (resp.code != 0) { // session has expired or is invalid, so redirect the user to the login page
          window.location.href = "../login?timeout=expired&redirect=" + encodeURIComponent(window.location.href);
          return;
        } else {
          if (logged !== resp.user && resp.isAdmin) { // admin user but different user so reload
            window.location.reload();
            return;
          } else if (!resp.isAdmin) { // new user and isn't an admin user, so redirect to the search page
            window.location.href = "../";
            return;
          }
          // session has been successfully refreshed so update the lastActiveTime
          lastActiveTime = new Date().getTime();
        }
        if (resp.keycloakUser != undefined && resp.keycloakUser != null) {
          keycloakUser = resp.keycloakUser;
        }
      },
      error : function(request, status, err) {
        if (status !== "timeout") {
          if (request.status === 401 || (request.responseText != undefined && request.responseText.indexOf("session has been expired") !== -1) || (request.status === 0 && ssoEnabled)) {
            if (!keycloakUser && !ssoEnabled) {
              window.location.href = "../login?timeout=expired&redirect=" + encodeURIComponent(window.location.href);
              return;
            } else {
              window.location.reload();
              return;
            }
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
