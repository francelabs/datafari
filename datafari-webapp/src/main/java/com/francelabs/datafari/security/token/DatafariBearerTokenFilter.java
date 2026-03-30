package com.francelabs.datafari.security.token;

import com.francelabs.datafari.security.token.service.DatafariTokenService;
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

/**
 * Servlet filter responsible for authenticating incoming requests carrying a
 * Datafari bearer access token.
 *
 * <p>This filter inspects the {@code Authorization} HTTP header and looks for
 * a value starting with {@code Bearer }. When such a token is found, it is
 * delegated to {@link com.francelabs.datafari.security.token.service.DatafariTokenService}
 * in order to validate it and rebuild the corresponding
 * {@link org.springframework.security.core.Authentication} object.</p>
 *
 * <p>If the token is valid, the resulting authentication is stored in the
 * current {@link org.springframework.security.core.context.SecurityContext},
 * making the request authenticated for the rest of the Spring Security filter
 * chain and for the targeted controller.</p>
 *
 * <p>If no bearer token is present, or if the token cannot be resolved into a
 * valid authentication, the filter does not block the request and simply lets
 * the chain continue. This behavior allows other authentication mechanisms
 * configured in the application, such as form login or HTTP Basic, to apply
 * normally when relevant.</p>
 *
 * <p>This filter is designed to support the legacy token issuance mechanism
 * reintroduced in Datafari through {@code /oauth/token}. It therefore enables
 * previously issued Datafari access tokens to be reused on protected REST
 * endpoints.</p>
 */
public class DatafariBearerTokenFilter extends OncePerRequestFilter {
  private static final Logger LOGGER = LogManager.getLogger(DatafariBearerTokenFilter.class);
  private final DatafariTokenService tokenService;

  /**
   * Creates the bearer token filter.
   *
   * @param tokenService service used to validate a bearer token and rebuild the
   *                     corresponding authentication
   */
  public DatafariBearerTokenFilter(DatafariTokenService tokenService) {
    this.tokenService = tokenService;
  }

  /**
   * Extracts a bearer token from the {@code Authorization} header, validates it
   * and, if valid, stores the resulting authentication in the current security
   * context.
   *
   * <p>The filter only reacts to headers using the {@code Bearer } scheme.
   * Requests without such a header are left untouched.</p>
   *
   * @param request current HTTP request
   * @param response current HTTP response
   * @param filterChain remaining filter chain
   * @throws ServletException if the request cannot be processed
   * @throws IOException if an input/output error occurs during filtering
   */
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