package com.francelabs.datafari.security.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.francelabs.datafari.utils.DatafariMainConfiguration;

public class DatafariAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
  private final int defaultAuthenticatedSessionTimeout = 300;

  @Override
  public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException, ServletException {
    handle(request, response, authentication);
    clearAuthenticationAttributes(request);
    // Set session timeout
    int sessionTimeout = defaultAuthenticatedSessionTimeout;
    try {
      sessionTimeout = Integer.valueOf(DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SESSION_TIMEOUT_AUTH));
    } catch (final NumberFormatException e) {
      sessionTimeout = defaultAuthenticatedSessionTimeout;
    }
    request.getSession(false).setMaxInactiveInterval(sessionTimeout);
  }

  protected void handle(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException {

    final String targetUrl = determineTargetUrl(request, authentication);

    if (response.isCommitted()) {
      return;
    }

    redirectStrategy.sendRedirect(request, response, targetUrl);
  }

  protected String determineTargetUrl(final HttpServletRequest request, final Authentication authentication) {
    String langParam = null;

    // If the language parameter is defined take it, otherwise use the referrer in the message header
    final String lang = request.getParameter("lang");

    if (lang != null) {

      langParam = "?lang=" + lang;

    }

    final String mainPage = request.getContextPath();
    final String redirect = request.getParameter("redirect");

    if (redirect != null) {
      return redirect;
    } else {

      String urlRedirect = "/applyLang";

      // If the language param was defined in the source URL, append the language
      // selection to the adminUi page URL to be able to display it in the correct language
      if (langParam != null) {
        urlRedirect = urlRedirect + langParam + "&urlRedirect=" + mainPage + "/Search";
      } else {
        urlRedirect = urlRedirect + "?urlRedirect=" + mainPage + "/Search";
      }

      return urlRedirect;
    }
  }

  protected void clearAuthenticationAttributes(final HttpServletRequest request) {
    final HttpSession session = request.getSession(false);
    if (session == null) {
      return;
    }
    session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
  }

  public void setRedirectStrategy(final RedirectStrategy redirectStrategy) {
    this.redirectStrategy = redirectStrategy;
  }

  protected RedirectStrategy getRedirectStrategy() {
    return redirectStrategy;
  }

}
