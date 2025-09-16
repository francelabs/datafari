package com.francelabs.datafari.security.auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

public class DatafariKeycloakSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final Logger LOGGER = LogManager.getLogger(DatafariKeycloakSuccessHandler.class);

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
    clearAuthenticationAttributes(request);

    final String context = request.getContextPath(); 
    final String targetPath = "/datafariui/home";
    final String lang = request.getParameter("lang");
    String redirect = "/applyLang?urlRedirect=" + URLEncoder.encode(targetPath, StandardCharsets.UTF_8);
    if (lang != null && !lang.isBlank()) {
      redirect = "/applyLang?lang=" + URLEncoder.encode(lang, StandardCharsets.UTF_8)
          + "&urlRedirect=" + URLEncoder.encode(targetPath, StandardCharsets.UTF_8);
    }

    LOGGER.debug("Post-auth (Keycloak) forced redirect to {}", redirect);
    getRedirectStrategy().sendRedirect(request, response, redirect);
  }
}