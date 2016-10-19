<%-- Prevent the creation of a session --%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=utf-8"%>
<%@page import="java.net.URLEncoder"%>
<%

%>
<html>
<head>
<title>Redirect</title>

<meta http-equiv="refresh" content="0;URL='<%= request.getParameter("url") %>'">
</head>
<body>
</body>
</html>
