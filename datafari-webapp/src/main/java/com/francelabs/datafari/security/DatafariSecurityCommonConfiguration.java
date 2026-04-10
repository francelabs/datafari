package com.francelabs.datafari.security;

import com.francelabs.datafari.ldap.LdapConfig;
import com.francelabs.datafari.ldap.LdapRealm;
import com.francelabs.datafari.security.auth.DatafariLdapAuthoritiesPopulator;
import com.francelabs.datafari.security.auth.PostgresAuthenticationProvider;
import com.google.common.collect.ImmutableList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Common Spring Security beans shared by Datafari CE and EE.
 *
 * <p>This configuration contains reusable infrastructure beans that are
 * independent from the selected authentication mechanism:
 * password encoder, PostgreSQL authentication provider, LDAP authorities
 * populator, LDAP authentication providers and CORS configuration.</p>
 *
 * <p>HTTP security rules are intentionally not defined here. They are declared
 * in dedicated security configuration classes depending on the active
 * authentication mode (standard, OIDC, CAS, SAML, Kerberos, custom header,
 * etc.).</p>
 */
@Configuration
public class DatafariSecurityCommonConfiguration {
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

    final List<LdapRealm> adList = LdapConfig.getActiveDirectoryRealms();
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
}
