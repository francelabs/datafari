package com.francelabs.datafari.security;

import com.francelabs.datafari.ldap.LdapConfig;
import com.francelabs.datafari.ldap.LdapRealm;
import com.francelabs.datafari.security.auth.DatafariAuthenticationSuccessHandler;
import com.francelabs.datafari.security.auth.DatafariLdapAuthoritiesPopulator;
import com.francelabs.datafari.security.auth.PostgresAuthenticationProvider;
import com.francelabs.datafari.security.token.DatafariBearerTokenFilter;
import com.francelabs.datafari.security.token.DatafariTokenService;
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

  @Bean
  public AuthenticationManager datafariAuthenticationManager(PostgresAuthenticationProvider postgresAuthenticationProvider,
                                                     List<AuthenticationProvider> ldapAuthenticationProviders) {
    List<AuthenticationProvider> providers = new ArrayList<>();
    providers.add(postgresAuthenticationProvider);
    providers.addAll(ldapAuthenticationProviders);
    return new ProviderManager(providers);
  }

  @Configuration
  @ConditionalOnExpression("${oidc.enabled:false}==false && ${saml.enabled:false}==false && ${keycloak.enabled:false}==false && ${kerberos.enabled:false}==false && ${cas.enabled:false}==false && ${header.enabled:false}==false")
  @Order(Ordered.LOWEST_PRECEDENCE)
  public static class StandardSecurity {

    @Bean
    public DatafariBearerTokenFilter datafariBearerTokenFilter(DatafariTokenService tokenService) {
      return new DatafariBearerTokenFilter(tokenService);
    }

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