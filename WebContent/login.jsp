<%-- Prevent the creation of a session --%>
<%@ page session="false"%>
<%@ page contentType="text/html;charset=iso-8859-1"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.francelabs.datafari.utils.*"%>
<html>
<head>
<title>Login Page</title>
<meta HTTP-EQUIV="Pragma" CONTENT="no-cache" />
<meta HTTP-EQUIV="Expires" CONTENT="-1" />
<link rel="stylesheet" type="text/css" href="/Datafari/css/reset.css">
<link rel="stylesheet" type="text/css" href="/Datafari/css/login.css">
<link href="/Datafari/plugins/bootstrap/bootstrap.css" rel="stylesheet">
<link href="/Datafari/css/style_v2.css" rel="stylesheet">
<link rel="stylesheet" type="text/css"
	href="/Datafari/css/widgets/login-datafari-form-widget.css">

<script type="text/javascript" src="/Datafari/js/jquery-1.8.1.min.js"></script>
<script type="text/javascript" src="/Datafari/js/function/empty.func.js"></script>
<script type="text/javascript" src="/Datafari/js/jquery-ui-1.8.23.min.js"></script>

<script type="text/javascript" src="/Datafari/js/AjaxFranceLabs/core/Core.js"></script>
<script type="text/javascript"
	src="/Datafari/js/AjaxFranceLabs/core/AbstractModule.js"></script>
<script type="text/javascript"
	src="/Datafari/js/AjaxFranceLabs/core/AbstractWidget.js"></script>
<script type="text/javascript" src="/Datafari/js/AjaxFranceLabs/core/Parameter.js"></script>
<script type="text/javascript"
	src="/Datafari/js/AjaxFranceLabs/core/ParameterStore.js"></script>
<script type="text/javascript" src="/Datafari/js/AjaxFranceLabs/i18njs.js"></script>
<script type="text/javascript" src="/Datafari/js/AjaxFranceLabs/uuid.core.js"></script>
<script type="text/javascript"
	src="/Datafari/js/AjaxFranceLabs/core/AbstractManager.js"></script>
<script type="text/javascript"
	src="/Datafari/js/AjaxFranceLabs/manager/Manager.js"></script>

<script type="text/javascript" src="/Datafari/js/main.js"></script>
<!-- JS library useful to extract parameters value from URL  -->
<script type ="text/javascript" src ="/Datafari/js/url.min.js"></script>

<script type="text/javascript"
	src="/Datafari/js/AjaxFranceLabs/widgets/LoginDatafariForm.widget.js"></script>
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
		String j_password = (request.getParameter("j_password"));
		if (j_username != null && j_username.length() != 0 && j_password != null && j_password.length() != 0) {
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

	<form id="loginDatafariForm" class="box login" method="POST"
		action='<%=loginPage%>' accept-charset='utf-8'>
		<fieldset class="boxBody">
			<label id="loginFormAdminUiLabel"></label> 
			<label id="loginAdminUiLabel"></label> 
			<input type="text" tabindex="1" name="j_username" required>

			<label id="passwordAdminUiLabel"></label>
			<input type="password" tabindex="2" required name="j_password">
		</fieldset>


		<%
			if (error) {
		%>
		<span class="invalid"><label id="invalidLoginAdminUiLabel"></label></span>
		<%
			}
		%>
		<div class="col-sm-8"></div>
		<input type="submit" id="loginAdminUiBtn"
			class="btn btn-primary col-sm-3" value="Login" tabindex="4">
	</form>

	<!-- -->
	<%
		} else {
			response.sendRedirect(mainPage + "/admin");
		}
	%>
</body>
</html>
