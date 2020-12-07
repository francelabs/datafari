<%@page import="java.io.Console"%>
<%@ page import="com.francelabs.datafari.utils.DatafariMainConfiguration" %>
<%@ page import="com.francelabs.datafari.utils.CorePropertiesConfiguration" %>
<%@page contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
    <meta name="csrf-param" content="${_csrf.parameterName}">
    <meta name="csrf-token" content="${_csrf.token}">
    <title>Search</title>
    <link rel="icon" type="image/png" href="<c:url value="/resources/images/bullet.png" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/openlayers/6.1.1/ol.css" />"/>
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/bootstrap/4.3.1/css/bootstrap.min.css" />"/>
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/main.css" />" media="screen" />
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/animate.min.css" />" />
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/results.css" />" />
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/font-awesome/5.11.2/css/all.css" />">
    <link rel="stylesheet" type ="text/css" href="<c:url value="/resources/css/header.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/info-modal.css" />" />
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/searchView-status-bar.css" />" />
    <!-- New style -->
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/custom/new-style.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.theme.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/jquery-ui/1.12.1/css/jquery-ui.structure.min.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/preview/preview-displayer.css" />">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/widgets/search-suggest-ui-widget.css" />" media="screen">
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/libs/jquery-datatables/DataTables-1.10.20/css/dataTables.bootstrap4.min.css" />"/>
    <link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/parameters.css" />" media="screen">
    
    <script type="text/javascript" src="<c:url value="/resources/libs/jquery/3.4.1/jquery-3.4.1.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/jquery-waypoints/4.0.1/jquery.waypoints.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/popper/1.16.0/popper.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/bootstrap/4.3.1/js/bootstrap.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/jquery-ui/1.12.1/jquery-ui.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/history.ielte7.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/libs/openlayers/6.1.1/ol.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/function/empty.func.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/polyfill.js" />"></script>
    
    <script type="text/javascript" src="<c:url value="/resources/js/searchSessionTimeout.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/uuid.core.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/i18njs.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/Core.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractModule.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractWidget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractFacetWidget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/Parameter.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/ParameterStore.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/core/AbstractManager.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/manager/Manager.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/Pager.module.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/HierarchicalPager.module.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/Autocomplete.module.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/AdvancedAutocomplete.module.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/DateSelectorFacet.module.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/NumberSelectorFacet.module.js" />"></script>
<%--     <script type="text/javascript" src="<c:url value="/resources/js/search-external-sources.js" />"></script> --%>
<%--     <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/externalSources/GenericResultsDisplayer.js" />"></script> --%>
<%--     <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/externalSources/ExternalSourceCore.js" />"></script> --%>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/preview/PreviewDisplayer.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/Result.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/SearchBar.widget.js" />"></script>
    <script type="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/widgets/AdvancedSearch.widget.js" />"></script>
    <script type="text/javascript" src ="<c:url value="/resources/js/AjaxFranceLabs/widgets/AdvancedSearchWidget/AdvancedSearchField.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/SearchInformation.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/Table.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/FacetDuplicates.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/TableFacetQueriesWidget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/Promolink.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/TableMobile.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/tagCloud.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/Tab.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/PrevisualizeResult.widget.js" />" charset="utf-8"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/Aggregator.widget.js" />" charset="utf-8"></script>

    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/Spellchecker.widget.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/SubClassResult.widget.js" />" charset="utf-8"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/LikesAndFavorites.widget.js" />"></script>
    <!-- comment the tag bellow if you don't use illustrate mode for desktop and mobile -->
<%--     <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/ResultIllustrated.widget.js" />"></script> --%>
    <!-- comment the two script tag bellow if you autocompleteIllustrated isn't used -->
<%--   <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/SearchBarIllustrated.widget.js" />"></script> --%>
<%--   <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/AutocompleteIllustrated.module.js" />"></script> --%>
    <script type="text/javascript" src="<c:url value="/resources/customs/js/customFieldsForResults.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/main.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/desktop_design.js" />"></script>

    <!-- Enable the queryElevator module if the user has the "SearchAdministrator" role -->
  <%
    if (request.getUserPrincipal() != null) {
        if (request.getUserPrincipal().getName() != null) {
          if(request.isUserInRole("SearchAdministrator") || request.isUserInRole("SearchExpert")) {
  %>
            <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/QueryElevator.module.js" />"></script>
            <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/modules/RelevancyQuery.module.js" />"></script>
  <%
    }
        }
      }
  %>
  <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/SaveSearch.widget.js" />"></script>
    
  <%
  if (CorePropertiesConfiguration.getInstance().getProperty(CorePropertiesConfiguration.DEDUPLICATION)!=null && CorePropertiesConfiguration.getInstance().getProperty(CorePropertiesConfiguration.DEDUPLICATION ).equals("true") ){
  %>
    <script type="text/javascript" src="<c:url value="/resources/js/doublon.js" />"></script>
  <%
  }
  %>
  <%
  if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES)!=null && DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES).equals("true") ){
  %>
    <script type="text/javascript"> window.isLikesAndFavoritesEnabled = true</script>
  <%
  }else{ 
  %>
    <script type="text/javascript"> window.isLikesAndFavoritesEnabled = false</script>
  <%
  }
  %>
  
    <script type="text/javascript" src="<c:url value="/resources/customs/js/customWidgetsBuild.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/customs/js/legitWidgetsToRemove.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/search.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/nav_mobile.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/AjaxFranceLabs/widgets/SearchSuggestUI.widget.js" />"></script>

