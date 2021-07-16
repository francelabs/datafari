<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Check processes</title>
</head>
<body>
  <jsp:include
    page="/resources/templates/feature-not-available.jsp" >
    <jsp:param name="featureDesc" value="The check processes UI" />
    <jsp:param name="featureImg" value="/resources/images/missing-features/check_processes.png" />
  </jsp:include>
</body>
</html>