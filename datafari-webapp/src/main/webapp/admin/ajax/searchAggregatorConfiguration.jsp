<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/admin/searchAggregatorConfiguration.css" />" />
<script src="<c:url value="/resources/js/admin/ajax/searchAggregatorConfiguration.js" />" type="text/javascript"></script>
<meta charset="UTF-8">
<title>Search Aggregator Configuration</title>
<Link rel="stylesheet" href="<c:url value="/resources/css/animate.min.css" />"/>
</head>
<body>
  <nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3"></li>
    </ol>
  </nav>
  <div class="col-sm-12"><span id="globalAnswer"></span></div>
  <div class="col-sm-12"></div>
  <div class="box">
    <div class="box-header">
      <div class="box-name">
        <i class="fas fa-table"></i><span  id="title"></span>
      </div>
      <div class="box-icons">
        <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
      </div>
      <div class="no-move"></div>
    </div>
    <div id="thBox" class="box-content">
      <div class="documentation-style-no-margin">
        <p class="documentation-preview"> <span id="documentation-search-aggregator-configuration"></span><a class="documentation-link" href="https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/1064861698/SearchAggregator+-+Enterprise+Edition" target="_blank"> ...see more</a></p>
      </div>
      <div id="search-aggregator-client-box" class="box">
        <div class="box-header">
          <div class="box-name">
            <i class="fas fa-table"></i><span  id="searchAggregatorClientConfig"></span>
          </div>
          <div class="box-icons">
            <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
          </div>
          <div class="no-move"></div>
        </div>
        <div id="thBox" class="box-content">
      
          <div class="form-group row">
            <label id="searchAggregatorRenewPasswordLabel" class="col-sm-4 col-form-label"></label>
            <div class="col-sm-3">
              <button id="renew_search_aggregator_secret" name="renew_search_aggregator_secret" class="btn btn-primary"></button>
            </div>  
          </div>
          <div class="col feedback-message alert" id="search-aggregator-secret-message"></div>
          <div class="col feedback-message alert" id="search-aggregator-secret-message2"></div>
        </div>
      </div>
      <div id="search-aggregator-server-box" class="box">
        <div class="box-header">
          <div class="box-name">
            <i class="fas fa-table"></i><span  id="searchAggregatorServerConfig"></span>
          </div>
          <div class="box-icons">
            <a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
          </div>
          <div class="no-move"></div>
        </div>
        <div id="thBox" class="box-content">
          <div class="form-group row">
            <label id="searchAggregatorActivationLabel" class="col-sm-4 col-form-label"></label>
            <div class="col-sm-3">
              <input type="checkbox" id="search_aggregator_activation" name="search_aggregator_activation" data-size="sm" data-toggle="toggle" data-onstyle="success" data-offstyle="danger">
            </div>  
          </div>
          <div class="col feedback-message alert" id="search-aggregator-message"></div>
          <form id="timeout-form" class="needs-validation" novalidate>
          <fieldset>
              <legend id="searchAggregatorTimeouts"></legend>
              <div class="form-group row">
                <label id="timeoutPerRequestLabel" class="col-sm-3 col-form-label"></label>
                <div class="col-sm-3">
                  <input type="number"  id="timeoutPerRequest" name="timeoutPerRequest" min="1000" class="form-control">
                  <div class="invalid-feedback">
                    Please provide a valid timeout
                  </div>
                </div>
              </div>
              <div class="form-group row">
                <label id="globalTimeoutLabel" class="col-sm-3 col-form-label"></label>
                <div class="col-sm-3">
                  <input type="number"  id="globalTimeout" name="globalTimeout" min="1" class="form-control">
                  <div class="invalid-feedback">
                    Please provide a valid timeout
                  </div>
                </div>
              </div>
              <div class="form-group row">
                <label id="saveTimeoutsLabel" class="col-sm-3 col-form-label">
                </label>
                <div class="col-sm-3">
                  <button id="submitTimeouts" name="submitTimeouts" class="btn btn-primary"></button>
                </div>
              </div>
              <div class="col feedback-message alert" id="timeouts-message"></div>
            </fieldset>
          </form>
          <form id="external-datafaris-form" class="needs-validation" novalidate>
            <fieldset>
              <legend id="EXDatafaris"></legend>
                <select id="select-ex-datafari" class="form-control">
                  <OPTION value="" id="label-select-ex-datafari" disabled selected></OPTION>
                  <OPTION value="new" id="label-new-ex-datafari">New</OPTION>
                </select>
            </fieldset>
            <fieldset id="fs-parameters" style="display: none;">
              <legend id="ExDatafariParameters"></legend> 
              <div>
                <div class="form-group row">
                  <label id="datafariNameLabel" class="col-sm-4 col-form-label"></label>
                  <div class="col-sm-5">
                    <input type="text"  id="datafariName" name="datafariName" class="form-control">
                    <div class="invalid-feedback">
                      Please provide a Name
                    </div>
                  </div>
                </div>
                <div class="form-group row">
                  <label id="searchAPIUrlLabel" class="col-sm-4 col-form-label"></label>
                  <div class="col-sm-5">
                    <input type="text" id="search_api_url" name="search_api_url" class="form-control"  placeholder="http://localhost:8080/Datafari/api/search">
                    <div class="invalid-feedback">
                      Please provide a search API url
                    </div>
                  </div>
                </div>
                <div class="form-group row">
                  <label id="tokenRequestUrlLabel" class="col-sm-4 col-form-label"></label>
                  <div class="col-sm-5">
                    <input type="text" id="token_request_url" name="token_request_url" class="form-control"  placeholder="http://localhost:8080/Datafari/oauth/token">
                    <div class="invalid-feedback">
                      Please provide a token request url
                    </div>
                  </div>
                </div>
                <div class="form-group row">
                  <label id="searchAggregatorSecretLabel" class="col-sm-4 col-form-label"></label>
                  <div class="col-sm-5">
                    <input type="password" id="search_aggregator_secret" name="search_aggregator_secret" class="form-control">
                    <div class="invalid-feedback">
                      Please provide a password for the search-aggregator client
                    </div>
                    <div class="valid-feedback">
                      Connection working
                    </div>
                  </div>
                  <div id="pwd-spinner" style="display:none;" class="col-sm-1 form-control">
                    <i style="" class="fas fa-spinner fa-spin"></i>
                  </div>
                </div>
                <div class="form-group row">
                  <label id="datafariActivationLabel" class="col-sm-4 col-form-label"></label>
                  <div class="col-sm-5">
                    <input type="checkbox" id="ex_datafari_activation" name="ex_datafari_activation" data-size="sm" data-toggle="toggle" data-onstyle="success" data-offstyle="danger">
                  </div>  
                </div>
                <br/>
                <div class="form-group row">
                  <label id="saveLabel" class="col-sm-3 col-form-label">
                  </label>
                  <div class="col-sm-3">
                    <button id="submit" disabled name="submitth" class="btn btn-primary"></button>
                  </div>
                </div>
                <div class="form-group row delete-group">
                  <label id="deleteLabel" class="col-sm-3 col-form-label">
                  </label>
                  <div class="col-sm-3">
                    <button id="delete" type="button" name="deleteth" class="btn btn-primary"></button>
                  </div>
                </div>
              
               <br/>
              
              </div>
            </fieldset>
          </form>

          <form id="default-datafari-form" class="needs-validation" novalidate>
            <fieldset>
              <legend id="defaultEXDatafaris"></legend>
              <select id="select-default-ex-datafari" class="form-control">
                <OPTION value="" id="label-select-default-ex-datafari" disabled></OPTION>
              </select>
            </fieldset>
            <fieldset id="fs-save-default">
              <div class="form-group row">
                <label id="saveDefaultLabel" class="col-sm-3 col-form-label">
                </label>
                <div class="col-sm-3">
                  <button id="submit-default" name="submitth" class="btn btn-primary"></button>
                </div>
              </div>

              <br/>
            </fieldset>
            <div id="current-default">
              <span id="current-default-label"></span>
              <div id="current-default-value"></div>
            </div>
          </form>
          <div class="form-group row">
            <label id="searchAggregatorAlwaysUseDefaultLabel" class="col-sm-4 col-form-label"></label>
            <div class="col-sm-3">
              <input type="checkbox" id="search_aggregator_always_use_default" name="search_aggregator_always_use_default" data-size="sm" data-toggle="toggle" data-onstyle="success" data-offstyle="danger">
            </div>  
          </div>
          <div class="col feedback-message alert" id="message"></div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>