</head>
<body class="gecko win">
	<jsp:include page="/resources/modals/infoModal.jsp" />
	<jsp:include page="/resources/customs/html/customs_js_files.jsp" />
	<jsp:include page="header.jsp" />
	<div id="facets_mobile" style="">
		<div id="nav_mobile" class="bc-color">
		<!-- Faut mettre un truc dynamique par rapport à la langue -->
			<a href="#previous_mobile"><i class="fas fa-chevron-left"></i> </a>
		</div>
		<div id="facet_source_mobile"></div>
		<div id="facet_type_mobile"></div>
  </div>
	<div id="solr">
    <div class="search-view-ui search-view-header">
  		<div id="searchBar" class="search-view-header">
      </div>
    </div>
    <div id="search-sources-tabs" class="search-view-ui">
		  <ul id="search-sources-list" class="force-hide nav nav-tabs">
		    <li class="search-source nav-item active"><a id="datafari-search-link" class="nav-link"><img alt="" src="<c:url value="/resources/images/datafari.png" />"></a></li>
		    <li class="search-source nav-item force-hide"><a id="dropbox-search-link" class="nav-link"><img alt="" src="<c:url value="/resources/images/Dropbox_logo_2017.svg" />"></a></li>
		  </ul>
		</div>
		<div id="facet_tabs" class="search-view-ui search-view-header results-elm"></div>
    <div id="status-div" class="search-view-ui search-view-header results-elm">
      <div id="search_information"></div>
      <div id="results_action">
        <!-- Enable the SaveSearch widget if the user is authenticated -->
        <%
          if (request.getUserPrincipal() != null) {
            if (request.getUserPrincipal().getName() != null) {
        %>
              <span id="save_search" class="pull-right"></span>
        <%
            }
          }
        %>
        <jsp:include page="./resources/customs/html/custom_results_action_elms.jsp" />
      </div>
    </div>
		<div id="advancedSearch" class ="header-menu-ui force-hide"></div>
		<div id="parametersUi" class ="header-menu-ui force-hide"><jsp:include page="parameters.jsp" /></div>
		<div id="favoritesUi" class ="header-menu-ui force-hide"><jsp:include page="favorites.jsp" /></div>
<%-- 		<div id="externalSourcesUi" class ="header-menu-ui force-hide"><jsp:include page="external-sources.jsp" /></div> --%>
<!-- 		<div id="external-results_div" class="search-view-ui external-results-elm" style="display: none;"> -->
<!--       <div id="external_results_nav_mobile"  class="bc-color"> -->
<!--         <div id="number_external_results_mobile"><span></span></div> -->
<!--         <div id="external_nav_facets_mobile"><a href="#external_facets_mobile"><i class="fas fa-bars"></i> </a></div> -->
<!--       </div> -->
<!-- 		  <div class="external-results-infos"></div> -->
<!-- 		  <div class="doc_list"></div> -->
<!-- 		  <div class="doc_list_pagination"></div> -->
<!-- 		</div> -->
		<div id="results_div" class="search-view-ui results-elm">
			<div id="results_nav_mobile"  class="bc-color">
				<div id="number_results_mobile"><span></span></div>
				<div id="nav_facets_mobile"><a href="#facets_mobile"><i class="fas fa-bars"></i> </a></div>
			</div>
			<div id="tab_line"></div>

			<div class="col left">
				<div id="facets">
				  <jsp:include page="./resources/customs/html/custom_facets_div.jsp" />
          <div id="facet_slider"></div>
          <div id="facet_aggregator"></div>
					<!--  <div id="facet_last_modified"></div> -->
					<div id="facet_creation_date"></div>
					<div id="facet_extension"></div>
					<!-- To enable Facet Hierarchical widget -->
					<!--  <div id="facet_hierarchical_url"></div> -->
					<!-- To enable entity extraction -->
					<div id="facet_entity_phone_present"></div>
					<div id="facet_entity_phone"></div>
          <div id="facet_entity_person"></div>
          <div id="facet_entity_special_present"></div>

					<div id="facet_source"></div>
					<div id="facet_language"></div>
					<div id="facet_signature"></div>
					<div id="facet_file_size"></div>
					<div id="facet_tag_cloud"></div>
				</div>
			</div>
			<div class="col right">

				<div id="promolink"></div>
				<div id="suggestion"></div>
				<div id="spellchecker"></div>
				<!--  <div id="external"></div> -->
			  	<div id="previsualize"></div>
				<div id="results"><div id='relevancy-div' style="display: none;"></div></div>
			</div>

			<div id="search-suggest-ui">
			</div>

			<div class="clear"></div>
		</div>
	</div>
	<jsp:include page="footer.jsp"/>
	<!--  <div id="spinner_mobile"><i class="fas fa-spinner fa-spin"></i></div>-->

</body>
</html>
