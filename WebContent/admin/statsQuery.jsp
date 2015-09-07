<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
	<%@ page trimDirectiveWhitespaces="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
<title>Statistiques</title>

		<link rel="icon" type="image/png" href="images/bullet.png">
		<link rel="stylesheet" type="text/css" href="../css/main.css" media="screen" />
		<link rel="stylesheet" type="text/css" href="../css/admin.css" media="screen" />
<link rel="stylesheet" type="text/css" href="../css/style_v2.css" />
<link rel="stylesheet" type="text/css" href="../css/checkbox.css" />
<link rel="stylesheet" type="text/css" href="../css/flexigrid.pack.css" />
	    <script type="text/javascript" src="../js/jquery-1.8.1.min.js"></script>
		<script type="text/javascript" src="../js/menu.js"></script>
</head>
<body class="gecko win" style="background-color : #F0F0F0 ;">

	<script type="text/javascript" src="../js/jquery-1.8.1.min.js"></script>
	<script type="text/javascript" src="../js/function/empty.func.js"></script>
	<script type="text/javascript" src="../js/jquery-ui-1.8.23.min.js"></script>
	<script type="text/javascript" src="../js/jquery.columnizer.js"></script>
	<script type="text/javascript" src="../js/AjaxFranceLabs/i18njs.js"></script>
	<script type="text/javascript" src="../js/AjaxFranceLabs/uuid.core.js"></script>
	<script type="text/javascript" src="../js/AjaxFranceLabs/core/Core.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/core/AbstractModule.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/core/AbstractWidget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/core/AbstractFacetWidget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/core/Parameter.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/core/ParameterStore.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/core/AbstractManager.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/manager/Manager.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/modules/Pager.module.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/modules/Autocomplete.module.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/widgets/ResultStatsQuery.widget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/widgets/ResultStats.widget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/widgets/StatsQuery.widget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/widgets/StatsInformation.widget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/widgets/Table.widget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/widgets/TableFacetQueriesWidget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/widgets/Promolink.widget.js"></script>
	<script type="text/javascript"
		src="../js/AjaxFranceLabs/widgets/Spellchecker.widget.js"></script>
<!-- 	<script type="text/javascript" src="../js/statsQuery.js"></script> -->
	<script type="text/javascript" src="../js/stats.js"></script>
	<script type="text/javascript" src="../js/flexigrid.pack.js"></script>
	<script type="text/javascript" src="../js/menu.js"></script>

<%-- 	<jsp:include page="../header.jsp" /> --%>
<%-- 	<jsp:include page="menu.jsp" /> --%>
	<div id="solr">
		<div id="searchBar"></div>
		<div id="result_information"></div>
		<div id="results"></div>			
		<div class="clear"></div>
	</div>
<%-- 	<jsp:include page="../footer.jsp" /> --%>
</body>
</html>
