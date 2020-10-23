<%-- Prevent the creation of a session --%>
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
String url = request.getParameter("url");
String baseUrl = url.substring(0, url.indexOf("/") + 1);
String browser = request.getHeader("user-agent");
if(baseUrl.startsWith("file:")) {
  //Encode path
  int endPathIndex = url.lastIndexOf("/");
	String filename = url.substring(endPathIndex + 1);
  String path = url.substring(0, endPathIndex + 1);
	url = UriUtils.encodePath(path, StandardCharsets.UTF_8.toString()) + UriUtils.encode(filename, StandardCharsets.UTF_8.toString());
} else if (baseUrl.startsWith("http")) {
  int paramIndex = url.indexOf("?");
  String urlPath = "";
  String urlParams = "";
  if(paramIndex != -1) {
    urlPath = url.substring(0, paramIndex);
    urlParams = url.substring(paramIndex + 1);
  } else {
    urlPath = url;
  }
  String encodedUrlPath = UriUtils.encodePath(urlPath, StandardCharsets.UTF_8.toString());
  url = encodedUrlPath;
  if(!urlParams.isEmpty()) {
    String encodedParams = UriUtils.encodeQuery(urlParams, StandardCharsets.UTF_8.toString());
    url += "?" + encodedParams;
  }
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
