<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<!-- Page specific JS -->
<script src="<c:url value="/resources/js/admin/ajax/tikaCreator.js" />" type="text/javascript"></script>
<!-- Page specific CSS -->
<link href="<c:url value="/resources/css/admin/tikaCreator.css" />" rel="stylesheet"></link>
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
		
		<div class="box-content" id="thBox">
      <div class="documentation-style-no-margin">
        <p class="documentation-preview">
          <span id="documentation-tikaCreator">Datafari 6 has introduced a new UI admin tool allowing to easily create and configure a Tika Server ! The required parameters depend on the type of Tika Server configuration that you will choose </span><a
            href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/2768601089/Tika+Server+-+Easy+creation+configuration"
            target="_blank">...see more</a>
        </p>
      </div>
      <form id="tikaCreateForm" name="tikaCreateForm" class="needs-validation" novalidate>
        <div class="form-group row">
          <div class="col-sm-3 control-label">
            <label id="installDirLabel" class="col-form-label" for="installDir">Install directory</label> <span id="installDir-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
          </div>
          <div class="col-sm-4">
            <input type="text" required id="installDir" name="installDir" placeholder="" class="form-control"></input>
            <div class="invalid-feedback">Please provide an installation directory</div>
          </div>
        </div>
        <div class="form-group row">
          <div class="col-sm">
            <label id="installDirWarnLabel" class="col-form-label alert alert-danger" for=""></label>
          </div>
        </div>
  			<div class="form-group row">
          <div class="col-sm-3 control-label">
            <label id="tikaHostLabel" class="col-form-label" for="tikaHost">Tika host</label> <span id="tikaHost-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
          </div>
          <div class="col-sm-4">
            <input type="text" required id="tikaHost" name="tikaHost" placeholder="" class="form-control"></input>
            <div class="invalid-feedback">Please provide a Tika host</div>
          </div>
        </div>
  			<div class="form-group row">
          <div class="col-sm-3 control-label">
            <label id="tikaPortLabel" class="col-form-label" for="tikaPort">Tika port</label> <span id="tikaPort-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
          </div>
          <div class="col-sm-4">
            <input type="text" required id="tikaPort" name="tikaPort" placeholder="" class="form-control"></input>
            <div class="invalid-feedback">Please provide a Tika port</div>
          </div>
        </div>
        <div class="form-group row">
          <div class="col-sm-3 control-label">
            <label id="tikaTempDirLabel" class="col-form-label" for="tikaTempDir">Tika temp dir</label> <span id="tikaTempDir-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
          </div>
          <div class="col-sm-4">
            <input type="text" required id="tikaTempDir" name="tikaTempDir" placeholder="" class="form-control"></input>
            <div class="invalid-feedback">Please provide a Tika temp dir</div>
          </div>
        </div>
        <div class="form-group row">
          <div class="col-sm-3 control-label">
            <label id="tikaTypeLabel" class="col-form-label" for="tikaType">Tika type</label> <span id="tikaType-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
          </div>
          <div class="col-sm-4">
            <select id="tikaType" name="tikaType" class="form-control"
              onchange="javascript: getTikaForm()">
              <OPTION value="simple" selected>Simple</OPTION>
              <OPTION value="ocr">OCR</OPTION>
            </select>
          </div>
        </div>
        <div class="form-group row" id="tikaOCRDiv" style="display:none;">
          <div class="col-sm-3 control-label">
            <label id="ocrStrategyLabel" class="col-form-label" for="ocrStrategy">Tika type</label> <span id="ocrStrategy-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
          </div>
          <div class="col-sm-4">
            <select id="ocrStrategy" name="ocrStrategy" class="form-control">
              <OPTION value="auto" selected>Auto</OPTION>
              <OPTION value="ocr_only">OCR Only</OPTION>
              <OPTION value="ocr_and_text_extraction">OCR and Text Extraction</OPTION>
            </select>
          </div>
        </div>
        <div class="form-group row">
          <div class="col-sm-3 control-label">
            <label id="externalTikaLabel" for="externalTika" class="col-form-label"></label> <span id="externalTika-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
          </div>
          <div class="col-sm-4 checkbox-div">
           <input type="checkbox" id="externalTika" name="externalTika"></input>                       
          </div>
        </div>
        <div class="form-group row">
          <div class="col-sm-3">
            <label id="tikaCreateLabel" class="col-form-label" for="createTikaServer"></label>
          </div>
          <div class="col-sm-4">
            <button type="Submit" id="createTikaServer" name="createTikaServer"
              class="btn btn-primary"
              data-loading-text="<i class='fa fa-spinner fa-spin'></i> Creating...">Create</button>
          </div>
        </div>
      </form>
      <div id="tikaCreatorMessageSuccess" class="feedback-message alert alert-success"><i class="fas fa-check"></i>Tika Server successfully created</div>
      <div id="tikaCreatorMessageFailure" class="feedback-message alert alert-danger">A problem occurred while creating the Tika Server</div>							
		</div>
	</div>
</body>
</html>