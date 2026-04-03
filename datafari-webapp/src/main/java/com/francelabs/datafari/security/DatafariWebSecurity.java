package com.francelabs.datafari.security;

import com.francelabs.datafari.ldap.LdapConfig;
import com.francelabs.datafari.ldap.LdapRealm;
import com.francelabs.datafari.security.auth.DatafariAuthenticationSuccessHandler;
import com.francelabs.datafari.security.auth.DatafariLdapAuthoritiesPopulator;
import com.francelabs.datafari.security.auth.PostgresAuthenticationProvider;
import com.francelabs.datafari.security.token.DatafariBearerTokenFilter;
import com.francelabs.datafari.security.token.service.DatafariTokenService;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Spring Security configuration for Datafari.
 *
 * <p>This class declares the shared security beans used by the application:
 * CORS configuration, password encoder, database authentication provider and
 * LDAP authentication providers.</p>
 *
 * <p>The actual HTTP security rules are defined in dedicated
 * {@link SecurityFilterChain} beans inside the nested {@link StandardSecurity}
 * configuration. Splitting the HTTP security into multiple filter chains makes
 * it possible to apply different security behaviors depending on the targeted
 * endpoint.</p>
 *
 * <p>In particular, the legacy token endpoint {@code /oauth/token} is isolated
 * in its own filter chain so it can stay accessible for token issuance without
 * being impacted by the application's standard web security mechanisms
 * (form login, session handling, bearer token filter, etc.). The rest of the
 * application is handled by the main filter chain.</p>
 *
 * <p>This configuration is used as the base security layer for the standard
 * username/password authentication mode of Datafari.</p>
 */
@Configuration
@EnableWebSecurity
public class DatafariWebSecurity {

