<%-- Prevent the creation of a session --%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=ISO-8859-1"%>
<%@page import="java.net.URLEncoder"%>
<%

%>
<html>
<head>
<title>Redirect</title>

<meta http-equiv="refresh" content="0;URL=<%= request.getParameter("url") %>">
</head>
<body>
</body>
</html>
