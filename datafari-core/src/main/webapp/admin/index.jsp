<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<%@ page import="java.util.Locale" %>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<title>Admin Datafari</title>
		<meta name="description" content="description">
		<meta name="author" content="Admin Datafari">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<link rel="stylesheet" type="text/css" href="../css/mainbis.css" media="screen" />
		<link href="../plugins/bootstrap/bootstrap.css" rel="stylesheet">
		<link href="../plugins/jquery-ui/jquery-ui.min.css" rel="stylesheet">
		<link rel="stylesheet" type ="text/css" href="../css/font-awesome/font-awesome.min.css">
		<link rel="stylesheet" type ="text/css" href="../css/google-fonts/righteous.css">
		<link href="../plugins/fullcalendar/fullcalendar.css" rel="stylesheet">
		<link href="../plugins/xcharts/xcharts.min.css" rel="stylesheet">
		<link href="../plugins/select2/select2.css" rel="stylesheet">
		<link href="../plugins/justified-gallery/justifiedGallery.css" rel="stylesheet">
		<link href="../css/style_v2.css" rel="stylesheet">
		<link href="../plugins/chartist/chartist.min.css" rel="stylesheet">
		<link href="../plugins/CLEditor/jquery.cleditor.css" rel="stylesheet">
		<script type="text/javascript" src="../js/endswith.js"></script>
		<script type="text/javascript" src="../js/startswith.js"></script>
		<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
		<!--[if lt IE 9]>
				<script src="http://getbootstrap.com/docs-assets/js/html5shiv.js"></script>
				<script src="http://getbootstrap.com/docs-assets/js/respond.min.js"></script>
		<![endif]-->
	</head>
<body>
<!--Start Header-->
<div id="screensaver">
	<canvas id="canvas"></canvas>
	<i class="fa fa-lock" id="screen_unlock"></i>
</div>
<div id="modalbox">
	<div class="devoops-modal">
		<div class="devoops-modal-header">
			<div class="modal-header-name">
				<span>Basic table</span>
			</div>
			<div class="box-icons">
				<a class="close-link">
					<i class="fa fa-times"></i>
				</a>
			</div>
		</div>
		<div class="devoops-modal-inner">
		</div>
		<div class="devoops-modal-bottom">
		</div>
	</div>
</div>
<header class="navbar">
	<div class="container-fluid expanded-panel">
		<div class="row">
			<div id="Datafari" class="col-xs-12 col-sm-2">
				<a id="datafariHomePageSearchUiLink">Datafari</a>
			</div>
			<div id="top-panel" class="col-xs-12 col-sm-10">
				<div class="row">
					<div class="col-xs-8 col-sm-4">
						<!--  <img src="../images/datafari.png" width=50% height=50% > -->
					</div>
					<div class="col-xs-4 col-sm-8 top-panel-right">
<!-- 						<a href="#" class="about">about</a> -->

					 	<ul class="nav navbar-nav pull-right panel-menu" >
					 	
					 	<li class="searchLink"><a id="datafariSearchUiLink" class="searchPageLink"></a></li>
<!-- 							<li class="hidden-xs"> -->
<!-- 								<a href="index.html" class="modal-link"> -->
<!-- 									<i class="fa fa-bell"></i> -->
<!-- 									<span class="badge">7</span> -->
<!-- 								</a> -->
<!-- 							</li> -->
<!-- 							<li class="hidden-xs"  > -->
<!-- 								<a class="ajax-link" href="ajax/calendar.html"> -->
<!-- 									<i class="fa fa-calendar"></i> -->
<!-- 									<span class="badge">7</span> -->
<!-- 								</a> -->
<!-- 							</li> -->
<!-- 							<li class="hidden-xs"> -->
<!-- 								<a href="ajax/page_messages.html" class="ajax-link"> -->
<!-- 									<i class="fa fa-envelope"></i> -->
<!-- 									<span class="badge">7</span> -->
<!-- 								</a> -->
<!-- 							</li>  -->

							<li class="dropdown" >
								<a href="#" class="dropdown-toggle account" data-toggle="dropdown">
									<div class="avatar">
										<img src="../images/pic.jpg" class="img-circle" alt="avatar" /> 
									</div>

