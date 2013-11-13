<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
<title>Recherche</title>
<link rel="icon" type="image/png" href="images/bullet.png">
<link rel="stylesheet" type="text/css" href="css/main.css"
	media="screen" />
<link rel="stylesheet" type="text/css" href="css/results.css" />
</head>
<body class="gecko win">
	<script type="text/javascript" src="js/jquery-1.8.1.min.js"></script>
	<script type="text/javascript" src="js/function/empty.func.js"></script>
	<script type="text/javascript" src="js/jquery-ui-1.8.23.min.js"></script>

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
		src="js/AjaxFranceLabs/widgets/TableFacetQueriesWidget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Capsule.widget.js"></script>
	<script type="text/javascript"
		src="js/AjaxFranceLabs/widgets/Spellchecker.widget.js"></script>

	<script type="text/javascript" src="js/main.js"></script>

	<script type="text/javascript" src="js/search.js"></script>


	<jsp:include page="header.jsp" />
	<div id="solr">
		<div id="searchBar"></div>
		<div id="result_information"></div>

		<div class="col left">
			<div id="facets">
				<div id="facet_date"></div>
				<div id="facet_type"></div>
				<div id="facet_source"></div>
				<div id="facet_language"></div>
			</div>
		</div>
		<div class="col right">
			<div id="spellchecker"></div>
			<div id="results"></div>
		</div>
		<div class="clear"></div>
	</div>
	<jsp:include page="footer.jsp" />

</body>
</html>
