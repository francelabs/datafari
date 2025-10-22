package com.francelabs.datafari.utils;

import com.francelabs.datafari.ldap.LdapUsers;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.Objects;

public class AuthenticatedUserName {
  public static String getName(final HttpServletRequest request) {
    return getName(request.getUserPrincipal());
  }

  public static String getName(final Principal principal) {
    // Add AuthenticatedUserName param if user authenticated
    if (principal != null) {
      String authenticatedUserName = principal.getName();
      if (principal instanceof OAuth2AuthenticationToken oauthToken) {
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        if (oAuth2User instanceof OidcUser oidcUser){
          authenticatedUserName = firstNonBlank(
              oidcUser.getPreferredUsername(),
              oidcUser.getEmail(),
              oidcUser.getClaimAsString("upn"),
              oidcUser.getName()
          );
        } else {
          String usernameAttr = SpringSecurityConfiguration.getInstance().getProperty("oidc.username.attribute", "username");
          authenticatedUserName = firstNonBlank(
              getAttribute(oAuth2User, usernameAttr),
              getAttribute(oAuth2User, "preferred_username"),
              getAttribute(oAuth2User, "upn"),
              getAttribute(oAuth2User, "email"),
              oAuth2User.getName()
          );
        }

      } else if (principal instanceof Authentication auth && auth instanceof JwtAuthenticationToken jwtAuth) {
        var jwtToken = jwtAuth.getToken();
        authenticatedUserName = firstNonBlank(
            jwtToken.getClaimAsString("preferred_username"),
            jwtToken.getClaimAsString("upn"),
            jwtToken.getClaimAsString("email"),
            jwtToken.getSubject(),
            auth.getName()
        );

      } else {
        authenticatedUserName = authenticatedUserName.replaceAll("[^\\\\]*\\\\", "");
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

  private static String getAttribute(OAuth2User u, String name) {
    Object v = u.getAttribute(name);
    return v != null ? String.valueOf(v) : null;
  }

  @SafeVarargs
  private static <T> T firstNonBlank(T... values) {
    for (T v : values) {
      if (v != null && !Objects.toString(v, "").isBlank()) return v;
    }
    return null;
  }

}