<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.ResourceBundle"  %>
<%@ page import="java.util.Locale" %>
<!DOCTYPE html>

<html lang="en">
	<head>
		<meta charset="utf-8" />
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<title>stopWords Configuration</title>
		<link rel="icon" type="image/png" href="images/bullet.png">
		<link rel="stylesheet" type="text/css" href="../css/main.css" media="screen" />
		<link rel="stylesheet" type="text/css" href="../css/admin.css" media="screen" />
	    <script type="text/javascript" src="../js/jquery-1.8.1.min.js"></script>
		<script type="text/javascript" src="../js/menu.js"></script>
	</head>
	<body class="gecko win">
	<jsp:include page="../header.jsp" />
	<jsp:include page="menu.jsp" />
	

    
<br /> <br />
<div class="adminConfig">
<% ResourceBundle resourceBundle = ResourceBundle.getBundle("com.francelabs.i18n.text", request.getLocale()); %>
	
<h3><%= resourceBundle.getString("stopwordTitle") %></h3>
<div id="stopwordLeft">
<%= resourceBundle.getString("stopwordIntro") %><br/><br/>
<%= resourceBundle.getString("stopwordTechno") %><br/><br/>
<%= resourceBundle.getString("stopwordHelp") %><br/><br/>

<form action="stopWords.jsp" method="post"
                        enctype="multipart/form-data">
<input type="file" name="file" size="30" />
<br />
<input type="submit" value="<%= resourceBundle.getString("stopwordSend") %>" id="submitstopwords" />
<%= resourceBundle.getString("stopwordSelection") %>
</form><br /><br />

<form action="download.jsp">
    <input type="submit" value="<%= resourceBundle.getString("stopwordDownloadButton") %>" id="downloadstopwords" >
    <%= resourceBundle.getString("stopwordDownload") %> <br/>
</form> 

 
</div>

</div>

	<jsp:include page="../footer.jsp" />
</body>
</html>
