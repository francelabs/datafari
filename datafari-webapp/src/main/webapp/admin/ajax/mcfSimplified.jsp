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
                      <div class="col-sm-4" id="security-div">
	                      <input type="checkbox" id="security" name="security" disabled></input>	                      
                      </div>
                    </div>
                    <div class="form-group row" id="div16">
                      <div class="col-sm-3 control-label">
                        <label id="startJobLabel" for="startJob" class="col-form-label"></label>
                      </div>
                      <div class="col-sm-4" id="startJob-div">
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
                    <div class="form-group row" id="div13">
                      <div class="col-sm-3 control-label">
                        <label id="startJobWebLabel" for="startJobWeb" class="col-form-label"></label>
                      </div>
                      <div class="col-sm-4" id="startJobWeb-div">
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
