<%-- Prevent the creation of a session --%>
<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="java.net.URLEncoder"%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=utf-8"%>
<%

%>
<html>
<head>
<title>Redirect</title>
<%
String url = request.getParameter("url");
String baseUrl = url.substring(0, url.indexOf("/") + 1);
String browser = request.getHeader("user-agent");
if(baseUrl.startsWith("file:")) {
  //Encode filename
  String filename = url.substring(url.lastIndexOf("/") + 1);
  filename = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
  //Encode baseUrl
  baseUrl = url.substring(url.indexOf("/"), url.lastIndexOf("/") + 1);
  baseUrl = URLEncoder.encode(baseUrl, StandardCharsets.UTF_8.name()).replaceAll("%2F", "/").replaceAll("\\+", "%20");
  url = "file:" + baseUrl + filename;
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
