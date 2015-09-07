<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.ResourceBundle"  %>
<%@ page import="java.util.Locale" %>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta charset="utf-8">
		<title>Admin Datafari</title>
		<meta name="description" content="description">
		<meta name="author" content="Admin Datafari">
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<link rel="stylesheet" type="text/css" href="../css/mainbis.css" media="screen" />
		<link href="../plugins/bootstrap/bootstrap.css" rel="stylesheet">
		<link href="../plugins/jquery-ui/jquery-ui.min.css" rel="stylesheet">
		<link href="http://maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css" rel="stylesheet">
		<link href='http://fonts.googleapis.com/css?family=Righteous' rel='stylesheet' type='text/css'>
		<link href="../plugins/fullcalendar/fullcalendar.css" rel="stylesheet">
		<link href="../plugins/xcharts/xcharts.min.css" rel="stylesheet">
		<link href="../plugins/select2/select2.css" rel="stylesheet">
		<link href="../plugins/justified-gallery/justifiedGallery.css" rel="stylesheet">
		<link href="../css/style_v2.css" rel="stylesheet">
		<link href="../plugins/chartist/chartist.min.css" rel="stylesheet">
		<link href="../plugins/CLEditor/jquery.cleditor.css" rel="stylesheet">
		<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
		<!--[if lt IE 9]>
				<script src="http://getbootstrap.com/docs-assets/js/html5shiv.js"></script>
				<script src="http://getbootstrap.com/docs-assets/js/respond.min.js"></script>
		<![endif]-->
	</head>
<body>
<% ResourceBundle resourceBundle = ResourceBundle.getBundle("com.francelabs.i18n.text", request.getLocale()); %>
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
				<a href="../index.jsp">Datafari</a>
			</div>
			<div id="top-panel" class="col-xs-12 col-sm-10">
				<div class="row">
					<div class="col-xs-8 col-sm-4">
						<!--  <img src="../images/datafari.png" width=50% height=50% > -->
					</div>
					<div class="col-xs-4 col-sm-8 top-panel-right">
<!-- 						<a href="#" class="about">about</a> -->

					 	<ul class="nav navbar-nav pull-right panel-menu" >
					 	
					 	<li class="searchLink"><a class="searchPageLink" href="/Datafari/Search"><% out.print(resourceBundle.getString("searchPage")); %></a></li>
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
										<span class="welcome"><% out.print(resourceBundle.getString("welcome")); %>,</span>

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
 										<a href="/Datafari/SignOut"> 
 											<i class="fa fa-power-off"></i> 
 											<span><% out.print(resourceBundle.getString("signout")); %></span> 
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
<li class="dropdown" id="User">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-male"></i>
						<span class="hidden-xs"><%= resourceBundle.getString("myaccount")%></span>
					</a>
					<ul class="dropdown-menu">
<!-- 						<li><a class="ajax-link" href="../ajax/maps.html">Save search queries</a></li> -->
						<li id="Alerts"><a class="ajax-link" href="../ajax/Alerts.html"><%= resourceBundle.getString("alerts")%></a></li>
						<li id="LikesAndFavorites"><a class="ajax-link" href="../ajax/getFavorites.html"><%= resourceBundle.getString("Favorites")%></a></li>
<!-- 						<li><a class="ajax-link" href="../ajax/map_leaflet.html">Favorite documents</a></li> -->
					</ul>
				</li>
