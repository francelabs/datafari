<%@page import="com.francelabs.datafari.utils.AuthenticatedUserName"%>
<%@page import="org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount"%>
<%@page import="org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken"%>
<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<header>
  <nav id="admin-navbar" class="navbar navbar-expand navbar-dark bg-dark py-0">
    <div class="navbar-brand" id="datafariAdminMenu"><span id="admin-menu-icon" class="side-menu-control"><i class="fas fa-angle-left"></i></span> Admin Menu</div>
    <ul class="navbar-nav ml-auto" >
      <li class="nav-item">
        <a id="datafariSearchUiLink" class="searchPageLink nav-link"></a>
      </li>
      <li class="nav-item dropdown" >
        <a id="dropdown-user" href="#" class="nav-link dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
          <span class="avatar">
            <img src="<c:url value="/resources/images/pic.jpg" />" class="rounded-circle" alt="avatar" />
          </span> 
          <span class="user-mini">
            <span id="welcomeAdminUiMsg" class="welcome"></span>
            <span><% 
            String name = AuthenticatedUserName.getName(request);
            out.print(name);
            %></span>
          </span>
        </a>
        <div class="dropdown-menu w-100">
          <a onclick="logout();" class ="dropdown-item px-0" aria-labelledby="dropdown-user" style="cursor: pointer;">
            <i class="fas fa-power-off"></i>
            <span id="logout-AdminUI"></span>
          </a>
        </div>
      </li>
    </ul>
  </nav>
</header>
