<%@page import="java.io.Console"%>
<%@ page import="com.francelabs.datafari.utils.DatafariMainConfiguration" %>
<%@ page import="com.francelabs.datafari.utils.CorePropertiesConfiguration" %>
<%@page contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<title>Search</title>
<link rel="icon" type="image/png" href="images/bullet.png">
<link rel="stylesheet" type="text/css" href="plugins/bootstrap/bootstrap.css"/>
<link rel="stylesheet" type="text/css" href="css/main.css"
	media="screen" />
<link rel="stylesheet" type="text/css" href="css/animate.min.css" />
<link rel="stylesheet" type="text/css" href="css/results.css" />
<link rel="stylesheet" type ="text/css" href="plugins/font-awesome/css/all.css">
<link rel="stylesheet" type="text/css" href="css/searchView-status-bar.css" />
<script type="text/javascript" src="js/history.ielte7.min.js"></script>
<script type="text/javascript" src="js/jquery-1.8.1.min.js"></script>
<!-- comment the tag bellow if you don't use illustrate mode for desktop and mobile -->
<!-- <link rel="stylesheet" type="text/css" href="css/results-illustrate.css" />  -->
<!--[if lt IE 9]>
	<script type="text/javascript" src="js/html5shiv-printshiv.js"></script>
	<link rel="stylesheet" type="text/css" media="all" href="css/results-template-ie.css"/>
		<script>
			$(document).ready(function(){
				if ($(window).width()< 840){
					$('head').append('<link rel="stylesheet" href="css/results-template-ie-lower.css" type="text/css" />');
				}
			});
		</script>
<![endif]-->
<!--[if IE 9]>
	<style>
		.searchBarWidget #sortMode:after{
		display:none;
		}
	</style>
<![endif]-->

<link rel="stylesheet" type ="text/css" href="css/custom/customSearchView.css">
<!-- New style -->
<link rel="stylesheet" type="text/css" href="css/custom/new-style.css">

<link rel="stylesheet" type ="text/css" href="css/jquery-ui-1.11.4/jquery-ui.min.css">
<link rel="stylesheet" type ="text/css" href="css/jquery-ui-1.11.4/jquery-ui.theme.min.css">
<link rel="stylesheet" type ="text/css" href="css/jquery-ui-1.11.4/jquery-ui.structure.min.css">
<link rel="stylesheet" type ="text/css" href="css/preview/preview-displayer.css">

<!-- <link rel="stylesheet" type ="text/css" href="css/widgets/slider-widget.css"> -->

</head>
<body class="gecko win">


	<script type="text/javascript" src="plugins/openlayers/OpenLayers.js"></script>
	<script type="text/javascript" src="js/function/empty.func.js"></script>
	<script type="text/javascript" src="js/jquery-ui-1.11.4/jquery-ui.js"></script>
	<script type="text/javascript" src="js/jquery.waypoints.min.js"></script>
	<script type="text/javascript" src="js/polyfill.js"></script>
  <script type="text/javascript" src="plugins/bootstrap/bootstrap.min.js"></script>

	<script type="text/javascript" src="js/AjaxFranceLabs/uuid.core.js"></script>
	<script type="text/javascript" src="js/AjaxFranceLabs/i18njs.js"></script>
	<script type="text/javascript" src="js/AjaxFranceLabs/core/Core.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/core/AbstractModule.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/core/AbstractWidget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/core/AbstractFacetWidget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/core/Parameter.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/core/ParameterStore.js"></script>

	<script type="text/javascript"
		src="js/AjaxFranceLabs/core/AbstractManager.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/manager/Manager.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/modules/Pager.module.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/modules/HierarchicalPager.module.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/modules/Autocomplete.module.js"></script>
		<script type="text/javascript"
		src="js/AjaxFranceLabs/modules/DateSelectorFacet.module.js"></script>
 	<script type="text/javascript"
    src="js/AjaxFranceLabs/preview/PreviewDisplayer.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Result.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/SearchBar.widget.js"></script>
	<script type ="text/javascript" src ="js/AjaxFranceLabs/widgets/AdvancedSearch.widget.js"></script>
	<script type ="text/javascript" src ="js/AjaxFranceLabs/widgets/AdvancedSearchWidget/AdvancedSearchField.js"></script>

	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/SearchInformation.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Table.widget.js"></script>

		<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/FacetDuplicates.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/TableFacetQueriesWidget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Promolink.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/TableMobile.widget.js"></script>
