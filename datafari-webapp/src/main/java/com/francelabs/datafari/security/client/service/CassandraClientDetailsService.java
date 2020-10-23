package com.francelabs.datafari.security.client.service;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

import com.francelabs.datafari.security.client.model.CassandraClientDetails;
import com.francelabs.datafari.security.client.repo.CassandraClientDetailsRepository;

@Service
public class CassandraClientDetailsService implements ClientDetailsService {

  @Autowired
  CassandraClientDetailsRepository clientDetailsRepo;

  @Autowired
  PasswordEncoder passwordEncoder;

  @Override
  public ClientDetails loadClientByClientId(final String clientId) throws ClientRegistrationException {

    final Optional<CassandraClientDetails> oClientDetails = clientDetailsRepo.findByClientId(clientId);
    if (oClientDetails.isPresent()) {
      final CassandraClientDetails clientDetails = oClientDetails.get();
      final String resourceIds = clientDetails.getResourceIds().stream().collect(Collectors.joining(","));
      final String scopes = clientDetails.getScope().stream().collect(Collectors.joining(","));
      final String grantTypes = clientDetails.getAuthorizedGrantTypes().stream().collect(Collectors.joining(","));
      final String authorities = clientDetails.getAuthorities().stream().collect(Collectors.joining(","));
      final BaseClientDetails base = new BaseClientDetails(clientId, resourceIds, scopes, grantTypes, authorities);
      base.setClientSecret(clientDetails.getClientSecret());
      base.setAccessTokenValiditySeconds(clientDetails.getAccessTokenValiditySeconds());
      base.setRefreshTokenValiditySeconds(clientDetails.getRefreshTokenValiditySeconds());
      base.setAutoApproveScopes(clientDetails.getScope());
      return base;
    } else {
      throw new ClientRegistrationException("Client Id does not exist");
    }
  }

}
