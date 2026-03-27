package com.francelabs.datafari.security.token;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class DatafariBearerTokenFilter extends OncePerRequestFilter {
  private static final Logger LOGGER = LogManager.getLogger(DatafariBearerTokenFilter.class);
  private final DatafariTokenService tokenService;

  public DatafariBearerTokenFilter(DatafariTokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    LOGGER.debug("header: {}", header);

    if (header != null && header.toLowerCase().startsWith("bearer ")) {
      String tokenValue = header.substring(7);
      LOGGER.debug("tokenValue: {}", tokenValue);
      Authentication authentication = tokenService.authenticate(tokenValue);
      if (authentication != null) {
        LOGGER.debug("getPrincipal: {} - getAuthorities: {}", authentication.getPrincipal(), authentication.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }
}