<!-- 	<script type="text/javascript" -->
<!-- 		src="js/AjaxFranceLabs/widgets/Tab.widget.js"></script> -->
	<!--
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/HierarchicalFacet.js"></script>
	-->
	
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/PrevisualizeResult.widget.js" charset="utf-8"></script>
	
	<!--
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/ExternalResult.widget.js" charset="utf-8"></script>
	-->
<!-- 	<script type="text/javascript" -->
<!-- 		src="js/AjaxFranceLabs/widgets/OntologySuggestion.widget.js"></script> -->

<!-- <script type="text/javascript" -->
<!-- 		src="js/AjaxFranceLabs/widgets/Slider.widget.js"></script> -->

	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Spellchecker.widget.js"></script>
	<!-- comment the tag bellow if you don't use illustrate mode for desktop and mobile -->
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/ResultIllustrated.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/SubClassResult.widget.js" charset="utf-8"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/LikesAndFavorites.widget.js"></script>
	<!-- comment the two script tag bellow if you autocompleteIllustrated isn't used -->
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/SearchBarIllustrated.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/modules/AutocompleteIllustrated.module.js"></script>
	<script type="text/javascript" src="js/main.js"></script>
	<script type="text/javascript" src="js/desktop_design.js"></script>

	<!-- Enable the queryElevator module if the user has the "SearchAdministrator" role -->
	<%
	  if (request.getUserPrincipal() != null) {
				if (request.getUserPrincipal().getName() != null) {
					if(request.isUserInRole("SearchAdministrator") || request.isUserInRole("SearchExpert")) {
	%>
 					<script type="text/javascript" 
 					src="js/AjaxFranceLabs/modules/QueryElevator.module.js"></script> 
	<%
 	  }
 				}
 			}
 	%>




<%
  if (CorePropertiesConfiguration.getInstance().getProperty(CorePropertiesConfiguration.DEDUPLICATION)!=null && CorePropertiesConfiguration.getInstance().getProperty(CorePropertiesConfiguration.DEDUPLICATION ).equals("true") ){
%>
	<script type="text/javascript" src="js/doublon.js"></script>
	<%
	  }
	%>
<%
  if (DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES)!=null && DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.LIKESANDFAVORTES).equals("true") ){
%>
	<script type="text/javascript"> window.isLikesAndFavoritesEnabled = true</script>
<%}else{ %>
	<script type="text/javascript"> window.isLikesAndFavoritesEnabled = false</script>
<%}%>
	<script type="text/javascript" src="customs/js/customWidgetsBuild.js"></script>
	<script type="text/javascript" src="js/search.js"></script>
	<script type="text/javascript" src="js/nav_mobile.js"></script>
	<jsp:include page="customs/html/customs_js_files.jsp" />
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
    <div id="status-div" class="search-view-ui search-view-header">
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
        <jsp:include page="customs/html/custom_results_action_elms.jsp" />
      </div>
    </div>
		<div id="advancedSearch" class ="header-menu-ui force-hide"></div>
		<div id="parametersUi" class ="header-menu-ui force-hide"><jsp:include page="parameters.jsp" /></div>
		<div id="favoritesUi" class ="header-menu-ui force-hide"><jsp:include page="favorites.jsp" /></div>
		<div id="results_div" class="search-view-ui">
			<div id="results_nav_mobile"  class="bc-color">
				<div id="number_results_mobile"><span></span></div>
				<div id="nav_facets_mobile"><a href="#facets_mobile"><i class="fas fa-bars"></i> </a></div>
			</div>
			<div id="tab_line"></div>

			<div id="source_tabs"></div>

			<div class="col left">
				<div id="facets">
				<jsp:include page="customs/html/custom_facets_div.jsp" />
					<div id="facet_slider"></div>
					<div id="facet_last_modified"></div>
					<div id="facet_extension"></div>
					<!-- To enable Facet Hierarchical widget -->
					<!--  <div id="facet_hierarchical_url"></div> -->
					<div id="facet_source"></div>
					<div id="facet_language"></div>
					<div id="facet_signature"></div>
					<div id="facet_file_size"></div>
				</div>
			</div>
			<div class="col right">

				<div id="promolink"></div>
				<div id="suggestion"></div>
				<div id="spellchecker"></div>
				<!--  <div id="external"></div> -->
				<div id="previsualize"></div>
				<div id="results"></div>
			</div>

			<div class="clear"></div>
		</div>
	</div>
	<jsp:include page="footer.jsp"/>
	<!--  <div id="spinner_mobile"><i class="fas fa-spinner fa-spin"></i></div>-->

</body>
</html>
