// Very important to allow debugging !
//# sourceURL=/Datafari/resources/js/admin/ajax/serviceRestart.js

$(document).ready(function () {
  // in seconds
  var STATUS_CHECK_INTERVAL = 60;
  // in milliseconds
  var STATUS_CHECK_TIMEOUT = 5 * 1000;
  // number of retries
  var MAX_STATUS_CHECK_RETRY = 10;

  var nextStatusCheck = STATUS_CHECK_INTERVAL + 1;
  var numRetry = 1;

  var currentTimeout = null;

  var nextReportCheck = STATUS_CHECK_INTERVAL + 1;

  var messages = {
    serviceRestarting : "Service Restarting",
    inProgress: "In Progress",
    done: "Done",
    noData: "No Data",
    noReport : "No report found",
    serviceRestarted : "Service Restarted",
    error: {
      formNotCorrect:
        "You did not fill the form correctly ! Please correct and resubmit.",
      clockSyncError:
        "Your computer and the server clock might be out of sync, this functionality cannot work in this context",
      unexpectedError:
        "An unexpected error occurred while processing the request, please try again and check the logs or contact your system administrator if the problem persists.",
      lastRequestTooRecent: "Last restart request is too recent !",
      cannotCheckState: "Cannot check cluster status, cluster actions disabled until status can be checked. Rety by refreshing the page or contact administrators if the error persists.",
      cannotRestart: {
        1: "Cluster actions UI service not available. Your installation is not standard and requires manual backups. You may want to contact your administrator",
        2: "A restart is in progress, cluster actions are disabled until it is finished.",
        3: "A backup is in progress, cluser actions are disabled uintil it is finished.",
        4: "A connector reinitialization is in progress, cluster actions are disabled until it is finished.",
      }
    },
  };

  var infoblockDate = $("#infoblock-date");
  var infoblockUser = $("#infoblock-user");
  var infoblockIp = $("#infoblock-ip");
  var infoblockStatus = $("#infoblock-status");
  var infoblockReport = $("#infoblock-report");
  infoblockReport.spinner = infoblockReport.find("#report-spinner");
  infoblockReport.reportCountDown = infoblockReport.find("#report-countdown");
  var infoblockStatusMessage = $("#infoblock-status-message");
  var infoPopup = $("#info-popup");
  infoPopup.content = infoPopup.find("#info-popup-content");

  var formPopup = $("#form-popup");
  formPopup.spinner = formPopup.find("#form-spinner");
  formPopup.content = formPopup.find("#form-popup-content");

  var setUnmanagedButton = $("#unmanaged-button");

  var fullScreenPopup = $("#status-check");
  fullScreenPopup.retryCountContainer = fullScreenPopup.find("#status-check_retry-p");
  fullScreenPopup.retryCountContainer.count = fullScreenPopup.retryCountContainer.find("#status-check_retry-count");
  fullScreenPopup.countDownContainer = fullScreenPopup.find("#status-check_countdown-p");
  fullScreenPopup.countDownContainer.countDown = fullScreenPopup.countDownContainer.find("#status-check_countdown");
  fullScreenPopup.checking = fullScreenPopup.find("#status-check_checking-msg");
  fullScreenPopup.tooMuchRetries = fullScreenPopup.find("#status-check_too-much-retries");
  fullScreenPopup.warning = fullScreenPopup.find("#status-check_warning");
  fullScreenPopup.spinner = fullScreenPopup.find("#spinner");

  // Set the breadcrumbs
  document.getElementById("topbar1").innerHTML = window.i18n.msgStore["home"];
  document.getElementById("topbar2").innerHTML =
    window.i18n.msgStore["adminUI-ServiceAdministration"];
  document.getElementById("topbar3").innerHTML =
    window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart"];

  $("#forcerestart-tip").attr("title", window.i18n.msgStore['forcerestart-tip']);
  $('[data-toggle="tooltip"]').tooltip();
 
  // Set the i18n for page elements
  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-last-restart-info"]) {
    document.getElementById("box-title_restart-info").innerHTML =
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-last-restart-info"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-info-date"]) {
    infoblockDate
      .find(".infoblock_elm-label")
      .html(window.i18n.msgStore["adminUI-ClusterActions-info-date"]);
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-info-ip"]) {
    infoblockIp
      .find(".infoblock_elm-label")
      .html(window.i18n.msgStore["adminUI-ClusterActions-info-ip"]);
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-info-report"]) {
    infoblockReport
      .find(".report_label")
      .html(window.i18n.msgStore["adminUI-ClusterActions-info-report"]);
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-info-status"]) {
    infoblockStatus
      .find(".infoblock_elm-label")
      .html(window.i18n.msgStore["adminUI-ClusterActions-info-status"]);
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-info-user"]) {
    infoblockUser
      .find(".infoblock_elm-label")
      .html(window.i18n.msgStore["adminUI-ClusterActions-info-user"]);
  }

  if (
    window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-datafari-complete-restart"]
  ) {
    document.getElementById("box-title_restart-form").innerHTML =
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-datafari-complete-restart"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart"]) {
    document.getElementById("doRestart-btn").innerHTML =
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart"];
  }

  if (window.i18n.msgStore["documentation-ServiceRestart"]) {
    document.getElementById("documentation-servicerestart").innerHTML =
      window.i18n.msgStore["documentation-ServiceRestart"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-datafari-not-responding"]) {
    document.getElementById("datafari-not-responding-label").innerHTML =
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-datafari-not-responding"] +
      " (type: YES)";
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-job-stopped-label"]) {
    document.getElementById("job-stopped-label").innerHTML =
      window.i18n.msgStore["adminUI-clusterActions-ServiceRestart-job-stopped-label"] +
      " (type: YES)";
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-form-incorrectly-filled"]) {
    messages.error.formNotCorrect =
      window.i18n.msgStore["adminUI-ClusterActions-form-incorrectly-filled"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-clock-not-synced"]) {
    messages.error.clockSyncError = window.i18n.msgStore["adminUI-ClusterActions-clock-not-synced"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-warning-msg"]) {
    $("#status-check_warning").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-warning-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-retry-msg"]) {
    $("#status-check_retry-msg").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-retry-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-countdown-msg"]) {
    $("#status-check_countdown-msg").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-countdown-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-checking-msg"]) {
    $("#status-check_checking-msg").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-checking-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-too-much-retries-msg"]) {
    $("#status-check_too-much-retries-msg").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-too-much-retries-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-wait-more-msg"]) {
    $("#status-check_wait-more-msg").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-wait-more-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-contact-sysadmin-msg"]) {
    $("#status-check_contact-sysadmin-msg").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-contact-sysadmin-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-check-logs-msg"]) {
    $("#status-check_check-logs-msg").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-check-logs-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-go-home-msg"]) {
    $("#status-check_go-home-msg").html(
      window.i18n.msgStore["adminUI-ClusterActions-ServiceRestart-go-home-msg"]
    );
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-force-unmanaged"]) {
    formPopup
      .find("#form-popup-unmanaged-message")
      .html(window.i18n.msgStore["adminUI-ClusterActions-force-unmanaged"]);
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-force-unmanaged-button"]) {
    setUnmanagedButton.html(window.i18n.msgStore["adminUI-ClusterActions-force-unmanaged-button"]);
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-last-request-too-recent"]) {
    messages.error.lastRequestTooRecent =
      window.i18n.msgStore["adminUI-ClusterActions-last-request-too-recent"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-no-report"]) {
    messages.noReport =
      window.i18n.msgStore["adminUI-ClusterActions-no-report"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-unexpected-error"]) {
    messages.error.unexpectedError =
      window.i18n.msgStore["adminUI-ClusterActions-unexpected-error"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-in-progress"]) {
    messages.inProgress =
      window.i18n.msgStore["adminUI-ClusterActions-in-progress"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-done"]) {
    messages.done = window.i18n.msgStore["adminUI-ClusterActions-done"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-no-data"]) {
    messages.noData =
      window.i18n.msgStore["adminUI-ClusterActions-no-data"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-cannot-check-state"]) {
    messages.cannotCheckState =
      window.i18n.msgStore["adminUI-ClusterActions-cannot-check-state"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-installation-not-standard"]) {
    messages.error.cannotRestart[1] =
      window.i18n.msgStore["adminUI-ClusterActions-installation-not-standard"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-restart-in-progress"]) {
    messages.error.cannotRestart[2] =
      window.i18n.msgStore["adminUI-ClusterActions-restart-in-progress"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-backup-in-progress"]) {
    messages.error.cannotRestart[3] =
      window.i18n.msgStore["adminUI-ClusterActions-backup-in-progress"];
  }

  if (window.i18n.msgStore["adminUI-ClusterActions-connectors-reinit-in-progress"]) {
    messages.error.cannotRestart[4] =
      window.i18n.msgStore["adminUI-ClusterActions-connectors-reinit-in-progress"];
  }

  $("#restart-form").submit(function (event) {
    event.preventDefault();
    if (
      ($("#datafari-not-responding-input").val() === "YES" &&
      $("#job-stopped-input").val() === "YES") || ($("#force-restart-input").is(":checked") === true)
    ) {
console.log($("#force-restart-input").val());
      sendRestartRequest($("#force-restart-input").is(":checked"));
    } else {
      $("#doRestartReturnStatus-label").show();
      $("#doRestartReturnStatus-label").html(
        messages.error.formNotCorrect
      );
    }
  });

  $("#status-check_wait-more-btn").click(checkStatusCountdown);

  function initUI() {
    infoPopup.hide();
    formPopup.hide();
    infoblockStatusMessage.hide();
    $("#doRestartReturnStatus-label").hide();
    fullScreenPopup.hide();
    fullScreenPopup.retryCountContainer.count.html(
      numRetry + "/" + MAX_STATUS_CHECK_RETRY
    );
    infoblockReport.spinner.hide();
  }

  function unmanagedButtonClick() {
    $.ajax(
      "./../admin/cluster/backup?action=setUnmanaged",
      {
        type: "POST",
        success: function(data, jqxhr) {
          if (data.success === "true") {
            setUnmanagedState();
          } else {
            setErrorRequestingUnmanagedState();
          }
        },
        error: function (jqxhr, text, error) {
          setErrorRequestingUnmanagedState();
        }
      }
    )
  }

  function sendRestartRequest(isForced) {
    var now = new Date();
    var data = { date: now.toISOString(), forceRestart: isForced };
    $.ajax("./../admin/cluster/restart", {
      data: JSON.stringify(data),
      type: "PUT",
      success: function (data, text, jqxhr) {
        if (jqxhr.status === 304) {
          $("#doRestartReturnStatus-label").show();
          $("#doRestartReturnStatus-label").html(messages.error.lastRequestTooRecent);
        } else if (jqxhr.status === 400) {
          $("#doRestartReturnStatus-label").show();
          $("#doRestartReturnStatus-label").html(messages.error.clockSyncError);
        } else {
          // Let's clear the report check process and reinit it
          if (currentTimeout != null) {
            clearTimeout(currentTimeout);
            currentTimeout = null;
            nextReportCheck = STATUS_CHECK_INTERVAL + 1;
            setReportLoadedState();
          }
          checkStatusCountdown();
        }
      },
      error: function (jqxhr, text, error) {
        if (jqxhr.status === 304) {
          $("#doRestartReturnStatus-label").show();
          $("#doRestartReturnStatus-label").html(messages.error.lastRequestTooRecent);
        } else if (jqxhr.status === 400) {
          $("#doRestartReturnStatus-label").show();
          $("#doRestartReturnStatus-label").html(messages.error.clockSyncError);
        } else {
          $("#doRestartReturnStatus-label").show();
          $("#doRestartReturnStatus-label").html(messages.error.unexpectedError);
        }
      },
    });
  }

  function checkStatusCountdown() {
    nextStatusCheck--;
    setCountDownState();
    if (nextStatusCheck > 0) {
      fullScreenPopup.retryCountContainer.count.html(
        numRetry + "/" + MAX_STATUS_CHECK_RETRY
      );
      fullScreenPopup.countDownContainer.countDown.html(" " + nextStatusCheck + "s");
      setTimeout(checkStatusCountdown, 1000);
    } else {
      nextStatusCheck = STATUS_CHECK_INTERVAL + 1;
      checkStatus(true);
    }
  }

  function checkStatus(isLooping) {
    setCheckStatusState();
    numRetry++;
    $.ajax("./../admin/cluster/restart", {
      type: "GET",
      timeout: STATUS_CHECK_TIMEOUT,
      success: function (data, text, jqxhr) {
        fullScreenPopup.hide();
        if (jqxhr.status === 200) {
          setStatusCheckFinishedState();
          fillStatusData(data);
          numRetry = 1;
        } else if (isLooping) {
          if (numRetry > MAX_STATUS_CHECK_RETRY) {
            numRetry = 1;
            setTooMuchRetriesState();
          } else {
              checkStatusCountdown();
          }
        }
      },
      error: function (jqxhr, text, error) {
        if (isLooping && (text === "timeout" || text === "abort" || jqxhr.status >= 500)) {
          if (numRetry > MAX_STATUS_CHECK_RETRY) {
            numRetry = 1;
            setTooMuchRetriesState();
          } else {
            checkStatusCountdown();
          }
        } else {
          numRetry = 1;
          setInfoblockUnexpectedErrorState();
        }
      },
    });
  }

  function getReportLoop() {
    setLoadingReportState();
    nextReportCheck--;
    setReportCountDown(nextReportCheck);
    if (nextReportCheck > 0) {
      currentTimeout = setTimeout(getReportLoop, 1000);
    } else {
      nextReportCheck = STATUS_CHECK_INTERVAL + 1;
      getReport(true);
    }
  }

  function setReportCountDown(countdown) {
    infoblockReport.reportCountDown.html(countdown + "s");
  }

  function getReport(isLooping) {
    setLoadingReportState();
    $.ajax("./../admin/cluster/restart?action=getReport", {
      type: "GET",
      timeout: STATUS_CHECK_TIMEOUT,
      success: function (data, text, jqxhr) {
        fullScreenPopup.hide();
        if (jqxhr.status === 200) {
          infoblockReport.find(".infoblock_elm-value").html(data);
          if (isLooping) {
            checkForReportLoop();
          } else {
            setReportLoadedState();
          }
        } else {
          setInfoblockUnexpectedErrorState();
        }
      },
      error: function (jqxhr, text, error) {
        setInfoblockUnexpectedErrorState();
      },
    });
  }

  function checkForReportLoop() {
    $.ajax("./../admin/cluster/restart", {
      type: "GET",
      timeout: STATUS_CHECK_TIMEOUT,
      success: function (data, text, jqxhr) {
        if (data.inProgress) {
          getReportLoop();
        } else {
          fillStatusData(data);
        }
      },
    });
  }

  function fillStatusData(data) {
    $("#doRestartReturnStatus-label").hide();

    if (data.date && data.date != "") {
      infoblockDate.find(".infoblock_elm-value").html(data.date);
    } else {
      infoblockDate.find(".infoblock_elm-value").html(messages.noData);
    }
    if (data.fromIp && data.fromIp != "") {
      infoblockIp.find(".infoblock_elm-value").html(data.fromIp);
    } else {
      infoblockIp.find(".infoblock_elm-value").html(messages.noData);
    }
    if (data.reportAvailable && data.reportAvailable != "") {
      if (data.inProgress) {
        setInProgressState();
        nextReportCheck = 0;
        getReportLoop();
      } else {
        setDoneState();
        getReport(false);
      }
    } else {
      infoblockReport.find(".infoblock_elm-value").html(messages.noData);
    }
    if (data.fromUser && data.fromUser != "") {
      infoblockUser.find(".infoblock_elm-value").html(data.fromUser);
    } else {
      infoblockUser.find(".infoblock_elm-value").html(messages.noData);
    }
    if (data.inProgress) {
      infoblockStatus
        .find(".infoblock_elm-value")
        .html(messages.inProgress);
    } else {

      infoblockStatus.find(".infoblock_elm-value").html(messages.done);
    }

    if (data.canPerformAction[0].code !== 0) {

      if (data.canPerformAction[0].code === 1) {
        hideUnmanagedStateOption();
      } else {
        var unavailable = false;
        var warningMessages = [];
        var unavailableMessage = [];
        $.each(data.canPerformAction, function(index, status) {
          if (parseInt(status.code, 10) < 10) {
            unavailable = true;
            unavailableMessage.push(parseInt(status.code, 10));
          } else {
            warningMessages.push(parseInt(status.code, 10));
          }
        });
        if (unavailable){
          setFormDisableState(messages.error.cannotRestart[unavailableMessage[0]]);
        } else {
          setFormEnableState();
          setInfoblockEnableState();
        }
      }
    }

    if (data.forceUnmanaged === "true") {
      setUnmanagedState();
    }
  }

  function setCheckStatusState() {
    formPopup.hide();
    infoPopup.hide();
    infoblockStatusMessage.hide();
    fullScreenPopup.show();
    fullScreenPopup.checking.show();
    fullScreenPopup.countDownContainer.hide();
    fullScreenPopup.tooMuchRetries.hide();
  }

  function setTooMuchRetriesState() {
    formPopup.hide();
    infoPopup.hide();
    infoblockStatusMessage.hide();
    fullScreenPopup.show();
    fullScreenPopup.tooMuchRetries.show();
    fullScreenPopup.retryCountContainer.hide();
    fullScreenPopup.countDownContainer.hide();
    fullScreenPopup.checking.hide();
    fullScreenPopup.warning.hide();
    fullScreenPopup.spinner.hide();
  }

  function setCountDownState() {
    formPopup.hide();
    infoPopup.hide();
    infoblockStatusMessage.hide();
    fullScreenPopup.show();
    fullScreenPopup.spinner.show();
    fullScreenPopup.warning.show();
    fullScreenPopup.retryCountContainer.show();
    fullScreenPopup.retryCountContainer.count.show();
    fullScreenPopup.countDownContainer.show();
    fullScreenPopup.tooMuchRetries.hide();
    fullScreenPopup.checking.hide();
  }

  function setStatusCheckFinishedState() {
    fullScreenPopup.hide();
    formPopup.hide();
    infoPopup.hide();
  }

  function setLoadingReportState() {
    infoblockReport.spinner.show();
    infoblockReport.reportCountDown.show();
  }

  function setReportLoadedState() {
    infoblockReport.spinner.hide();
    infoblockReport.reportCountDown.hide();
  }

  function setInfoblockUnexpectedErrorState() {
    fullScreenPopup.hide();
    infoblockStatusMessage
      .find("#infoblock-status-message-label")
      .html(messages.error.unexpectedError);
    infoblockStatusMessage.show();
    setFormDisableState(messages.error.cannotCheckState);
  }

  function setInProgressState() {
    infoblockStatusMessage.hide();
    setFormDisableState(messages.error.cannotRestart["2"]);
  }

  function setDoneState() {
    infoblockStatusMessage.hide();
    setFormEnableState();
  }

  function setFormDisableState(message) {
    formPopup.show();
    formPopup.find("#unmanaged-request-error").hide();
    formPopup.spinner.hide();
    formPopup.content.html(message);
  }

  function setInfoblockDisableState(message) {
    infoPopup.show();
    infoPopup.content.html(message);
  }

  function setFormEnableState() {
    formPopup.hide();
  }

  function setInfoblockEnableState() {
    infoPopup.hide();
  }

  function hideUnmanagedStateOption() {
    formPopup.find("#form-popup-unmanaged").hide();
  }

  function setUnmanagedState() {
    formPopup.find("#form-spinner").hide();
    formPopup.find("#unmanaged-request-error").hide();
    formPopup.hide();

    infoblockStatusMessage.hide();
  }

  initUI();
  setUnmanagedButton.click(unmanagedButtonClick);
  checkStatus(false);
});