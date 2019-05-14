<%-- Prevent the creation of a session --%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=utf-8"%>
<%

%>
<html>
<head>
<title>Redirect</title>

<meta http-equiv="refresh" content="0;URL='<%= request.getParameter("url").replace("'", "%27") %>'">
</head>
<body>
</body>
</html>
