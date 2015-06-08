<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="java.util.Locale"%>
<%@page import="java.io.FileReader"%>
<%@page import="java.util.Properties"%>
<%@page import="java.io.BufferedReader"%>
<!DOCTYPE html>

<html lang="en">
<head>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
<title>alerts Configuration</title>
<link rel="icon" type="image/png" href="images/bullet.png">
<link rel="stylesheet" type="text/css" href="../css/main.css"
	media="screen" />
<link rel="stylesheet" type="text/css" href="../css/admin.css"
	media="screen" />
<script type="text/javascript" src="../js/jquery-1.8.1.min.js"></script>
<script type="text/javascript" src="../js/menu.js"></script>
</head>
<body class="gecko win">
	<jsp:include page="../header.jsp" />
	<jsp:include page="menu.jsp" />



	<br />
	<br />
	<div class="adminConfig">
		<% ResourceBundle resourceBundle = ResourceBundle.getBundle("com.francelabs.i18n.text", request.getLocale());
//  			Properties properties = new Properties();
//  			properties.load(getServletContext().getResourceAsStream("/../bin/datafari.properties"));
%>

		<h3><%= resourceBundle.getString("alertTitle") %></h3>
		<div id="alertLeft">
			<%= resourceBundle.getString("alertIntro") %><br />
			<%= resourceBundle.getString("alertConfig") %><br />
			<br />
			<form method=get Action="alertModif.jsp">
				<fieldset>
					<input type=text name="frequency" > <%= resourceBundle.getString("alertFrequency") %>
					 <input type="submit" name="modif" value="modification">
				</fieldset>
			</form>
			<form method=get Action="alertAdd.jsp">
				<fieldset>
					<input type=text name="frequency" > <%= resourceBundle.getString("alertFrequency") %>
					 <input type="submit" name="add" value="add">
				</fieldset>
			</form>
			

			
			<%-- <%= resourceBundle.getString("alertTechno") %><br/><br/> --%>
			<%-- <%= resourceBundle.getString("alertHelp") %><br/><br/> --%>

			<!-- <form action="stopWords.jsp" method="post" -->
			<!--                         enctype="multipart/form-data"> -->
			<!-- <input type="file" name="file" size="30" /> -->
			<!-- <br /> -->
			<%-- <input type="submit" value="<%= resourceBundle.getString("alertSend") %>" id="submitalerts" /> --%>
			<%-- <%= resourceBundle.getString("alertSelection") %> --%>
			<!-- </form><br /><br /> -->

			<!-- <form action="download.jsp"> -->
			<%--     <input type="submit" value="<%= resourceBundle.getString("alertDownloadButton") %>" id="downloadalerts" > --%>
			<%--     <%= resourceBundle.getString("alertDownload") %> <br/> --%>
			<!-- </form>  -->


		</div>

	</div>

	<jsp:include page="../footer.jsp" />
</body>
</html>
