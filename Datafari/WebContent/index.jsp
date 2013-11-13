<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8" />
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<title>Recherche</title>
			<link rel="icon" type="image/png" href="images/bullet.png">
		<link rel="stylesheet" type="text/css" href="css/main.css" media="screen" />
			<link rel ="stylesheet" type ="text/css" href ="css/results.css" />
	</head>
	<body class="gecko win">
	    <script type="text/javascript" src="js/jquery-1.8.1.min.js"></script>
	    <script type="text/javascript" src="js/function/empty.func.js"></script>
			<script type ="text/javascript" src ="js/jquery-ui-1.8.23.min.js"></script>

			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/Core.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/AbstractModule.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/AbstractWidget.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/AbstractFacetWidget.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/Parameter.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/ParameterStore.js"></script>

			<script type="text/javascript" src="js/AjaxFranceLabs/i18njs.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/AbstractManager.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/manager/Manager.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/modules/Pager.module.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/modules/Autocomplete.module.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/widgets/Result.widget.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/widgets/SearchBar.widget.js"></script>


			<script type ="text/javascript" src ="js/main.js"></script>

			<script type ="text/javascript" src ="js/searchBar.js"></script>
		

	<jsp:include page="header.jsp" />
	<div id="logo-search"></div>
	<div id="solr">
		<div id="searchBar"></div>
		<div class="clear"></div>
	</div>
	<jsp:include page="footer.jsp" />

</body>
</html>
