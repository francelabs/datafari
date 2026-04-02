package com.francelabs.datafari.security.standard;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security filter chain dedicated to the legacy {@code /oauth/token} endpoint
 * when Datafari runs with the standard authentication mode.
 *
 * <p>This chain is intentionally isolated from the main application chain so
 * that token issuance remains unaffected by form login, session-oriented
 * security behavior and custom application filters.</p>
 */
@Configuration
@ConditionalOnExpression("${oidc.enabled:false}==false && ${saml.enabled:false}==false && ${keycloak.enabled:false}==false && ${kerberos.enabled:false}==false && ${cas.enabled:false}==false && ${header.enabled:false}==false")
public class DatafariBearerTokenSecurityConfiguration {

  /**
   * <p>Its scope is restricted through {@link HttpSecurity#securityMatcher(String...)}
   * so that it only applies to requests targeting {@code /oauth/token}. Because
   * of its {@code @Order(1)}, it is evaluated before the main application
   * filter chain. As soon as a request matches {@code /oauth/token}, this
   * chain is selected and the other chains are ignored for that request.</p>
   *
   * <p>The chain enables CORS support and disables CSRF protection for this
   * endpoint. CSRF is not relevant here because the endpoint is designed to be
   * called programmatically by clients posting credentials in
   * {@code application/x-www-form-urlencoded} format, not as a browser-based
   * session-oriented form.</p>
   *
   * <p>All requests matched by this chain are permitted at the Spring Security
   * authorization level. The actual security of the endpoint is therefore not
   * enforced by URL authorization rules here, but by the controller itself,
   * which validates:</p>
   * <ul>
   *   <li>the client credentials sent through the {@code Authorization: Basic ...} header,</li>
   *   <li>the resource owner credentials ({@code username}/{@code password}),</li>
   *   <li>the supported grant type.</li>
   * </ul>
   *
   * @param http the {@link HttpSecurity} builder dedicated to this chain
   * @return the filter chain protecting {@code /oauth/token}
   * @throws Exception if the security chain cannot be built
   */

  @Bean
  @Order(1)
  public SecurityFilterChain tokenSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/oauth/token")
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth/token"))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

    return http.build();
  }
}