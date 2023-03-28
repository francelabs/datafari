package com.francelabs.datafari.utils;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import com.francelabs.datafari.ldap.LdapUsers;

public class AuthenticatedUserName {
  public static String getName(final HttpServletRequest request) {
    return getName(request.getUserPrincipal());
  }

  public static String getName(final Principal principal) {
    // Add AuthenticatedUserName param if user authenticated
    if (principal != null) {
      String authenticatedUserName = principal.getName().replaceAll("[^\\\\]*\\\\", "");
      if (principal instanceof KeycloakAuthenticationToken) {
        final KeycloakAuthenticationToken keycloakToken = (KeycloakAuthenticationToken) principal;
        if (keycloakToken.getDetails() instanceof SimpleKeycloakAccount) {
          final SimpleKeycloakAccount keycloakAccount = (SimpleKeycloakAccount) keycloakToken.getDetails();
          authenticatedUserName = keycloakAccount.getKeycloakSecurityContext().getToken().getPreferredUsername();
        }
      }
      if (principal instanceof OAuth2AuthenticationToken) {
        final OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) principal;
        final String usernameAttr = SpringSecurityConfiguration.getInstance().getProperty("oidc.username.attribute", "username");

        if (oauthToken.getPrincipal().getAttribute(usernameAttr) != null) {
          authenticatedUserName = oauthToken.getPrincipal().getAttribute(usernameAttr);
        }
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