  private static final int maxConcurrentSessions =
      Integer.parseInt(DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.MAX_CONCURRENT_SESSIONS));

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(ImmutableList.of("*"));
    configuration.setAllowedMethods(ImmutableList.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
    configuration.setAllowCredentials(true);
    configuration.setAllowedHeaders(ImmutableList.of("Authorization", "Cache-Control", "Content-Type"));
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public PostgresAuthenticationProvider postgresAuthenticationProvider(){
    return new PostgresAuthenticationProvider();
  }

  @Bean
  public DatafariLdapAuthoritiesPopulator datafariLdapAuthoritiesPopulator(){
    return new DatafariLdapAuthoritiesPopulator();
  }

  @Bean
  public List<AuthenticationProvider> ldapAuthenticationProviders(DatafariLdapAuthoritiesPopulator authorities) {
    List<AuthenticationProvider> providers = new ArrayList<>();

    List<LdapRealm> adList = LdapConfig.getActiveDirectoryRealms();
    for (LdapRealm adr : adList) {
      LdapContextSource ctx = new LdapContextSource();
      ctx.setUserDn(adr.getConnectionName());
      ctx.setPassword(adr.getDeobfuscatedConnectionPassword());
      ctx.setUrl(adr.getConnectionURL());
      ctx.afterPropertiesSet(); // Check all previous properties set. If they are compatible, the context is initialized.

      for (String userBase : adr.getUserBases()){
        String filter = "(" + adr.getUserSearchAttribute() + "={0})";
        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(userBase, filter, ctx);
        BindAuthenticator bindAuthenticator = new BindAuthenticator(ctx);
        bindAuthenticator.setUserSearch(userSearch);

        LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(bindAuthenticator, authorities);
        providers.add(ldapProvider);
      }
    }
    return providers;
  }

  /**
   * Standard Spring Security configuration used when no external SSO mechanism
   * is enabled.
   *
   * <p>This configuration is activated only when OIDC, SAML, Keycloak,
   * Kerberos, CAS and custom header authentication are all disabled.</p>
   *
   * <p>It defines two distinct {@link SecurityFilterChain} beans:</p>
   * <ul>
   *   <li>a dedicated chain for {@code /oauth/token}, used to expose the legacy
   *   token issuance endpoint with minimal HTTP security constraints,</li>
   *   <li>the main application chain, used for the web UI and REST endpoints,
   *   including form login, logout, session management and bearer token
   *   authentication.</li>
   * </ul>
   *
   * <p>The use of two ordered filter chains avoids mixing the legacy token
   * issuance flow with the main interactive security flow of the application.</p>
   */
  @Configuration
  @ConditionalOnExpression("${oidc.enabled:false}==false && ${saml.enabled:false}==false && ${keycloak.enabled:false}==false && ${kerberos.enabled:false}==false && ${cas.enabled:false}==false && ${header.enabled:false}==false")
  @Order(Ordered.LOWEST_PRECEDENCE)
  public static class StandardSecurity {
    /**
     * Creates the custom bearer token filter used to authenticate REST calls
     * carrying a previously issued Datafari access token.
     *
     * <p>This filter is inserted into the main security filter chain before
     * {@link UsernamePasswordAuthenticationFilter} so that bearer token based
     * authentication can be resolved early in the chain, before the standard
     * username/password login processing takes place.</p>
     *
     * @param tokenService service responsible for validating access tokens and
     *                     rebuilding the associated {@link org.springframework.security.core.Authentication}
     * @return the custom bearer token filter
     */
    @Bean
    public DatafariBearerTokenFilter datafariBearerTokenFilter(DatafariTokenService tokenService) {
      return new DatafariBearerTokenFilter(tokenService);
    }

    /**
     * Builds the {@link AuthenticationManager} used by the standard security
     * configuration and by the legacy token endpoint.
     *
     * <p>This manager delegates authentication to the configured
     * {@link AuthenticationProvider}s in the declared order:
     * first the PostgreSQL provider, then the LDAP providers.</p>
     *
     * <p>Centralizing the authentication providers in a single
     * {@link ProviderManager} ensures that the same authentication logic is
     * reused both for interactive login and for token issuance through
     * {@code /oauth/token}.</p>
     *
     * @param postgresAuthenticationProvider authentication provider backed by the database
     * @param ldapAuthenticationProviders authentication providers backed by LDAP realms
     * @return the authentication manager used by Datafari standard security
     */
    @Bean
    public AuthenticationManager datafariAuthenticationManager(PostgresAuthenticationProvider postgresAuthenticationProvider,
                                                               List<AuthenticationProvider> ldapAuthenticationProviders) {
      List<AuthenticationProvider> providers = new ArrayList<>();
      providers.add(postgresAuthenticationProvider);
      providers.addAll(ldapAuthenticationProviders);
      return new ProviderManager(providers);
    }

    /**
     * Security filter chain dedicated exclusively to the legacy token endpoint
     * {@code /oauth/token}.
     *
     * <p>This chain is intentionally isolated from the main application security
     * chain in order to keep token issuance simple and predictable.</p>
     *
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
     * <p>This separation prevents the legacy token endpoint from being affected
     * by the application's standard login page, session management, logout
     * handling or custom bearer token authentication filter.</p>
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

    /**
     * Main security filter chain for the Datafari web application and REST API.
     *
     * <p>At runtime, Spring Security evaluates the filter chains in ascending order.
     * The first chain whose matcher accepts the request is the only one applied.
     * In this configuration, requests to {@code /oauth/token} are processed by the
     * dedicated chain ordered first, while all other requests fall through to the
     * main application chain ordered second.</p>
     *
     * <p>This chain is evaluated after {@link #tokenSecurityFilterChain(HttpSecurity)}
     * thanks to {@code @Order(2)}. It therefore handles all requests that are
     * not already matched by the dedicated {@code /oauth/token} chain.</p>
     *
     * <p>This filter chain defines the standard interactive and API-oriented
     * security behavior of Datafari:</p>
     * <ul>
     *   <li>CORS is enabled,</li>
     *   <li>CSRF protection is disabled for {@code /rest/**} endpoints because
     *   these endpoints are meant to be called programmatically, including with
     *   bearer tokens,</li>
     *   <li>session concurrency control is enabled for form-based authentication,</li>
     *   <li>form login is configured for browser users,</li>
     *   <li>logout behavior is customized to invalidate the HTTP session and
     *   delete the {@code JSESSIONID} cookie,</li>
     *   <li>HTTP Basic is enabled with a silent 401 entry point to avoid browser
     *   authentication popups for REST clients,</li>
     *   <li>a custom bearer token filter is inserted before
     *   {@link UsernamePasswordAuthenticationFilter} so that API requests can be
     *   authenticated using tokens previously issued by Datafari.</li>
     * </ul>
     *
     * <p>The chain uses the shared {@link AuthenticationManager} built from the
     * PostgreSQL and LDAP authentication providers. This guarantees consistent
     * authentication behavior across form login and token-based flows.</p>
     *
     * <p>Authorization rules are then applied depending on the requested path:
     * administrative endpoints require elevated roles, some REST endpoints
     * require an authenticated user, and all remaining requests are allowed.</p>
     *
     * <p>Although {@code /oauth/token} is explicitly marked as permitted here,
     * that endpoint is normally processed by the higher-priority
     * {@link #tokenSecurityFilterChain(HttpSecurity)}. Keeping it listed here
     * improves readability and documents that the endpoint must remain publicly
     * reachable.</p>
     *
     * @param http the {@link HttpSecurity} builder for the main application chain
     * @param datafariAuthenticationManager the authentication manager shared by
     *                                      form login and token issuance
     * @param datafariBearerTokenFilter custom filter used to authenticate bearer tokens
     * @return the main security filter chain of the application
     * @throws Exception if the security chain cannot be built
     */
    @Bean
    @Order(2)
    public SecurityFilterChain standardSecurityFilterChain (HttpSecurity http, AuthenticationManager datafariAuthenticationManager,
                                         DatafariBearerTokenFilter datafariBearerTokenFilter) throws Exception {
      http.cors(Customizer.withDefaults());
      http.csrf(csrf -> csrf.ignoringRequestMatchers("/rest/**"));

      http.sessionManagement(session -> session.sessionConcurrency( concurrency -> concurrency.maximumSessions(maxConcurrentSessions) ) );

      http.formLogin(login -> login
          .loginPage("/login")
          .defaultSuccessUrl("/index.jsp", false)
          .successHandler(new DatafariAuthenticationSuccessHandler()));

      http.logout(logout -> logout
          .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
          .logoutSuccessUrl("/index.jsp")
          .invalidateHttpSession(true)
          .clearAuthentication(true)
          .deleteCookies("JSESSIONID"));

      // Silent Basic authentication for REST API
      http.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint( (request, response, exception) -> {
        // If an API client sends an invalid Basic -> 401 without WWW-Authenticate (to avoid browser pop-up)
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      }));
      http.exceptionHandling(exception -> {/* let the "/login" entry point as form login if an exception occurs */});

      http.authenticationManager(datafariAuthenticationManager);

      http.addFilterBefore(datafariBearerTokenFilter, UsernamePasswordAuthenticationFilter.class);

      http.authorizeHttpRequests(requests -> requests
          .requestMatchers("/oauth/token").permitAll()
          .requestMatchers("/admin/**","/SearchExpert/**").hasAnyRole("SearchExpert", "SearchAdministrator")
          .requestMatchers("/SearchAdministrator/**", "/rest/v2.0/files/**", "/rest/v2.0/management/**").hasRole("SearchAdministrator")
          .requestMatchers("/rest/v1.0/auth*").authenticated()
          .anyRequest().permitAll()
      );

      return http.build();
    }

  }
}