<%
if(request.isUserInRole("SearchExpert")||request.isUserInRole("SearchAdministrator")){
%>
<!-- 					<a href="../ajax/dashboard.html" class="ajax-link"> <i -->
				<li id="Connectors">
<!-- 							class="fa fa-dashboard"></i> <span class="hidden-xs">Dashboard</span> -->
					</a> <a href="../ajax/manifoldcf.html" class="ajax-link">
						<i class="fa fa-arrows-alt"></i>
						<span class="hidden-xs"><%= resourceBundle.getString("connectors")%></span>
					</a>
				</li>

				<li class="dropdown" id="Statistics">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-bar-chart"></i>
						 <span class="hidden-xs"><%= resourceBundle.getString("statistics")%></span>
					</a>
					<ul class="dropdown-menu">
						<li id="UsageStatistics"><a class="ajax-link" href="../ajax/usageStatistics.html"><%= resourceBundle.getString("useStats")%></a></li>
						<li><a class="ajax-link" href="../ajax/StatsQuery.html"><%= resourceBundle.getString("queryStats")%></a></li> 
<!-- 						<li><a class="ajax-link" href="../ajax/corpusStatistics.html">Corpus statistics</a></li> -->
<!-- 						<li><a class="ajax-link" href="../ajax/systemStatistics.html">System statistics</a></li> -->
					</ul>
				</li>
				<li class="dropdown" id="Admin">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-wrench"></i>
						 <span class="hidden-xs"><%= resourceBundle.getString("admin")%></span>
					</a>
					<ul class="dropdown-menu">
						<li id="SolrAdmin"><a class="ajax-link" href="../ajax/solr.html"><%= resourceBundle.getString("solrAdmin")%></a></li>
						<li id="AlertAdmin"><a class="ajax-link" href="../ajax/alertsAdmin.html"><%= resourceBundle.getString("alertAdmin")%></a></li>
						<li id="IndexField"><a class="ajax-link" href="../ajax/IndexField.html"><%= resourceBundle.getString("indexField")%></a></li>
						<li id="SchemaAnalysis"><a class="ajax-link" href="../ajax/SchemaAnalysis.html"><%= resourceBundle.getString("schemaAnalysis")%></a></li>
<% 
	if(request.isUserInRole("SearchAdministrator")){
%>
						<li id="SizeLimitation"><a class="ajax-link" href="../ajax/SizeLimitations.html"><%= resourceBundle.getString("sizeLimitation")%></a></li>
						<li id="AutocompleteConfiguration"><a class="ajax-link" href="../ajax/AutocompleteConfiguration.html"><%= resourceBundle.getString("autocompleteConfiguration")%></a></li>
<%
	}
%>
					</ul>
				</li>
				<li class="dropdown" id="Conf">
					<a href="#" class="dropdown-toggle">
						<i class="fa fa-desktop"></i>
						 <span class="hidden-xs"><%= resourceBundle.getString("conf")%></span>
					</a>
					<ul class="dropdown-menu">
						<li id="PromoLink"><a class="ajax-link" href="../ajax/promoLinks.html"><%= resourceBundle.getString("promoLink")%></a></li>
						<li id="Synonyms" ><a class="ajax-link" href="../ajax/Synonyms.html"><%= resourceBundle.getString("synonyms")%></a></li>
<%-- 						<li id="Stopwords"><a class="ajax-link" href="../ajax/StopWords.html"><%= resourceBundle.getString("stopwords")%></a></li> --%>
						<li id="FieldWeight"><a class="ajax-link" href="../ajax/FieldWeight.html"><%= resourceBundle.getString("fieldWeight")%></a></li>
						<li id="FacetConfig"><a class="ajax-link" href="../ajax/FacetConfig.html"><%= resourceBundle.getString("facetConfig")%></a></li>
						<li id="Deduplication"><a class="ajax-link" href="../ajax/config_deduplication.html"><%= resourceBundle.getString("deduplication")%></a></li>

						<li id="LikesAndFavorites"><a class="ajax-link" href="../ajax/config_likesAndFavorites.html"><%= resourceBundle.getString("likesAndFavorites")%></a></li>
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
						 <span class="hidden-xs"><%= resourceBundle.getString("servers")%></span>
					</a>
					<ul class="dropdown-menu">
						<li id="Tomcat"><a class="ajax-link" href="../ajax/Tomcat.html">Tomcat</a></li>
<!-- 						<li><a class="ajax-link" href="../ajax/Jetty.html">Jetty</a></li> -->
					</ul>
				</li>
				
				<li class="dropdown" id="userManagement">
					<a href="#" class="dropdown-toggle">
					<i class="fa fa-users"></i>
						 <span class="hidden-xs"><%= resourceBundle.getString("userManagement")%></span>
					</a>
					<ul class="dropdown-menu">
						<li id="ldapConfiguration"><a class="ajax-link" href="../ajax/ldapConfiguration.html"><%= resourceBundle.getString("ldapConfiguration")%></a></li>
						<li id="modifyUser"><a class="ajax-link" href="../ajax/modifyUsers.html"><%=resourceBundle.getString("modifyUsers")%></a></li>
						<li id="addUser"><a class="ajax-link" href="../ajax/addUser.html"><%=resourceBundle.getString("addUser")%></a></li>
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
		<jsp:include page="../footer.jsp"  />
	</div>
</div>
<script> 
</script>
<!--End Container-->
<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
<!--<script src="http://code.jquery.com/jquery.js"></script>-->
<script src="../plugins/jquery/jquery.min.js"></script>
<script src="../plugins/jquery-ui/jquery-ui.min.js"></script>
<script src="../plugins/CLEditor/jquery.cleditor.js"></script>
<script src="../plugins/CLEditor/jquery.cleditor.min.js"></script>

<script src="./../plugins/jquery-ui-timepicker-addon/jquery-ui-timepicker-addon.js"></script>
<!-- Include all compiled plugins (below), or include individual files as needed -->
<script src="../plugins/bootstrap/bootstrap.min.js"></script>
<script src="../plugins/justified-gallery/jquery.justifiedGallery.min.js"></script>
<script src="./../admin/i18nInit.js" ></script>
<script type="text/javascript" src="./../js/AjaxFranceLabs/i18njs.js"></script>
<!-- All functions for this theme + document.ready processing -->
<script src="../js/devoops.js"></script>
</body>
</html>
