package com.francelabs.datafari.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;

import com.francelabs.datafari.aggregator.utils.SearchAggregatorPasswordManager;
import com.francelabs.datafari.security.auth.DatafariSimpleUserDetailsService;
import com.francelabs.datafari.security.client.model.CassandraClientDetails;
import com.francelabs.datafari.security.client.repo.CassandraClientDetailsRepository;
import com.francelabs.datafari.security.client.service.CassandraClientDetailsService;
import com.francelabs.datafari.security.token.store.CassandraTokenStore;

@Configuration
@EnableAuthorizationServer
@EnableCassandraRepositories
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false", matchIfMissing = false)
public class DatafariAuthServer extends AuthorizationServerConfigurerAdapter {

  private static Logger LOGGER = LogManager.getLogger(DatafariAuthServer.class.getName());

  private static final String GRANT_TYPE_PASSWORD = "password";
  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String SCOPE_READ = "read";

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private CassandraClientDetailsService cassandraClientDetailsService;

  @Autowired
  CassandraClientDetailsRepository clientDetailsRepo;

  @Autowired
  PasswordEncoder passwordEncoder;

  @Autowired
  private DatafariSimpleUserDetailsService datafariUserDetailService;

  @Bean
  public CassandraTokenStore tokenStore() {
    return new CassandraTokenStore();
  }

  @Bean
  public SearchAggregatorPasswordManager saPasswordManager() {
    return new SearchAggregatorPasswordManager();
  }

  @Override
  public void configure(final AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
    endpoints.authenticationManager(authenticationManager).tokenStore(tokenStore()).userDetailsService(datafariUserDetailService);
  }

  @Override
  public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
    this.initializeClients();
    clients.withClientDetails(cassandraClientDetailsService);
  }

  private void initializeClients() {

    // Check presence of datafari-client
    final Optional<CassandraClientDetails> oClientDetails = clientDetailsRepo.findByClientId("datafari-client");
    if (!oClientDetails.isPresent()) {
      // Create it
      final Set<String> searchAggScopes = new HashSet<>(Arrays.asList(SCOPE_READ));
      final Set<String> searchAggGrant = new HashSet<>(Arrays.asList(CLIENT_CREDENTIALS, GRANT_TYPE_PASSWORD, REFRESH_TOKEN));
      final CassandraClientDetails datafariClient = new CassandraClientDetails("datafari-client", passwordEncoder.encode(""), new HashSet<String>(), searchAggScopes, searchAggGrant, new HashSet<String>(), new HashSet<String>(), 60 * 30,
          60 * 60 * 24);
      clientDetailsRepo.save(datafariClient);
      LOGGER.info("Created oauth2 client 'datafari-client'");
    }
  }

}
