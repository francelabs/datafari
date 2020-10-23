<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>MCF Backup/Restore</title>
</head>
<body>
  <jsp:include
    page="/resources/templates/feature-not-available.jsp" >
    <jsp:param name="featureDesc" value="The MCF backup/restore feature" />
    <jsp:param name="featureImg" value="/resources/images/missing-features/backup_mcf_feature.png" />
  </jsp:include>
</body>
</html>