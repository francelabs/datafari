<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
	<%@ page import="com.francelabs.datafari.utils.ScriptConfiguration" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<title>Search</title>
<link rel="icon" type="image/png" href="images/bullet.png">
<link rel="stylesheet" type="text/css" href="css/main.css"
	media="screen" />
<link rel="stylesheet" type="text/css" href="css/animate.min.css" />
<link rel="stylesheet" type="text/css" href="css/results.css" />
<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
<script type="text/javascript" src="js/history.ielte7.min.js"></script>
	<script type="text/javascript" src="js/jquery-1.8.1.min.js"></script>
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
	

</head>
<body class="gecko win">
	
	<script type="text/javascript" src="js/function/empty.func.js"></script>
	<script type="text/javascript" src="js/jquery-ui-1.8.23.min.js"></script>
	<script type="text/javascript" src="js/jquery.waypoints.min.js"></script>

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
		src="js/AjaxFranceLabs/modules/Autocomplete.module.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Result.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/SearchBar.widget.js"></script>

	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/SearchInformation.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Table.widget.js"></script>
		<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/FacetDuplicates.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/TableFacetQueriesWidget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Capsule.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/TableMobile.widget.js"></script>
	
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Spellchecker.widget.js"></script>

	<script type="text/javascript" src="js/main.js"></script>
	<script type="text/javascript" src="js/desktop_design.js"></script>
	



<%
 if (ScriptConfiguration.getProperty("DEDUPLICATION_IS_ENABLED")!=null && ScriptConfiguration.getProperty("DEDUPLICATION_IS_ENABLED").equals("true") ){
%>
	<script type="text/javascript" src="js/doublon.js"></script>
	<%} %>
	<script type="text/javascript" src="js/search.js"></script>
	<script type="text/javascript"
		src="js/nav_mobile.js"></script>

	<jsp:include page="header.jsp" /> 
	<div id="facets_mobile" style="">
		<div id="nav_mobile" class="bc-color">
		<!-- Faut mettre un truc dynamique par rapport à la langue -->
			<a href="#previous_mobile"><i class="fa fa-chevron-left"></i> Previous</a>
		</div>
		<div id="facet_source_mobile"></div>
		<div id="facet_type_mobile"></div>
    </div>
	<div id="solr">
		<div id="searchBar"><img src="css/images/logo_zebre.png"/></div>
		<div id="results_nav_mobile"  class="bc-color">
			<div id="number_results_mobile"><span></span> résultats</div>
			<div id="nav_facets_mobile"><a href="#facets_mobile"><i class="fa fa-bars"></i> Filtrer</a></div>
		</div>
		<div id="result_information"></div>

		<div class="col left">
			<div id="facets">
				<div id="facet_date"></div>
				<div id="facet_type"></div>
				<div id="facet_source"></div>
				<div id="facet_language"></div>
				<div id="facet_signature"></div>
			</div>
		</div>
		<div class="col right">
			<div id="capsule"></div>
			<div id="spellchecker"></div>
			<div id="results"></div>
		</div>
		<div class="clear"></div>
	</div>
	<jsp:include page="footer.jsp"/> 
	<div id="spinner_mobile"><i class="fa fa-spinner fa-spin"></i></div>

</body>
</html>
