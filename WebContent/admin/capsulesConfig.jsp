<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.ResourceBundle"  %>
<%@ page import="java.util.Locale" %>
<!DOCTYPE html>

<html>
<html lang="en">
	<head>
		<meta charset="utf-8" />
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<title>Capsule Configuration</title>
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

<% ResourceBundle resourceBundle = ResourceBundle.getBundle("com.francelabs.i18n.text", request.getLocale()); %>
	
<h3><%= resourceBundle.getString("capsuleTitre") %></h3>
<p>
<%= resourceBundle.getString("capsuleTexte1") %> <br />
query;title;text 
</p>

<p>
<%= resourceBundle.getString("capsuleTexte2") %> <br />
foo;Great;Hello world <br />
bar;Awesome;Hey <br /> <br />

<%= resourceBundle.getString("capsuleTexte3") %>
</p> <br />
<b><%= resourceBundle.getString("capsuleTexte4") %></b>
<form action="capsules.jsp" method="post"
                        enctype="multipart/form-data">
<input type="file" name="file" size="50" />
<br /> <br />
<input type="submit" value="<%= resourceBundle.getString("capsuleTexte5") %>" />
</form>

<br /> <br /> <br /> 
<%= resourceBundle.getString("capsuleTexte6") %>
<a href="download.jsp">capsules.txt</a>  
 
	<jsp:include page="../footer.jsp" />
</body>
</html>



