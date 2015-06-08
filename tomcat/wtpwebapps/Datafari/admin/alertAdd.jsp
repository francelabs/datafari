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
<title>stopWords Configuration</title>
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
	<div class="alertConfig">
		<% ResourceBundle resourceBundle = ResourceBundle.getBundle("com.francelabs.i18n.text", request.getLocale());%>

		<h3><%= resourceBundle.getString("alertTitle") %></h3>
		<div id="alertLeft">
			<%= resourceBundle.getString("alertIntro") %><br />
			<%= resourceBundle.getString("alertAdd") %><br />
			<br />

			<%String frequency = request.getParameter("frequency").toString();
			String filePath = "/home/youp/git/datafari-master/bin/common/alerts"+frequency+".txt";
				%>
			<form method=post Action="alerts.jsp">
				<fieldset>
					<%= resourceBundle.getString("alertFile") %><input type="text" name="file" value=<%out.println(filePath); %> ><br/>
					<%= resourceBundle.getString("alertCore") %><input type="text" name="core" ><br/>
					<%= resourceBundle.getString("alertKeyword") %><input type="text" name="keyword" ><br/>
					<%= resourceBundle.getString("alertMail") %><input type="text" name="mail" ><br/>
					<%= resourceBundle.getString("alertSubject") %><input type="text" name="object" ><br/>
					<br> <input type="submit" value="Confirm" />
				</fieldset>
			</form>
			


		</div>

	</div>

	<jsp:include page="../footer.jsp" />
</body>
</html>
