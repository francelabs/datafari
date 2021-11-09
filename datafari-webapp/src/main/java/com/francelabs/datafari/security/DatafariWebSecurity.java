package com.francelabs.datafari.security;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.datastax.oss.driver.api.core.CqlSession;
import com.francelabs.datafari.ldap.LdapConfig;
import com.francelabs.datafari.ldap.LdapRealm;
import com.francelabs.datafari.security.auth.CassandraAuthenticationProvider;
import com.francelabs.datafari.security.auth.DatafariAuthenticationSuccessHandler;
import com.francelabs.datafari.security.auth.DatafariLdapAuthoritiesPopulator;
import com.francelabs.datafari.service.db.CassandraManager;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import com.google.common.collect.ImmutableList;

@EnableWebSecurity
public class DatafariWebSecurity {

  private static final Logger LOGGER = LogManager.getLogger(DatafariWebSecurity.class.getName());

  private static final int maxConcurrentSessions = Integer.parseInt(DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.MAX_CONCURRENT_SESSIONS));

  // Tell spring witch Cassandra session to use
  public @Bean CqlSession session() {
    return CassandraManager.getInstance().getSession();
  }

  @Bean
  CassandraOperations cassandraTemplate() {
    return new CassandraTemplate(session());
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(ImmutableList.of("*"));
    configuration.setAllowedMethods(ImmutableList.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
    // setAllowCredentials(true) is important, otherwise:
    // The value of the 'Access-Control-Allow-Origin' header in the response must not be the wildcard '*' when the request's credentials mode is
    // 'include'.
    configuration.setAllowCredentials(true);
    // setAllowedHeaders is important! Without it, OPTIONS preflight request
    // will fail with 403 Invalid CORS request
    configuration.setAllowedHeaders(ImmutableList.of("Authorization", "Cache-Control", "Content-Type"));
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Configuration
  @ConditionalOnExpression("${saml.enabled:false}==false && ${keycloak.enabled:false}==false && ${kerberos.enabled:false}==false")
  @Order(Ordered.LOWEST_PRECEDENCE)
  public static class StandardSecurity extends WebSecurityConfigurerAdapter {

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
      return super.authenticationManagerBean();
    }

    @Override
    public void configure(final HttpSecurity http) throws Exception {
      final RequestMatcher loginRM = new AntPathRequestMatcher("/login", "POST");
      final RequestMatcher logoutRM = new AntPathRequestMatcher("/logout", "POST");
      final RequestMatcher adminRM = new AntPathRequestMatcher("/admin/**", "POST");
      final RequestMatcher searchAdminRM = new AntPathRequestMatcher("/SearchAdministrator/**", "POST");
      final RequestMatcher searchExpertRM = new AntPathRequestMatcher("/SearchExpert/**", "POST");
      final RequestMatcher csrfRM = new OrRequestMatcher(loginRM, logoutRM, adminRM, searchAdminRM, searchExpertRM);
      http.csrf().requireCsrfProtectionMatcher(csrfRM);
      http.cors();
      http.sessionManagement().sessionFixation().migrateSession().maximumSessions(maxConcurrentSessions);
      http.formLogin().loginPage("/login").defaultSuccessUrl("/index.jsp", false).successHandler(new DatafariAuthenticationSuccessHandler());
      http.logout().logoutSuccessUrl("/index.jsp").invalidateHttpSession(true);
      http.authorizeRequests().antMatchers("/*").permitAll().antMatchers("/admin/*").hasAnyRole("SearchExpert", "SearchAdministrator");
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
      auth.authenticationProvider(new CassandraAuthenticationProvider());
      final List<LdapRealm> adList = LdapConfig.getActiveDirectoryRealms();
      for (final LdapRealm adr : adList) {
        for (final String userBase : adr.getUserBases()) {
          auth.ldapAuthentication().ldapAuthoritiesPopulator(new DatafariLdapAuthoritiesPopulator()).userSearchBase(userBase).userSearchFilter("(" + adr.getUserSearchAttribute() + "={0})")
              .contextSource().managerDn(adr.getConnectionName()).managerPassword(adr.getDeobfuscatedConnectionPassword()).url(adr.getConnectionURL());
        }
      }
    }
  }

}
