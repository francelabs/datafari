<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html >
<html>
<head>
<title>User Login</title>
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/reset.css" />">
<link rel="stylesheet" type="text/css" href="<c:url value="/resources/css/login.css" />">
</head>
<br>
<body>
	<form class="box login" method="POST"
		action='<%=response.encodeURL("j_security_check")%>'>
		<fieldset class="boxBody">
			<label>Nom d'utilisateur : </label> <input type="text" tabindex="1" name="j_username" required> <label>Mot de passe : </label>
			<input type="password" tabindex="2" required name="j_password">
		</fieldset>
		<footer>
			<label><span class="invalid">Informations invalides</span></label><input type="submit" class="btnLogin" value="Login"
				tabindex="4">
		</footer>
	</form>
</body>
</html>