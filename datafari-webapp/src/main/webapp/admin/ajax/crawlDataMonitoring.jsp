<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Crawl Data Monitoring</title>
</head>
<body>
  <jsp:include
    page="/resources/templates/feature-not-available.jsp" >
    <jsp:param name="featureDesc" value="The crawl data monitoring feature" />
    <jsp:param name="featureImg" value="/resources/images/missing-features/crawl_data_monitoring_feature.png" />
  </jsp:include>
</body>
</html>