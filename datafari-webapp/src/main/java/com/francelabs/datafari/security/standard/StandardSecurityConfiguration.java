package com.francelabs.datafari.security.standard;

import com.francelabs.datafari.security.DatafariHttpSecuritySupport;
import com.francelabs.datafari.security.auth.PostgresAuthenticationProvider;
import com.francelabs.datafari.security.standard.token.DatafariBearerTokenFilter;
import com.francelabs.datafari.security.standard.token.service.DatafariTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard Spring Security configuration used when no external SSO mechanism is enabled.
 *
 * <p>This configuration corresponds to the classic Datafari authentication mode
 * based on local PostgreSQL and LDAP authentication providers, together with
 * form login for browser users and bearer token authentication for API calls.</p>
 */
@Configuration
@ConditionalOnExpression("${oidc.enabled:false}==false && ${saml.enabled:false}==false && ${keycloak.enabled:false}==false && ${kerberos.enabled:false}==false && ${cas.enabled:false}==false && ${header.enabled:false}==false")
public class StandardSecurityConfiguration extends DatafariHttpSecuritySupport {

  /**
   * Creates the custom bearer token filter used to authenticate REST requests
   * carrying a Datafari-issued access token.
   *
   * @param tokenService the service used to validate tokens
   * @return the bearer token filter
   */
  @Bean
  public DatafariBearerTokenFilter datafariBearerTokenFilter(DatafariTokenService tokenService) {
    return new DatafariBearerTokenFilter(tokenService);
  }

  /**
   * Creates the authentication manager used by the standard authentication mode.
   *
   * <p>The configured providers are evaluated in the declared order:
   * PostgreSQL first, then LDAP providers.</p>
   *
   * @param postgresAuthenticationProvider the PostgreSQL authentication provider
   * @param ldapAuthenticationProviders the LDAP authentication providers
   * @return the authentication manager used by standard security
   */
  @Bean
  public AuthenticationManager datafariAuthenticationManager(
      PostgresAuthenticationProvider postgresAuthenticationProvider,
      List<AuthenticationProvider> ldapAuthenticationProviders) {

    final List<AuthenticationProvider> providers = new ArrayList<>();
    providers.add(postgresAuthenticationProvider);
    providers.addAll(ldapAuthenticationProviders);
    return new ProviderManager(providers);
  }

  /**
   * Main security filter chain for the standard Datafari web application mode.
   *
   * <p>This chain applies the shared Datafari HTTP security defaults and adds
   * the mechanism-specific behavior of the standard mode:
   * form login, silent HTTP Basic for REST clients and bearer token processing.</p>
   *
   * @param http the {@link HttpSecurity} builder
   * @param authenticationManager the authentication manager used by the chain
   * @param datafariBearerTokenFilter the custom bearer token filter
   * @return the configured security filter chain
   * @throws Exception if the chain cannot be built
   */
  @Bean
  @Order(2)
  public SecurityFilterChain standardSecurityFilterChain(
      HttpSecurity http,
      AuthenticationManager authenticationManager,
      DatafariBearerTokenFilter datafariBearerTokenFilter) throws Exception {

    http.cors(Customizer.withDefaults());
    http.csrf(DatafariHttpSecuritySupport::applyCsrfSecurity);
    http.sessionManagement(DatafariHttpSecuritySupport::applyStandardSessionManagement);

    http.formLogin(DatafariHttpSecuritySupport::applyLoginConfig);
    http.logout(DatafariHttpSecuritySupport::applyLogoutConfig);

    // Silent Basic authentication for REST API clients
    http.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint((request, response, exception) -> {
        // If an API client sends an invalid Basic -> 401 without WWW-Authenticate (to avoid browser pop-up)
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }));

    // Keep the form login entry point for interactive requests
    http.exceptionHandling(exception -> {
      // no custom override here on purpose
    });

    http.authenticationManager(authenticationManager);
    http.addFilterBefore(datafariBearerTokenFilter, UsernamePasswordAuthenticationFilter.class);

    http.authorizeHttpRequests(DatafariHttpSecuritySupport::applyStandardRequestMatchers);

    return http.build();
  }
}