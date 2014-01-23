<%-- Prevent the creation of a session --%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=iso-8859-1"%>
<html>
<head>
<title>Redirect</title>
<meta HTTP-EQUIV="Pragma" CONTENT="no-cache" />
<meta HTTP-EQUIV="Expires" CONTENT="-1" />
<meta HTTP-EQUIV="refresh" CONTENT="0;URL='<%= request.getParameter("url") %>'">
</head>
<body>
</body>
</html>
