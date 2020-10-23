package com.francelabs.datafari.utils;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;

import com.francelabs.datafari.ldap.LdapUsers;

public class AuthenticatedUserName {
  public static String getName(final HttpServletRequest request) {
    return getName(request.getUserPrincipal());
  }

  public static String getName(final Principal principal) {
    // Add AuthenticatedUserName param if user authenticated
    if (principal != null) {
      String authenticatedUserName = "";
      if (principal instanceof KeycloakAuthenticationToken) {
        final KeycloakAuthenticationToken keycloakToken = (KeycloakAuthenticationToken) principal;
        if (keycloakToken.getDetails() instanceof SimpleKeycloakAccount) {
          final SimpleKeycloakAccount keycloakAccount = (SimpleKeycloakAccount) keycloakToken.getDetails();
          authenticatedUserName = keycloakAccount.getKeycloakSecurityContext().getToken().getPreferredUsername();
        } else {
          authenticatedUserName = principal.getName().replaceAll("[^\\\\]*\\\\", "");
        }
      } else {
        authenticatedUserName = principal.getName().replaceAll("[^\\\\]*\\\\", "");
      }
      // If the user is the search-aggregator, do not try to add the domain
      if (!authenticatedUserName.toLowerCase().contentEquals("search-aggregator") && !authenticatedUserName.toLowerCase().contentEquals("service-account-search-aggregator")
          && !authenticatedUserName.contains("@")) {
        final String domain = LdapUsers.getInstance().getUserDomain(authenticatedUserName);
        if (domain != null && !domain.isEmpty()) {
          authenticatedUserName += "@" + domain;
        }
      }
      return authenticatedUserName;
    }
    return null;
  }
}