<!-- 									<i class="fa fa-angle-down pull-right"></i> -->

									<div class="user-mini pull-right" >
										<span id="welcomeAdminUiMsg" class="welcome"></span>

									<span><% out.print(request.getUserPrincipal().getName());%></span>

									</div>
								</a>
 								<ul class="dropdown-menu"> 
<!-- 									<li> -->
<!-- 										<a href="#"> -->
<!-- 											<i class="fa fa-user"></i> -->
<!-- 											<span>Profile</span> -->
<!-- 										</a> -->
<!-- 									</li> -->
<!-- 									<li> -->
<!-- 										<a href="ajax/page_messages.html" class="ajax-link"> -->
<!-- 											<i class="fa fa-envelope"></i> -->
<!-- 											<span>Messages</span> -->
<!-- 										</a> -->
<!-- 									</li> -->
<!-- 									<li> -->
<!-- 										<a href="ajax/gallery_simple.html" class="ajax-link"> -->
<!-- 											<i class="fa fa-picture-o"></i> -->
<!-- 											<span>Albums</span> -->
<!-- 										</a> -->
<!-- 									</li> -->
<!-- 									<li> -->
<!-- 										<a href="ajax/calendar.html" class="ajax-link"> -->
<!-- 											<i class="fa fa-tasks"></i> -->
<!-- 											<span>Tasks</span> -->
<!-- 										</a> -->
<!-- 									</li> -->
<!-- 									<li> -->
<!-- 										<a href="#"> -->
<!-- 											<i class="fa fa-cog"></i> -->
<!-- 											<span>Settings</span> -->
<!-- 										</a> -->
<!-- 									</li> -->
 									<li> 
 										<a onclick="logout();" style="cursor: pointer;"> 
 											<i class="fa fa-power-off"></i> 
 											<span id="logout-AdminUI"></span> 
 										</a> 
 									</li> 
 								</ul> 

								</li> 
						</ul>

					</div>
				</div>
			</div>
		</div>
	</div>
</header>
<!--End Header-->
<!--Start Container-->
<div id="main" class="container-fluid">
	<div class="row">
		<div id="sidebar-left" class="col-xs-2 col-sm-2">
			<ul class="nav main-menu">
