<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
  <head>
    <meta charset="utf-8" />
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
    <title>Preview</title>
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/css/preview/previewUI.css" />">
  </head>
  <body>
    <script type="text/javascript" src="<c:url value="/resources/libs/jquery/3.4.1/jquery-3.4.1.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/polyfill.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/popper/1.16.0/popper.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/bootstrap/4.3.1/js/bootstrap.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/jquery-ui/1.12.1/jquery-ui.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/uuid.core.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/i18njs.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/Core.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractWidget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/previewUI.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/logout.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/customs/js/preview/customWidgetsBuild.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/customs/js/preview/legitWidgetsToRemove.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/manager/PreviewManager.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/PreviewRequestWidget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/PreviewContentBuilder-Widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/WidgetCore.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/FacetCore.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/DocInfos-Widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/PreviewNavigation-Widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/ShareLinks-Widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/Properties-Widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/widgets/QueryHighlighting-Widget.js" />"></script>
    <jsp:include page="./resources/customs/html/preview/custom_js_files.jsp" />
    <jsp:include page="header.jsp" />
    <div id="loading-div"></div>
    <div id="preview-error" style="display: none"></div>
    <div id="previewUI" class="" style="display: none">
      <!--Start Header section-->
      <div class="header">
        <div class="section-m">&nbsp;</div>
        <div class="section-xxl">
          <div id="doc-infos-widget"></div>
          <jsp:include page="./resources/customs/html/preview/custom-header-div.jsp"></jsp:include>
        </div>
      </div>
      <!--End Header section-->
      <!-- Container area with three section -->
      <div class="section-container">
        <!-- left side bar area -->
        <div class="left-side-bar section-m">
          <div id="doc-properties"></div>
          <jsp:include page="./resources/customs/html/preview/custom-section-left-div.jsp" ></jsp:include>
        </div>
        <!-- End left side bar -->
        <!-- Inner area -->
        <div class="inner-area section-xl">
          <div id="docContent" class="inner-content"></div>
        </div>
        <!-- End Inner area -->
        <!-- Right side bar -->
        <div class="right-side-bar section-l">
          <div id="query-highlighting"></div>
          <div id="preview-nav"></div>
          <div id="share-links"></div>
          <div class="box" style="display: none;">
            <div class="box-heading">
              <i class="fas fa-chevron-down"></i> <b>Document Entities</b>
            </div>
            <div class="box-content box-content-addition">
              <div class="checkbox-conatiner">
                <div class="checkboxes"> <input type="checkbox" /> Names </div>
                <div style="background-color:#8c6239" class="color-bar"></div>
              </div>
              <div class="checkbox-conatiner">
                <div class="checkboxes"> <input type="checkbox" /> Bith dates </div>
                <div style="background-color:#f69679" class="color-bar"></div>
              </div>
              <div class="checkbox-conatiner">
                <div class="checkboxes"> <input type="checkbox" /> IMEI </div>
                <div style="background-color:#f7941d" class="color-bar"></div>
              </div>
              <div class="checkbox-conatiner">
                <div class="checkboxes"> <input type="checkbox" /> IBAN </div>
                <div style="background-color:#fff200" class="color-bar"></div>
              </div>
              <div class="checkbox-conatiner">
                <div class="checkboxes"> <input type="checkbox" /> Licence Plates </div>
                <div style="background-color:#959595" class="color-bar"></div>
              </div>
              <div class="checkbox-conatiner">
                <div class="checkboxes"> <input type="checkbox" /> Addresses </div>
                <div style="background-color:#ed145b" class="color-bar"></div>
              </div>
              <div class="checkbox-conatiner">
                <div class="checkboxes"> <input type="checkbox" /> Phone Numbers </div>
                <div style="background-color:#0072bc" class="color-bar"></div>
              </div>
            </div>
          </div>
          <jsp:include page="./resources/customs/html/preview/custom-section-right-div.jsp" ></jsp:include>
        </div>
        <!-- End Right side bar -->
      </div>
    </div>
  </body>
</html>