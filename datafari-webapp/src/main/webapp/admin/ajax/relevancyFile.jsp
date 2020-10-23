<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<!-- Page specific JS -->
<script src="<c:url value="/resources/js/admin/ajax/relevancySetupFile.js" />" type="text/javascript"></script>
<!-- Page specific CSS -->
<link href="<c:url value="/resources/css/admin/relevancySetupFile.css" />" rel="stylesheet"></link>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
  <!--Start Breadcrumb-->
  <nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3"></li>
    </ol>
  </nav>
  <!--End Breadcrumb-->

  <div class="box">

    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span id="box-title"></span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    <div class="documentation-style">
        <p class="documentation-preview"> <span id="documentation-relevancyfile"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/135135233/Graphical+Tool+-+Enterprise+Edition" target="_blank"> ...see more</a></p>
      </div>
    <div class="box-content" id="thBox">
      <div class="row">
        <label for="relevancySetupFile-input" id="relevancySetupFile-label" class="col-sm-3 col-form-label"></label>
        <input type="text" id="relevancySetupFile-input" class="col-sm-5 form-control"/>
        <label id="relevancySetupFile-label-default" class="col-sm-6"></label>
      </div>
      <div class="row">
        <label for="goldenQueriesSetupFile-input" id="goldenQueriesSetupFile-label" class="col-sm-3 col-form-label"></label>
        <input type="text" id="goldenQueriesSetupFile-input" class="col-sm-5 form-control"/>
        <label id="goldenQueriesSetupFile-label-default" class="col-sm-6"></label>
      </div>
      <div class="row">
        <div class="col-sm-3">
          <label class="relevancySave col-form-label" for=""></label>
        </div>
        <div class="col-sm-2 col-form-label pl-0">
          <button type="button" class="relevancySetupFile-btn btn btn-primary btn-label-left" id="doSave-btn" onclick="javascript : doSave()"></button>
        </div>
      </div>
      <div class="row">
        <label class="col-sm-3 col-form-label" id="doSaveReturnStatus-label"></label>
      </div>
    </div>
  </div>

  <div class="box">

    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span id="box-config-title"></span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    <div class="documentation-style">
        <p class="documentation-preview">There are two categories of parameters. First, the fixed parameters, which are set to a given values and won't be optimized, either because you did optimize them previously or you know that you want to assign them a specific value. Second, the parameters, which are to be optimized and for which a range of values is defined by giving a min and a max value<a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/111870123/Self-Tuning+Tools+-+Enterprise+Edition+TBC" target="_blank"> ...see more</a></p>
      </div>
    <div class="box-content" id="configBox">
      <br>
      <form>
        <fieldset id="fixed_params_set">
          <legend id="fixed_parameters_set_legend">Fixed parameters configuration</legend>
        </fieldset>
        <br>
        <fieldset id="parameters_set">
          <legend id="parameters_set_legend">Parameters configuration</legend>
        </fieldset>
        <br>
        <div class="row">
          <div class="col-sm-3">
            <label class="relevancySave col-form-label" for=""></label>
          </div>
          <div class="col-sm-2 col-form-label pl-0">
            <button type="button" class="relevancySetupFile-btn btn btn-primary btn-label-left" id="doSaveRelevancy-btn" onclick="javascript : doSaveRelevancySetup()">Save
            </button>
          </div>
        </div>
        <br>
        <div class="row">
          <label class="col-sm-3 col-form-label" id="doSaveRelevancyStatus-label"></label>
        </div>
      </form>
    </div>
  </div>
</body>
</html>
