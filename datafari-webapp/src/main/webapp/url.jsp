<%-- Prevent the creation of a session --%>
<%@page import="java.net.URLDecoder"%>
<%@page import="org.springframework.web.util.UriUtils"%>
<%@page import="java.nio.charset.StandardCharsets"%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=utf-8"%>
<%

%>
<html>
<head>
<title>Redirect</title>
<%
String url = URLDecoder.decode(request.getParameter("url"), StandardCharsets.UTF_8);
String baseUrl = url.substring(0, url.indexOf("/") + 1);
String browser = request.getHeader("user-agent");
// File system path needs to be re-encoded whereas URL path not
if(baseUrl.startsWith("file:")) {
  //Encode path
  int endPathIndex = url.lastIndexOf("/");
	String filename = url.substring(endPathIndex + 1);
  String path = url.substring(0, endPathIndex + 1);
	url = UriUtils.encodePath(path, StandardCharsets.UTF_8.toString()) + UriUtils.encode(filename, StandardCharsets.UTF_8.toString());
}
if(!browser.toLowerCase().contains("chrome") || baseUrl.startsWith("http")) {
  response.sendRedirect(url);
}
%>
</head>
<body>
  <a href="<%=url%>">Link to doc</a>
</body>
</html>
