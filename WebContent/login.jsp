<%-- Prevent the creation of a session --%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=iso-8859-1"%>
<%@ page import="java.util.ResourceBundle"  %>
<%@ page import="com.francelabs.datafari.utils.*" %>
<html>
<head>
<title>Login Page</title>
<meta HTTP-EQUIV="Pragma" CONTENT="no-cache" />
<meta HTTP-EQUIV="Expires" CONTENT="-1" />
<link rel="stylesheet" type="text/css" href="/Datafari/css/reset.css">
<link rel="stylesheet" type="text/css" href="/Datafari/css/login.css">
<link href="../plugins/bootstrap/bootstrap.css" rel="stylesheet">
<link href="/Datafari/css/style_v2.css" rel="stylesheet">
<style>
input[type="submit"]{
    margin-top: 4px;
    position: absolute;
    bottom: 0px;
    right: 12px;
}
</style>
</head>
<br>
<body style="background-color : #F0F0F0;">
	<%
		HttpSession sess = request.getSession(false);
		String message = "";
		boolean error = false;
		ResourceBundle resourceBundle = ResourceBundle.getBundle("com.francelabs.i18n.text", request.getLocale());

		request.setCharacterEncoding("UTF-8");
		//if request is not the url, redirect to the url of this jsp
	%>
	
	<%
		String j_username = request.getParameter("j_username");
		String j_password = (request.getParameter("j_password"));
		if (j_username != null && j_username.length() != 0
				&& j_password != null && j_password.length() != 0) {
			try {
				request.login(j_username, j_password);
				request.getSession().setAttribute("SSOFailure", true);

			} catch (Exception e) {
				e.printStackTrace();
				message = e.getMessage();
				error = true;
			}
		}

		String mainPage = request.getContextPath();
		String loginPage = mainPage + request.getServletPath();

		if (request.getUserPrincipal() == null) {
	%>

	<form class="box login" method="POST"  action='<%=loginPage%>'
		accept-charset='utf-8'>
		<fieldset class="boxBody">
		<label>
		<% out.print("Datafari login"); %>
		</label>
			<label><% out.print(resourceBundle.getString("login")); %></label> <input type="text" tabindex="1"
				name="j_username" required> <label><% out.print(resourceBundle.getString("password"));%></label> <input
				type="password" tabindex="2" required name="j_password">
		</fieldset>


			<%
				if (error) {
			%>
			<label><span class="invalid">Informations invalides</span></label>
			<%
				} 
			%>
			<div class="col-sm-8"></div>
			<input type="submit" class="btn btn-primary col-sm-3" style="margin-top : 4px;" value="Login" tabindex="4">
	</form>

	<!-- -->
	<%
		} else {
			response.sendRedirect(mainPage+"/admin");
		}
	%>
</body>
</html>
