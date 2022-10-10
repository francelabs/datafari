<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<!-- Page specific JS -->
<script src="<c:url value="/resources/js/admin/ajax/mcfSimplified.js" />" type="text/javascript"></script>
<!-- Page specific CSS -->
<Link rel="stylesheet" href="<c:url value="/resources/css/animate.min.css" />"/>
<link href="<c:url value="/resources/css/admin/mcfSimplified.css" />" rel="stylesheet"></link>
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
        <p class="documentation-preview"> <span id="documentation-mcfsimplified"></span><a href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/682754069/MCF+Simplified+UI+configuration" target="_blank">...see more</a></p>
      </div>  
      <div class="row">
        <label id="mcfsimplified-title-label" class="col-sm-4 control-label"></label>
        <label id="mcfsimplified-choice-label" class="col-sm-4 control-label"></label>

      <br>
      </div>
      <div class="row">
        <label class="col-sm-4 control-label" id="doRestoreReturnStatus-label"></label>
      </div>      
      <form>
        <fieldset>
          <legend id="Modify"></legend>
          <div class="from-group row">
            <div class="col-sm-3">
              <label class="col-form-label" for="">Select the source type of your choice</label>
            </div>
            <div class="col-sm-3">
	            <select id="jobType" class="form-control"
	              onchange="javascript: getJobForm()">
	              <OPTION></OPTION>
	              <OPTION value="webjob" id="createWebJob"></OPTION>
	              <OPTION value="filerjob" id="createFilerJob"></OPTION>
	              <OPTION value="dbjob" id="createDbJob"></OPTION>
	            </select>
            </div>
          </div>
        </fieldset>
      </form>      
    </div>
    <fieldset>
      <legend id="legendAdd"></legend>
      <div id="addPromForm">
        <div id="filerJobDiv" style="display:none;" class="formDiv">
          <div class="col-xl-12">
            <div class="box">
              <div class="box-header">
                <div class="box-name">
                  <i class="fas fa-table"></i>
                  <span id="filerJobTitle"></span>
                </div>
                <div class="box-icons">
                  <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
                  <a class="expand-link"><i class="fas fa-expand"></i></a>
                </div>
                <div class="no-move">
                </div>
              </div>
              <div id="addBox" class="box-content">
	              <form id="addFiler" class="needs-validation" novalidate>
	                <fieldset id="fieldContentFiler">
	                  <legend id="filerAddLegend"></legend>
	                  <div class="form-group row" id="filer-server-div">
	                    <div class="col-sm-3 control-label">
	                      <span class="fas fa-asterisk " style="color : red"></span>
	                      <label id="serverLabel" for="server" class="col-form-label"></label><span id="server-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
	                    </div>
	                    <div class="col-sm-4">
	                      <input type="text" required id="server" name="server" placeholder="" class="form-control"></input>
	                      <div class="invalid-feedback">
							          Please provide a server
							        </div>
	                    </div>                      
	                  </div>
	                  <div class="form-group row" id="filer-user-div">
	                    <div class="col-sm-3 control-label">
	                      <span class="fas fa-asterisk " style="color : red"></span>
	                      <label id="userLabel" for="user" class="col-form-label"></label><span id="user-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
	                    </div>
	                    <div class="col-sm-4">
	                      <input required type="text" id="user" name="user" placeholder="" class="form-control">
	                      <div class="invalid-feedback">
	                        Please provide a user
	                      </div>
	                    </div>                      
	                  </div>
	                  <div class="form-group row" id="filer-password-div">
	                    <div class="col-sm-3 control-label">
	                      <span class="fas fa-asterisk " style="color : red"></span>
	                      <label id="passwordLabel" for="password" class="col-form-label"></label><span id="password-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
	                    </div>
	                    <div class="col-sm-4">
	                      <input type="password" id="password" name="password" placeholder="" required class="form-control">
	                      <div class="invalid-feedback">
	                        Please provide a password
	                      </div>
	                    </div>                      
	                  </div>
	                  <div class="form-group row" id="filer-paths-div">
	                    <div class="col-sm-3 control-label">
	                      <span class="fas fa-asterisk " style="color : red"></span>
	                      <label id="pathsLabel" for="paths" class="col-form-label"></label><span id="paths-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
	                    </div>
	                    <div class="col-sm-4">
	                      <textarea required id="paths" name="paths" placeholder="" class="form-control"></textarea>
	                      <div class="invalid-feedback">
	                        Please provide at least one path
	                      </div>
	                    </div>                      
	                  </div>
	                  <div class="form-group row" id="filer-sourcename-div">
	                    <div class="col-sm-3 control-label">
	                      <span class="fas fa-asterisk " style="color : red"></span>
	                      <label id="filerSourcenameLabel" for="filerSourcename" class="col-form-label"></label><span id="filerSourcename-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
	                    </div>
	                    <div class="col-sm-4">
	                      <input required type="text" id="filerSourcename" name="filerSourcename" placeholder="" class="form-control">
	                      <div class="invalid-feedback">
	                        Please provide a name for the source
	                      </div>
	                    </div>                      
	                  </div>
	                  <div class="form-group row" id="filer-reponame-div">
	                    <div class="col-sm-3 control-label">
	                      <span class="fas fa-asterisk " style="color : red"></span>
	                      <label id="filerReponameLabel" for="filerReponame" class="col-form-label"></label><span id="filerReponame-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
	                    </div>
	                    <div class="col-sm-4">
	                      <input required type="text" id="filerReponame" name="filerReponame" placeholder="" class="form-control">
	                      <div class="invalid-feedback">
	                        Please provide a name for the repository
	                      </div>
	                    </div>                      
	                  </div>
                    <div class="form-group row" id="div15">
                      <div class="col-sm-3 control-label">
                        <label id="securityLabel" for="security" class="col-form-label"></label><span id="security-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span> <span class="alert-danger">(This feature is only available in the Enterprise Edition)</span>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                       <input type="checkbox" id="security" name="security" disabled></input>                       
                      </div>
                    </div>
                    <div class="form-group row">
                      <div class="col-sm-3 control-label">
                        <label id="duplicatesDetectionLabel" for="duplicatesDetection" class="col-form-label"></label><span id="duplicatesDetection-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span> <span class="alert-danger">(This feature is only available in the Enterprise Edition)</span>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                       <input type="checkbox" id="duplicatesDetection" name="duplicatesDetection" disabled></input>                       
                      </div>
                    </div>
                    <div class="form-group row" id="filerCreateOCR-div">
                      <div class="col-sm-3 control-label">
                        <label id="filerCreateOCRLabel" for="filerCreateOCR" class="col-form-label"></label><span id="filerCreateOCR-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                        <input type="checkbox" id="filerCreateOCR" name="filerCreateOCR"></input>                       
                      </div>
                    </div>
                    <div id="filerOCR" style="display:none;">
                      <div class="form-group row" id="filerTikaOCRHost-div">
                        <div class="col-sm-3 control-label">
                          <label id="filerTikaOCRHostLabel" for="filerTikaOCRHost" class="col-form-label"></label><span id="filerTikaOCRHost-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="filerTikaOCRHost" name="filerTikaOCRHost" class="form-control"></input>    
                          <div class="invalid-feedback">
                          Please provide a Tika server host
                        </div>                   
                        </div>
                      </div>
                      <div class="form-group row" id="filerTikaOCRPort-div">
                        <div class="col-sm-3 control-label">
                          <label id="filerTikaOCRPortLabel" for="filerTikaOCRPort" class="col-form-label"></label><span id="filerTikaOCRPort-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="filerTikaOCRPort" name="filerTikaOCRPort" class="form-control"></input>  
                          <div class="invalid-feedback">
                          Please provide a Tika server port
                        </div>                     
                        </div>
                      </div>
                      <div class="form-group row" id="filerTikaOCRName-div">
                        <div class="col-sm-3 control-label">
                          <label id="filerTikaOCRNameLabel" for="filerTikaOCRName" class="col-form-label"></label><span id="filerTikaOCRName-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="filerTikaOCRName" name="filerTikaOCRName" class="form-control"></input>
                          <div class="invalid-feedback">
                          Please provide a Tika server name
                        </div>                       
                        </div>
                      </div>
                    </div>
	                  <div class="form-group row" id="div16">
	                    <div class="col-sm-3 control-label">
	                      <label id="startJobLabel" for="startJob" class="col-form-label"></label>
	                    </div>
	                    <div class="col-sm-4 checkbox-div" >
	                      <input type="checkbox" id="startJob" name="startJob"></input>
	                    </div>
	                  </div>
	                  </fieldset>
	                  <div class="form-group row" id="div4">
	                    <div class="col-sm-3">
	                      <label class="MCFSave col-form-label" class="col-form-label" for=""></label>
	                    </div>
	                    <div class="col-sm-4">
	                      <button type="Submit" id="newFilerConfig" name="addProm" class="btn btn-primary" data-loading-text="<i class='fa fa-spinner fa-spin'></i> Saving...">Save</button>
	                    </div>
	                    <div class="col-sm-3">
	                      <i class="fas fa-asterisk" style="color : red"></i>
	                      <label class="col-form-label asteriskLabel"></label>
	                    </div>
	                  </div>
                </form>
                <div id="addFilerMessageSuccess" class="feedback-message alert alert-success"><i class="fas fa-check"></i>Configuration successfully saved.</div>
                <div id="addFilerMessageFailure" class="feedback-message alert alert-danger">A problem occurred while saving the configuration</div>
                <div id="addFilerCheckMessageFailure" class="feedback-message alert alert-danger"></div>
              </div>
            </div>
          </div>
        </div>
        
        <div id="dbJobDiv" style="display:none;" class="formDiv">
          <div class="col-xl-12">
            <div class="box">
              <div class="box-header">
                <div class="box-name">
                  <i class="fas fa-table"></i>
                  <span id="dbJobTitle"></span>
                </div>
                <div class="box-icons">
                  <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
                  <a class="expand-link"><i class="fas fa-expand"></i></a>
                </div>
                <div class="no-move">
                </div>
              </div>
              <div id="addBox" class="box-content">
                <form id="addDb" class="needs-validation" novalidate>
                  <fieldset id="fieldContentDb">
                    <legend id="dbAddLegend"></legend>
                    <div class="form-group row" id="db-type-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbTypeLabel" for="dbType" class="col-form-label"></label><span id="dbType-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <select id="dbType" name="dbType" class="form-control">
                          <OPTION value="oracle:thin:@" selected>Oracle</OPTION>
                          <OPTION value="postgresql://">Postgres</OPTION>
                          <OPTION value="jtds:sqlserver://">Microsoft SQL Server</OPTION>
                          <OPTION value="jtds:sybase://">Sybase</OPTION>
                          <OPTION value="mysql://">MySQL</OPTION>
                          <OPTION value="mariadb://">MariaDB</OPTION>
                        </select>
                      </div>
                    </div>                      
                    <div class="form-group row" id="db-host-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbHostLabel" for="user" class="col-form-label"></label><span id="dbHost-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input required type="text" id="dbHost" name="dbHost" placeholder="" class="form-control">
                        <div class="invalid-feedback">
                          Please provide a host
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-name-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbNameLabel" for="dbName" class="col-form-label"></label><span id="dbName-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input required type="text" id="dbName" name="dbName" placeholder="" class="form-control">
                        <div class="invalid-feedback">
                          Please provide a database name
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-conn-str-div">
                      <div class="col-sm-3 control-label">
                        <label id="dbConnStrLabel" for="dbConnStr" class="col-form-label"></label><span id="dbConnStr-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input type="text" id="dbConnStr" name="dbConnStr" placeholder="" class="form-control">
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-username-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbUsernameLabel" for="dbUsername" class="col-form-label"></label><span id="dbUsername-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input required type="text" id="dbUsername" name="dbUsername" placeholder="" class="form-control">
                         <div class="invalid-feedback">
                          Please provide a username
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-password-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbPasswordLabel" for="dbPassword" class="col-form-label"></label><span id="dbPassword-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input type="password" id="dbPassword" name="dbPassword" placeholder="" required class="form-control">
                        <div class="invalid-feedback">
                          Please provide a password
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-seeding-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbSeedingLabel" for="dbSeeding" class="col-form-label"></label><span id="dbSeeding-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <textarea required id="dbSeeding" name="dbSeeding" placeholder="" class="form-control"></textarea>
                        <div class="invalid-feedback">
                          Please provide a seeding query
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-version-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbVersionLabel" for="dbVersion" class="col-form-label"></label><span id="dbVersion-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <textarea required id="dbVersion" name="dbVersion" placeholder="" class="form-control"></textarea>
                        <div class="invalid-feedback">
                          Please provide a version query
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-data-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbDataLabel" for="dbData" class="col-form-label"></label><span id="dbData-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <textarea required id="dbData" name="dbData" placeholder="" class="form-control"></textarea>
                        <div class="invalid-feedback">
                          Please provide a data query
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-sourcename-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbSourcenameLabel" for="dbSourcename" class="col-form-label"></label><span id="dbSourcename-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input required type="text" id="dbSourcename" name="dbSourcename" placeholder="" class="form-control">
                        <div class="invalid-feedback">
                          Please provide a name for the source
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="db-reponame-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="dbReponameLabel" for="dbReponame" class="col-form-label"></label><span id="dbReponame-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input required type="text" id="dbReponame" name="dbReponame" placeholder="" class="form-control">
                        <div class="invalid-feedback">
                          Please provide a name for the repository
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row">
                      <div class="col-sm-3 control-label">
                        <label id="dbSecurityLabel" for="dbSecurity" class="col-form-label"></label><span id="dbSecurity-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span> <span class="alert-danger">(This feature is only available in the Enterprise Edition)</span>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                       <input type="checkbox" id="dbSecurity" name="dbSecurity" disabled></input>                       
                      </div>
                    </div>
                    <div class="form-group row" id="db-access-token-div">
                      <div class="col-sm-3 control-label">
                        <label id="dbAccessTokenLabel" for="dbAccessToken" class="col-form-label"></label><span id="dbAccessToken-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <textarea id="dbAccessToken" name="dbAccessToken" placeholder="" class="form-control"></textarea>
                        <div class="invalid-feedback">
                          Please provide an access token query
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row">
                      <div class="col-sm-3 control-label">
                        <label id="dbDuplicatesDetectionLabel" for="dbDuplicatesDetection" class="col-form-label"></label><span id="dbDuplicatesDetection-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span> <span class="alert-danger">(This feature is only available in the Enterprise Edition)</span>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                       <input type="checkbox" id="dbDuplicatesDetection" name="dbDuplicatesDetection" disabled></input>                       
                      </div>
                    </div>
                    <div class="form-group row" id="dbCreateOCR-div">
                      <div class="col-sm-3 control-label">
                        <label id="dbCreateOCRLabel" for="dbCreateOCR" class="col-form-label"></label><span id="dbCreateOCR-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                        <input type="checkbox" id="dbCreateOCR" name="dbCreateOCR"></input>                       
                      </div>
                    </div>
                    <div id="dbOCR" style="display:none;">
                      <div class="form-group row" id="dbTikaOCRHost-div">
                        <div class="col-sm-3 control-label">
                          <label id="dbTikaOCRHostLabel" for="dbTikaOCRHost" class="col-form-label"></label><span id="dbTikaOCRHost-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="dbTikaOCRHost" name="dbTikaOCRHost" class="form-control"></input>    
                          <div class="invalid-feedback">
                          Please provide a Tika server host
                        </div>                   
                        </div>
                      </div>
                      <div class="form-group row" id="dbTikaOCRPort-div">
                        <div class="col-sm-3 control-label">
                          <label id="dbTikaOCRPortLabel" for="dbTikaOCRPort" class="col-form-label"></label><span id="dbTikaOCRPort-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="dbTikaOCRPort" name="dbTikaOCRPort" class="form-control"></input>  
                          <div class="invalid-feedback">
                          Please provide a Tika server port
                        </div>                     
                        </div>
                      </div>
                      <div class="form-group row" id="dbTikaOCRName-div">
                        <div class="col-sm-3 control-label">
                          <label id="dbTikaOCRNameLabel" for="dbTikaOCRName" class="col-form-label"></label><span id="dbTikaOCRName-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="dbTikaOCRName" name="dbTikaOCRName" class="form-control"></input>
                          <div class="invalid-feedback">
                          Please provide a Tika server name
                        </div>                       
                        </div>
                      </div>
                    </div>
                    <div class="form-group row">
                      <div class="col-sm-3 control-label">
                        <label id="dbStartJobLabel" for="dbStartJob" class="col-form-label"></label>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                        <input type="checkbox" id="dbStartJob" name="dbStartJob"></input>
                      </div>
                    </div>
                    </fieldset>
                    <div class="form-group row">
                      <div class="col-sm-3">
                        <label class="MCFSave col-form-label" class="col-form-label" for=""></label>
                      </div>
                      <div class="col-sm-4">
                        <button type="Submit" id="newDbConfig" name="addProm" class="btn btn-primary" data-loading-text="<i class='fa fa-spinner fa-spin'></i> Saving...">Save</button>
                      </div>
                      <div class="col-sm-3">
                        <i class="fas fa-asterisk" style="color : red"></i>
                        <label class="col-form-label asteriskLabel"></label>
                      </div>
                    </div>
                </form>
                <div id="addDbMessageSuccess" class="feedback-message alert alert-success"><i class="fas fa-check"></i>Configuration successfully saved.</div>
                <div id="addDbMessageFailure" class="feedback-message alert alert-danger">A problem occurred while saving the configuration</div>
                <div id="addDbCheckMessageFailure" class="feedback-message alert alert-danger"></div>
              </div>
            </div>
          </div>
        </div>
      
      
        <div id="webJobDiv" style="display:none;" class="formDiv">
          <div class="col-xl-12">
            <div class="box">
              <div class="box-header">
                <div class="box-name">
                  <i class="fas fa-table"></i>
                  <span id="webJobTitle"></span>
                </div>
                <div class="box-icons">
                  <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
                  <a class="expand-link"><i class="fas fa-expand"></i></a>
                </div>
                <div class="no-move">
                </div>
              </div>
              <div id="addBox" class="box-content">
                <form id="addWeb" class="needs-validation" novalidate>
                  <fieldset id="fieldContentWeb">
                    <legend id="webAddLegend"></legend>
                    <div class="form-group row" id="div1">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="webSeedsLabel" for="seeds" class="col-form-label"></label><span id="seeds-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <textarea required id="seeds" name="seeds" placeholder="" class="form-control"></textarea>
                        <div class="invalid-feedback">
                          Please provide at least one seed
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="div11">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="webEmailLabel" for="email" class="col-form-label"></label><span id="email-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input required type="text" id="email" name="email" placeholder="" class="form-control">
                        <div class="invalid-feedback">
                          Please provide an email
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="web-sourcename-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="webSourcenameLabel" for="webSourcename" class="col-form-label"></label><span id="webSourcename-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input required type="text" id="webSourcename" name="webSourcename" placeholder="" class="form-control">
                        <div class="invalid-feedback">
                          Please provide a name for the source
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row" id="web-reponame-div">
                      <div class="col-sm-3 control-label">
                        <span class="fas fa-asterisk " style="color : red"></span>
                        <label id="webReponameLabel" for="webReponame" class="col-form-label"></label><span id="webReponame-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4">
                        <input required type="text" id="webReponame" name="webReponame" placeholder="" class="form-control">
                        <div class="invalid-feedback">
                          Please provide a name for the repository
                        </div>
                      </div>                      
                    </div>
                    <div class="form-group row">
                      <div class="col-sm-3 control-label">
                        <label id="webDuplicatesDetectionLabel" for="dbDuplicatesDetection" class="col-form-label"></label><span id="webDuplicatesDetection-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span> <span class="alert-danger">(This feature is only available in the Enterprise Edition)</span>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                       <input type="checkbox" id="webDuplicatesDetection" name="webDuplicatesDetection" disabled></input>                       
                      </div>
                    </div>
                    <div class="form-group row" id="webCreateOCR-div">
                      <div class="col-sm-3 control-label">
                        <label id="webCreateOCRLabel" for="webCreateOCR" class="col-form-label"></label><span id="webCreateOCR-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                        <input type="checkbox" id="webCreateOCR" name="webCreateOCR"></input>                       
                      </div>
                    </div>
                    <div id="webOCR" style="display:none;">
                      <div class="form-group row" id="webTikaOCRHost-div">
                        <div class="col-sm-3 control-label">
                          <label id="webTikaOCRHostLabel" for="webTikaOCRHost" class="col-form-label"></label><span id="webTikaOCRHost-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="webTikaOCRHost" name="webTikaOCRHost" class="form-control"></input>    
                          <div class="invalid-feedback">
                          Please provide a Tika server host
                        </div>                   
                        </div>
                      </div>
                      <div class="form-group row" id="webTikaOCRPort-div">
                        <div class="col-sm-3 control-label">
                          <label id="webTikaOCRPortLabel" for="webTikaOCRPort" class="col-form-label"></label><span id="webTikaOCRPort-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="webTikaOCRPort" name="webTikaOCRPort" class="form-control"></input>  
                          <div class="invalid-feedback">
                          Please provide a Tika server port
                        </div>                     
                        </div>
                      </div>
                      <div class="form-group row" id="webTikaOCRName-div">
                        <div class="col-sm-3 control-label">
                          <label id="webTikaOCRNameLabel" for="webTikaOCRName" class="col-form-label"></label><span id="webTikaOCRName-tip" class="fas fa-info-circle" data-toggle="tooltip" data-placement="right" title=""></span>
                        </div>
                        <div class="col-sm-4 checkbox-div">
                          <input type="text" id="webTikaOCRName" name="webTikaOCRName" class="form-control"></input>
                          <div class="invalid-feedback">
                          Please provide a Tika server name
                        </div>                       
                        </div>
                      </div>
                    </div>
                    <div class="form-group row" id="div13">
                      <div class="col-sm-3 control-label">
                        <label id="startJobWebLabel" for="startJobWeb" class="col-form-label"></label>
                      </div>
                      <div class="col-sm-4 checkbox-div">
                        <input type="checkbox" id="startJobWeb" name="startJobWeb"></input>
                      </div>
                    </div>
                    </fieldset>
                    <div class="form-group row" id="div4">
                      <div class="col-sm-3">
                        <label class="MCFSave" class="col-form-label" for=""></label>
                      </div>
                      <div class="col-sm-4">
                        <button type="Submit" id="newWebConfig" name="addProm" class="btn btn-primary" data-loading-text="<i class='fa fa-spinner fa-spin'></i> Saving...">Save</button>
                      </div>
                      <div class="col-sm-3">
                        <i class="fas fa-asterisk" style="color : red"></i>
                        <label class="col-form-label asteriskLabel"></label>
                      </div>
                    </div>
                  </form>
                  <div id="addWebMessageSuccess" class="feedback-message alert alert-success"><i class="fas fa-check"></i>Configuration successfully saved.</div>
            			<div id="addWebMessageFailure" class="feedback-message alert alert-danger">A problem occurred while saving the configuration</div>
                </div>
              </div>
            </div>
          </div>
      </div>
    </fieldset>
  </div>

</body>
</html>