<!-- <li class="dropdown" id="User"> -->
<!-- 					<a href="#" class="dropdown-toggle"> -->
<!-- 						<i class="fa fa-male"></i> -->
<!-- 						<span id="myAccount-AdminUI" class="hidden-xs"></span> -->
<!-- 					</a> -->
<!-- 					<ul class="dropdown-menu"> -->
<!-- 						<li id="Alerts"><a id="alerts-AdminUI" class="ajax-link" href="../ajax/Alerts.html"></a></li> -->
<!-- 						<li id="LikesAndFavorites"><a id="favorites-AdminUI" class="ajax-link" href="../ajax/getFavorites.html"></a></li> -->
<!-- 						<li id="SavedSearches"><a id="searches-AdminUI" class="ajax-link" href="../ajax/getSearches.html"></a></li> -->
<!-- 					</ul> -->
<!-- 				</li> -->
<%
if(request.isUserInRole("SearchExpert")||request.isUserInRole("SearchAdministrator")){
%>
<!-- 					<a href="../ajax/dashboard.html" class="ajax-link"> <i -->
				<li class="dropdown" id="Connectors">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-arrows-alt"></i>
						 <span id="connectors-AdminUI" class="hidden-xs"></span>
					</a>					 
					<ul class="dropdown-menu">
						<li id="MCFAdminUI">
							<a href="../ajax/manifoldcf.html" class="ajax-link" id="MCFAdmin-AdminUI"></a>
						</li>
						<li id="MCFBackupRestore"><a id="MCFBackupRestore-AdminUI" class="ajax-link" href="../ajax/mcfBackupRestore.html"></a></li>
						<li id="MCFChangePassword" ><a id="MCFPassword-AdminUI" class="ajax-link" href="../ajax/MCFChangePassword.html"></a></li>
					</ul>
				</li>

				<li class="dropdown" id="Statistics">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-bar-chart"></i>
						 <span id="statistics-AdminUI" class="hidden-xs"></span>
					</a>
					<ul class="dropdown-menu">
						<!-- <li id="onDemandStatistics"><a id="onDemandStatistics-AdminUI" class="ajax-link" href="../ajax/onDemandStatistics.html"></a></li> -->
						<li id="UsageStatistics"><a id="usageStatistics-AdminUI" class="ajax-link" href="../ajax/usageStatistics.html"></a></li>
						<li id="CorpusStatistics"><a id="corpusStatistics-AdminUI" class="ajax-link" href="../ajax/corpusStatistics.html"></a></li>
						<li id="CorpusOTStatistics"><a id="corpusOTStatistics-AdminUI" class="ajax-link" href="../ajax/corpusOTStatistics.html"></a></li>
						<li><a id="queryStatistics-AdminUI" class="ajax-link" href="../ajax/StatsQuery.html"></a></li>
						<li id="ELKConfiguration"><a id="elkConfiguration-AdminUI" class="ajax-link" href="../ajax/elkConfiguration.html"></a></li> 
<!-- 						<li><a class="ajax-link" href="../ajax/systemStatistics.html">System statistics</a></li> -->
					</ul>
				</li>
				<li class="dropdown" id="Admin">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-wrench"></i>
						 <span id="searchEngineAdmin-AdminUI" class="hidden-xs"></span>
					</a>
					<ul class="dropdown-menu">
						<li id="SolrAdmin"><a id="solrAdmin-AdminUI" class="ajax-link" href="../ajax/solr.html"></a></li>
						<li id="AlertAdmin"><a id="alertAdmin-AdminUI" class="ajax-link" href="../ajax/alertsAdmin.html"></a></li>
						<li id="IndexField"><a id="indexField-AdminUI" class="ajax-link" href="../ajax/IndexField.html"></a></li>
						<li id="SchemaAnalysis"><a id="schemaAnalysis-AdminUI" class="ajax-link" href="../ajax/SchemaAnalysis.html"></a></li>
<% 
	if(request.isUserInRole("SearchAdministrator")){
%>
						<li id="SizeLimitation"><a id="sizeLimitation-AdminUI" class="ajax-link" href="../ajax/SizeLimitations.html"></a></li>
						<li id="AutocompleteConfiguration"><a id="autocompleteConfig-AdminUI" class="ajax-link" href="../ajax/AutocompleteConfiguration.html"></a></li>
<%
	}
%>
					</ul>
				</li>
				<li class="dropdown" id="Conf">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-desktop"></i>
						 <span id="searchEngineConfig-AdminUI" class="hidden-xs"></span>
					</a>
					<ul class="dropdown-menu">
 						<li id="QueryElevator"><a id="queryElevator-AdminUI" class="ajax-link" href="../ajax/queryElevator.html"></a></li>
						<li id="PromoLink"><a id="promoLinks-AdminUI" class="ajax-link" href="../ajax/promoLinks.html"></a></li>
						<li id="Synonyms" ><a id="synonyms-AdminUI" class="ajax-link" href="../ajax/Synonyms.html"></a></li>
						<li id="Stopwords"><a id="stopwords-AdminUI" class="ajax-link" href="../ajax/StopWords.html"></a></li> 
						<li id="Protwords"><a id="protwords-AdminUI" class="ajax-link" href="../ajax/ProtWords.html"></a></li> 
						<li id="FieldWeight"><a id="fieldWeight-AdminUI" class="ajax-link" href="../ajax/FieldWeight.html"></a></li>
						<li id="FacetConfig"><a id="facetConfig-AdminUI" class="ajax-link" href="../ajax/FacetConfig.html"></a></li>
						<li id="Deduplication"><a id="deduplication-AdminUI" class="ajax-link" href="../ajax/config_deduplication.html"></a></li>

						<li id="LikesAndFavorites"><a id="likesFavoritesSearchEng-AdminUI" class="ajax-link" href="../ajax/config_likesAndFavorites.html"></a></li>
<% 
	if(request.isUserInRole("SearchAdministrator")){
%>
						<li id="Zookeeper"><a id="zookeeper-AdminUI" class="ajax-link" href="../ajax/config_zookeeper.html"></a></li>
<%
	}
%>
<!-- 						<li><a class="ajax-link" href="../ajax/ui_jquery-ui.html">Query based boosts</a></li> -->
<!-- 						<li><a class="ajax-link" href="../ajax/ui_icons.html">Document based boosts</a></li> -->
<!-- 						<li><a class="ajax-link" href="../ajax/ui_icons.html">OCR: On/Off</a></li> -->
<!-- 						<li><a class="ajax-link" href="../ajax/ui_icons.html">Duplicate identifiers</a></li> -->
<!-- 						<li><a class="ajax-link" href="../ajax/ui_icons.html">User Management</a></li> -->
<!-- 						<li><a class="ajax-link" href="../ajax/ui_icons.html">Autocomplete configuration</a></li> -->
					</ul>
				</li>
<% 
	if(request.isUserInRole("SearchAdministrator")){
%>
			<li class="dropdown" id="Servers">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-database"></i>
						 <span id="servers-AdminUI" class="hidden-xs"></span>
					</a>
					<ul class="dropdown-menu">
						<li id="Tomcat"><a class="ajax-link" href="../ajax/Tomcat.html">Tomcat</a></li>
<!-- 						<li><a class="ajax-link" href="../ajax/Jetty.html">Jetty</a></li> -->
					</ul>
				</li>
				
				<li class="dropdown" id="userManagement">
					<a href="#" class="dropdown-toggle">
					<i class="fa fa-users"></i>
						 <span id="userManagement-AdminUI" class="hidden-xs"></span>
					</a>
					<ul class="dropdown-menu">
						<li id="ADConfiguration"><a id="ADConfig-AdminUI" class="ajax-link" href="../ajax/ldapConfiguration.html"></a></li>
						<li id="modifyUser"><a id="modifyUsers-AdminUI" class="ajax-link" href="../ajax/modifyUsers.html"></a></li>
						<li id="addUser"><a id="addUser-AdminUI" class="ajax-link" href="../ajax/addUser.html"></a></li>
					</ul>
				</li>
<%
	}
}
%>
				
				
				
			</ul>
		</div>
		<!--Start Content-->
		<div id="content" class="col-xs-12 col-sm-10">
			<div id="about">
				<div class="about-inner">
					<h4 class="page-header">Open-source admin theme for you</h4>
					<p>DevOOPS team</p>
					<p>Homepage - <a href="http://devoops.me" target="_blank">http://devoops.me</a></p>
					<p>Email - <a href="mailto:devoopsme@gmail.com">devoopsme@gmail.com</a></p>
					<p>Twitter - <a href="http://twitter.com/devoopsme" target="_blank">http://twitter.com/devoopsme</a></p>
					<p>Donate - BTC 123Ci1ZFK5V7gyLsyVU36yPNWSB5TDqKn3</p>
				</div>
			</div>
			<div id="ajax-content"></div>
		</div>
		<!--End Content-->
		<jsp:include page="./admin-footer.jsp"  />
	</div>
</div>
<!--End Container-->
<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
<!--<script src="http://code.jquery.com/jquery.js"></script>-->
<script src="../plugins/jquery/jquery.min.js"></script>
<script src="../plugins/jquery-ui/jquery-ui.min.js"></script>
<script src="../plugins/CLEditor/jquery.cleditor.js"></script>
<script src="../plugins/CLEditor/jquery.cleditor.min.js"></script>
<script type="text/javascript" src="../js/logout.js"></script>
<script src="./../plugins/jquery-ui-timepicker-addon/jquery-ui-timepicker-addon.js"></script>
<!-- Include all compiled plugins (below), or include individual files as needed -->
<script src="../plugins/bootstrap/bootstrap.min.js"></script>
<script src="../plugins/justified-gallery/jquery.justifiedGallery.min.js"></script>
<script src="./i18nInit.js" ></script>
<script type="text/javascript" src="../js/AjaxFranceLabs/i18njs.js"></script>
<!-- All functions for this theme + document.ready processing -->
<script src="../js/devoops.js"></script>
<!-- JS library useful to extract parameters value from URL  -->
<script type ="text/javascript" src ="../js/url.min.js"></script>
<script type ="text/javascript" src ="./i18nAdminIndex.js"></script>
</body>
</html>
