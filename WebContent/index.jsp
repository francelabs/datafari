<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8" />
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
		<title>Search</title>
			<link rel="icon" type="image/png" href="images/bullet.png">
		<link rel="stylesheet" type="text/css" href="css/main.css" media="screen" />
		<link rel ="stylesheet" type ="text/css" href ="css/results.css" />
		<link rel ="stylesheet" type ="text/css" href ="css/index.css" />
		<link rel ="stylesheet" type ="text/css" href ="css/animate.min.css" />
		<link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
	</head>
	<body class="gecko win">
		
	    <script type="text/javascript" src="js/jquery-1.8.1.min.js"></script>
	    <script type="text/javascript" src="js/function/empty.func.js"></script>
			<script type ="text/javascript" src ="js/jquery-ui-1.8.23.min.js"></script>

			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/Core.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/AbstractModule.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/AbstractWidget.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/Parameter.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/ParameterStore.js"></script>
			<script type="text/javascript" src="js/AjaxFranceLabs/i18njs.js"></script>
			<script type="text/javascript" src="js/AjaxFranceLabs/uuid.core.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/core/AbstractManager.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/manager/Manager.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/modules/Pager.module.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/modules/Autocomplete.module.js"></script>
			<script type ="text/javascript" src ="js/AjaxFranceLabs/widgets/SearchBar.widget.js"></script>


			<script type ="text/javascript" src ="js/main.js"></script>
			<script type ="text/javascript" src ="js/searchBar.js"></script>
		

	<jsp:include page="header.jsp" />
	<div id="content_index">
		<img class="fadeInDown animated" id="logo-search" src="images/datafari.png"/>
		<div id="solr">
			<div id="searchBar"></div>
			<div class="clear"></div>
		</div>
	</div>
	<jsp:include page="footer.jsp" />
	
	<script>
		$(document).ready(function(){
			if ($(window).width()>799)
			var endAnimationEvent = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
			$('footer').hide();
			$('#logo-search').one(endAnimationEvent,function(){
				$('footer').show().addClass('animated fadeInUp');
			});
			$('.searchBar input').focus();
		});
	</script>
	

</body>
</html>
