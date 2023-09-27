package com.francelabs.datafari.security.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.francelabs.datafari.utils.DatafariMainConfiguration;

public class DatafariAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private RequestCache requestCache = new HttpSessionRequestCache();

  private final int defaultAuthenticatedSessionTimeout = 300;

  @Override
  public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException, ServletException {
    clearAuthenticationAttributes(request);
    // Set session timeout
    int sessionTimeout = defaultAuthenticatedSessionTimeout;
    try {
      sessionTimeout = Integer.valueOf(DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SESSION_TIMEOUT_AUTH));
    } catch (final NumberFormatException e) {
      sessionTimeout = defaultAuthenticatedSessionTimeout;
    }
    request.getSession(false).setMaxInactiveInterval(sessionTimeout);
    handle(request, response, authentication);
  }

  @Override
  protected void handle(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException {

    final String targetUrl = determineTargetUrl(request, response, authentication);

    if (response.isCommitted()) {
      return;
    }

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  @Override
  protected String determineTargetUrl(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) {

    final SavedRequest savedRequest = this.requestCache.getRequest(request, response);

    if (savedRequest != null) {
      return savedRequest.getRedirectUrl();
    } else {
      final String redirect = request.getParameter("redirect");
      if (redirect != null) {
        // Use the DefaultSavedRequest URL
        return redirect;
      } else {
        final String mainPage = request.getContextPath();
        String langParam = null;
        // If the language parameter is defined take it, otherwise use the referrer in the message header
        final String lang = request.getParameter("lang");

        if (lang != null) {

          langParam = "?lang=" + lang;

        }
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
  }

  public void setRequestCache(final RequestCache requestCache) {
    this.requestCache = requestCache;
  }

}
