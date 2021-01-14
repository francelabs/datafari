<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>

<!-- Page specific JS -->
<script src="<c:url value="/resources/js/admin/ajax/alertsAdmin.js" />"
  type="text/javascript"></script>

<link href="<c:url value="/resources/css/admin/alertsAdmin.css" />"
  rel="stylesheet"></link>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
  <nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i
      class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink"
        id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page"
        id="topbar3"></li>
    </ol>
  </nav>

  <div class="box no-padding col-sm" id="box">
    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span id="boxname"></span>
      </div>
      <div class="box-icons pull-right"></div>
      <div class="no-move"></div>
    </div>
    <div class="box-content" id="thBox">
      <div class="documentation-style-no-margin">
        <p class="documentation-preview">
          <span id="documentation-alerstadmin"></span><a
            class="documentation-link"
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/8192039/Alerts+management+-+Mail+Configuration"
            target="_blank"> ...see more</a>
        </p>
      </div>
      <form class="form-horizontal" id="form" role="form">
        <div id="errorPrint"></div>
        <fieldset>
          <legend id="delayLegend"></legend>
          <div class="row">
            <label id=HourlyLabel class="col-sm-3 col-form-label"></label>
            <input type="text" class="col-sm-2" id="HourlyDelay"
              style="min-width: 150px;">
            <div class="col-sm-1"></div>
            <div class="form-group col-sm-6" id="hint1">
              <label class="control-label" id="labelHint1"></label>
            </div>
          </div>
          <div class="row">
            <label id=DailyLabel class="col-sm-3 col-form-label"></label>
            <input type="text" class="col-sm-2" id="DailyDelay"
              style="min-width: 150px;">
          </div>
          <div class="row">
            <label id=WeeklyLabel class="col-sm-3 col-form-label"></label>
            <input type="text" class="col-sm-2" id="WeeklyDelay"
              style="min-width: 150px;">
          </div>
        </fieldset>
        <div id="prevNext" class="form-group"></div>
        <div id="mailForm" class="form-group">
          <fieldset id="SMTPForm">
            <legend id="mailLegend">Configuration du mail</legend>
            <div class="row">
              <div class="col-sm-3">
                <label id="SMTPLabel" class="col-form-label">SMTP</label>
                <span id="smtp-tip" class="fas fa-info-circle"
                  data-toggle="tooltip" data-placement="right" title=""></span>
              </div>
              <div class="col-sm-3">
                <input type="text" class="form-control" id="SMTP"
                  style="min-width: 150px;" value="smtp.gmail.com">
              </div>
            </div>
            <div class="row">
              <div class="col-sm-3">
                <label id="SMTPPortLabel" class="col-form-label">SMTP
                  Port</label> <span id="smtp-port-tip"
                  class="fas fa-info-circle" data-toggle="tooltip"
                  data-placement="right" title=""></span>
              </div>
              <div class="col-sm-3">
                <input type="text" class="form-control" id="SMTPPort"
                  style="min-width: 150px;" value="465">
              </div>
            </div>
            <div class="row">
              <div class="col-sm-3">
                <label id="SMTPSecurityLabel" class="col-form-label">Securité:</label>
                <span id="smtp-security-tip" class="fas fa-info-circle"
                  data-toggle="tooltip" data-placement="right" title=""></span>
              </div>
              <div class="col-sm-3">
                <select id="SMTPSecurity" class="form-control"
                  style="width: 100%;"><option
                    id="smtp-security-none" value="none">Aucun</option>
                  <option value="tls">TLS</option>
                  <option value="ssl">SSL</option></select>
              </div>
            </div>
            <div class="row">
              <div class="col-sm-3">
                <label id="AddressLabel" class="col-form-label">Adresse
                  mail:</label> <span id="smtp-address-tip"
                  class="fas fa-info-circle" data-toggle="tooltip"
                  data-placement="right" title=""></span>
              </div>
              <div class="col-sm-3">
                <input type="text" id="Address" class="form-control"
                  style="min-width: 150px;" value="Datafari">
              </div>
            </div>
            <div class="row">
              <div class="col-sm-3">
                <label id="UserLabel" class="col-form-label">Nom
                  d'utilisateur</label> <span id="username-tip"
                  class="fas fa-info-circle" data-toggle="tooltip"
                  data-placement="right" title=""></span>
              </div>
              <div class="col-sm-3">
                <input type="text" id="UserName" class="form-control"
                  style="min-width: 150px;"
                  value="onizukadu06@gmail.com">
              </div>
            </div>
            <div class="row">
              <div class="col-sm-3">
                <label id="PassLabel" class="col-form-label">Mot
                  de passe :</label> <span id="password-tip"
                  class="fas fa-info-circle" data-toggle="tooltip"
                  data-placement="right" title=""></span>
              </div>
              <div class="col-sm-3">
                <input type="password" class="form-control" id="Pass"
                  style="min-width: 150px;">
              </div>
            </div>
            <form class="needs-validation" novalidate="">
              <div class="row">
                <div class="col-sm-3">
                  <label id="ConfirmPassLabel" class="col-form-label">Confirmer
                    le mot de passe :</label> <span
                    id="password-confirmation-tip"
                    class="fas fa-info-circle" data-toggle="tooltip"
                    data-placement="right" title=""></span>
                </div>
                <div class="col-sm-3">
                  <input type="password" class="form-control"
                    id="ConfirmPass" style="min-width: 150px;">
                  <div class="invalid-feedback">Passwords do not
                    match</div>
                </div>
              </div>
            </form>
          </fieldset>
        </div>
        <div id="align-buttons" class="form-group">
          <div class="row" id="divParam">
            <div class="col-sm-3">
              <label id="paramRegLabel" for="" class="col-form-label"></label>
            </div>
            <div class="col-sm-3 col-form-label">
              <button type="button"
                class="btn btn-primary btn-label-center" id="paramReg"
                onclick="javascript : parameters()">
                <span><i class="fas fa-clock-o" id="paramRegtext"></i></span>
              </button>
            </div>

            <label id="parameterSaved" class="col-form-label success"></label>
          </div>
          <div class="form-group">
            <div id="switchAlertsAdmin" class="row">
              <div class="col-sm-3">
                <label id="paramRegEmail" for="" class="col-form-label"></label>
              </div>
              <div class="col-sm-5 col-form-label">
                <input type="checkbox" id="activated"
                  onchange="Javascript : onOff()" name="activated"
                  data-size="sm" data-toggle="toggle"
                  data-onstyle="success" data-offstyle="danger">
              </div>
            </div>
          </div>
        </div>
        <fieldset id="SMTPTestSection">
          <legend id="SMTPTestLegend">Test SMTP (requires to
            first save the parameters)</legend>
          <div class="row">
            <div class="col-sm-3">
              <label id=TestAddressLabel class="col-form-label">E-Mail
                address (will receive a test mail)</label>
            </div>
            <div class="col-sm-3">
              <input type="text" id="testAddress" />
            </div>
          </div>
          <button type="button" class="btn btn-primary btn-label-center"
            id="sendMail"
            data-loading-text="<i class='fa fa-spinner fa-spin'></i> Sending...">
            Send mail</button>
          <div id="smtpTestResult"></div>
        </fieldset>
      </form>
    </div>
    <!-- <div class="col-sm-4" id="hints" > -->
  </div>
</body>