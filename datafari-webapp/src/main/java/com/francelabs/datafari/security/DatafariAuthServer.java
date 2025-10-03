package com.francelabs.datafari.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.TokenStore;

import com.francelabs.datafari.aggregator.utils.SearchAggregatorPasswordManager;
import com.francelabs.datafari.security.auth.DatafariSimpleUserDetailsService;
import com.francelabs.datafari.security.client.model.PgsqlClientDetails;
import com.francelabs.datafari.security.client.repo.PgsqlClientDetailsRepository;
import com.francelabs.datafari.security.client.service.PgsqlClientDetailsService;
import com.francelabs.datafari.security.token.store.PgsqlTokenStore;

@Configuration
@EnableAuthorizationServer
public class DatafariAuthServer extends AuthorizationServerConfigurerAdapter {

  private static final Logger LOGGER = LogManager.getLogger(DatafariAuthServer.class);

  private static final String GRANT_TYPE_PASSWORD = "password";
  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String SCOPE_READ = "read";

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private PgsqlClientDetailsService pgsqlClientDetailsService;

  @Autowired
  private PgsqlClientDetailsRepository pgsqlClientDetailsRepo;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private DatafariSimpleUserDetailsService datafariUserDetailService;

  @Autowired
  private PgsqlTokenStore pgsqlTokenStore;

  // -- Beans for TokenStore and ClientDetailsService (Postgres version) --
  @Bean
  public TokenStore tokenStore() {
    return pgsqlTokenStore;
  }

  @Bean
  public ClientDetailsService clientDetailsService() {
    return pgsqlClientDetailsService;
  }

  @Bean
  public SearchAggregatorPasswordManager saPasswordManager() {
    return new SearchAggregatorPasswordManager();
  }

  @Override
  public void configure(final AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
    endpoints
      .authenticationManager(authenticationManager)
      .tokenStore(tokenStore())
      .userDetailsService(datafariUserDetailService);
  }

  @Override
  public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
    this.initializeClients();
    clients.withClientDetails(pgsqlClientDetailsService);
  }

  private void initializeClients() {
    // Check presence of datafari-client
    final Optional<PgsqlClientDetails> oClientDetails = pgsqlClientDetailsRepo.findByClientId("datafari-client");
    if (!oClientDetails.isPresent()) {
      // Create it
      final Set<String> searchAggScopes = new HashSet<>(Arrays.asList(SCOPE_READ));
      final Set<String> searchAggGrant = new HashSet<>(Arrays.asList(CLIENT_CREDENTIALS, GRANT_TYPE_PASSWORD, REFRESH_TOKEN));
      final Set<String> resourceIds = new HashSet<>();
      final Set<String> redirectUris = new HashSet<>();
      final Set<String> authorities = new HashSet<>();
      PgsqlClientDetails datafariClient = new PgsqlClientDetails(
          "datafari-client",
          passwordEncoder.encode(""),
          resourceIds,
          searchAggScopes,
          searchAggGrant,
          redirectUris,
          authorities,
          60 * 30,
          60 * 60 * 24
      );
      pgsqlClientDetailsRepo.save(datafariClient);
      LOGGER.info("Created oauth2 client 'datafari-client'");
    }
  }
}