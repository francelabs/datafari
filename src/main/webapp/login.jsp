<%-- Prevent the creation of a session --%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=iso-8859-1"%>
<html>
<head>
<title>Login Page</title>
<meta HTTP-EQUIV="Pragma" CONTENT="no-cache" />
<meta HTTP-EQUIV="Expires" CONTENT="-1" />
<link rel="stylesheet" type="text/css" href="/Datafari/css/reset.css">
<link rel="stylesheet" type="text/css" href="/Datafari/css/login.css">
</head>
<br>
<body>
	<%
		HttpSession sess = request.getSession(false);
		String message = "";
		boolean error = false;

		request.setCharacterEncoding("UTF-8");
		//if request is not the url, redirect to the url of this jsp
	%>
	<%
		String j_username = request.getParameter("j_username");
		String j_password = request.getParameter("j_password");
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

	<form class="box login" method="POST" action='<%=loginPage%>'
		accept-charset='utf-8'>
		<fieldset class="boxBody">
			<label>Nom d'utilisateur : </label> <input type="text" tabindex="1"
				name="j_username" required> <label>Mot de passe : </label> <input
				type="password" tabindex="2" required name="j_password">
		</fieldset>

		<footer>

			<%
				if (error) {
			%>
			<label><span class="invalid">Informations invalides</span></label>
			<%
				} 
			%>

			<input type="submit" class="btnLogin" value="Login" tabindex="4">
		</footer>
	</form>

	<!-- -->
	<%
		} else {
			response.sendRedirect(mainPage+"/admin");
		}
	%>
</body>
</html>
