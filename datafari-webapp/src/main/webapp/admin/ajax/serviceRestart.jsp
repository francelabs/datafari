<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>

<head>
  <!-- Page specific JS -->
  <script src="<c:url value="/resources/js/admin/ajax/serviceRestart.js" />" type="text/javascript"></script>
  <!-- Page specific CSS -->
  <link href="<c:url value="/resources/css/admin/serviceRestart.css" />" rel="stylesheet"></link>
  <meta charset="UTF-8">
  <title>Insert title here</title>
</head>

<body>
  <!-- Start Status check popup and backdrop -->
  <div id="status-check" class="">
    <div class="a-fullscreen-popup-with-backdrop_backdrop"></div>
    <div class="a-fullscreen-popup-with-backdrop_popup u-padding-2 u-rounded-corner-1" id="status-check_popup">
      <span id="spinner" class="fa fa-spinner fa-spin u-center-text"></span>
      <p id="status-check_warning">Do NOT close this window NOR refresh your screen. The server is currently restarting,
        the admin UI is not available at this time. The service status is checked regularly.</p>
      <p id="status-check_retry-p"><span id="status-check_retry-msg">Next status check</span>: <span id="status-check_retry-count"></span></p>
      <p id="status-check_countdown-p"><span id="status-check_countdown-msg">Server is currently restarting, next status check in</span><span
          id="status-check_countdown"></span></p>
      <p id="status-check_checking-msg">Chekcing server status...</p>
      <div id="status-check_too-much-retries">
        <p id="status-check_too-much-retries-msg">We cannot check if the server
          successfully restarted. What do you want to do ?</p>
        <ul>
          <li><span id="status-check_wait-more-msg">Try to wait for 10 more retries</span>: <button
              id="status-check_wait-more-btn" class="btn btn-primary">Retry</button></li>
          <li><span id="status-check_contact-sysadmin-msg">Contact you system administrator</span>: <a
              href="mailto:"><span class="fa fa-envelope"></span></a></li>
          <li><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/10747939/Utility+logs"
              id="status-check_check-logs-msg" target="new">Check the logs</a></li>
          <li><span id="status-check_go-home-msg">Try to go to the admin home page (at your own risks, will not work if
              the web server is not up)</span>: <a href="/Datafari/admin/" target="new">admin</a></li>
        </ul>
      </div>
    </div>
  </div>
  <!-- End Status check popup and backdrop-->
  <!-- Start Breadcrumb -->
  <nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1">Home</li>
      <li class="breadcrumb-item" id="topbar2">Service Administration</li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3">Restart</li>
    </ol>
  </nav>
  <!-- End Breadcrumb -->

  <!-- Start info box -->
  <div class="box">
    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span id="box-title_restart-info">Datafari Last Restart Information</span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    <div class="box-content" id="info-box-content">
      <div class="infoblock" id="infoblock">
        <div class="infoblock_elm row" id="infoblock-date">
          <span class="infoblock_elm-label col-2">Last restart date: </span>
          <span class="infoblock_elm-value col-10">No Data</span>
        </div>
        <div class="infoblock_elm row" id="infoblock-user">
          <span class="infoblock_elm-label col-2">Last restart from user: </span>
          <span class="infoblock_elm-value col-10">No Data</span>
        </div>
        <div class="infoblock_elm row" id="infoblock-ip">
          <span class="infoblock_elm-label col-2">Last restart from IP: </span>
          <span class="infoblock_elm-value col-10">No Data</span>
        </div>
        <div class="infoblock_elm row" id="infoblock-status">
          <span class="infoblock_elm-label col-2">Restart status: </span>
          <span class="infoblock_elm-value col-10">Done</span>
        </div>
        <div class="infoblock_elm row" id="infoblock-report">
          <span class="infoblock_elm-label col-2">
            <span class="report_label">Restart Report:</span> 
            <span id="report-spinner" class="fa fa-spinner fa-spin"></span>
            <span id="report-countdown"></span>  
          </span>
          <pre class="infoblock_elm-value col-10">
          No Data
        </pre>
        </div>
        <div class="infoblock-status-message alert alert-primary" id="infoblock-status-message">
          <span id="info-spinner" class="fa fa-spinner fa-spin"></span>
          <span id="infoblock-status-message-label"></span>
        </div>
      </div>
      <div class="a-popup-with-backdrop" id="info-popup">
        <div class="a-popup-with-backdrop_backdrop"></div>
        <div class="a-popup-with-backdrop_popup u-padding-2 u-rounded-corner-1">
          <span id="info-popup-content">This functionality is disabled</span>
        </div>
      </div>
    </div>
  </div>
  <!-- End info box -->

  <!-- Start form box -->
  <div class="box">

    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span id="box-title_restart-form">Datafari Complete Restart</span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    <div class="documentation-style">
      <p class="documentation-preview">
        <span id="documentation-servicerestart"></span>
        <a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/111903130/Services+Restart+Scripts+-+Enterprise+Edition" target="_blank">
          ...see more</a>
      </p>
    </div>
    <div class="box-content" id="thBox">
      <form class="form-horizontal" id="restart-form">
        <div class="row">
          <label for="datafari-not-responding-input" id="datafari-not-responding-label"
            class="col-sm-3 col-form-label">Do you confirm that your Datafari will not be available until all the
            components are up and running again ? (type: YES)</label>
          <input type="text" id="datafari-not-responding-input" class="col-sm-5 form-control" />
        </div>
        <div class="row">
          <label for="job-stopped-input" id="job-stopped-label" class="col-sm-3 col-form-label">Do you confirm that all
            crawling jobs are stopped ? (type: YES)</label>
          <input type="text" id="job-stopped-input" class="col-sm-5 form-control" />
        </div>
        
         <div class="row">
          <div class="col-sm-3">
            <label for="force-restart-input" id="force-restart-label" class="col-form-label">Force restart</label>
            <span id="forcerestart-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
          </div>
          <div class="col-sm-5">
            <input type="checkbox" id="force-restart-input" class="form-check-input" style="position:absolute;top:25%;margin:revert;" />
          </div>
        </div>
        
        <div class="row">
          <div class="col-sm-3">
            <label class="relevancySave col-form-label" for=""></label>
          </div>
          <div class="col-sm-2 col-form-label pl-0">
            <button type="submit" class="btn btn-primary btn-label-left" id="doRestart-btn">Restart Service</button>
          </div>
        </div>
        <div class="row">
          <div class="col-2"></div>
          <label class="col alert alert-warning" id="doRestartReturnStatus-label"></label>
          <div class="col-2"></div>
        </div>
      </form>
      <div class="a-popup-with-backdrop" id="form-popup">
        <div class="a-popup-with-backdrop_backdrop"></div>
        <div class="a-popup-with-backdrop_popup u-padding-2 u-rounded-corner-1">
          <span id="form-spinner" class="fa fa-spinner fa-spin"></span>
          <span id="form-popup-content">This functionality is disabled</span>
          <div id="form-popup-unmanaged">
            <span id="form-popup-unmanaged-message"><br/>
              You can still regain control forcefully by clicking the button below. This should allow you to perform Datafari administration actions even if your current procedure has not gracefully ended. However, by clicking on the button below, you acknowledge that Datafari may not work correctly since the current automatic procedure has not properly exited. In addition, you take full responsibilities over the potential consequences related to the administration actions you will be taking after you have clicked.<br/>
              <br/>
              Whenever an automatic procedure does not end gracefully, it is mandatory to notify the Datafari support team before taking any action. Not doing so cancels the contractual support of France Labs.</span><br/>
            <button class="btn btn-primary btn-label-left" id="unmanaged-button">I aknoledge and take back control</button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <!-- End form box -->
</body>

</html>