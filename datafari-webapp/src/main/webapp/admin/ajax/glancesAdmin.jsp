<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Monitoring information</title>
</head>
<body>
  <jsp:include
    page="/resources/templates/feature-not-available.jsp" >
    <jsp:param name="featureDesc" value="The check processes UI" />
    <jsp:param name="featureImg" value="/resources/images/missing-features/monitoring_information.png" />
  </jsp:include>
</body>
</html>