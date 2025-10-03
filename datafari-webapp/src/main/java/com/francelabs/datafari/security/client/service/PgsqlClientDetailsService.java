package com.francelabs.datafari.security.client.service;

import com.francelabs.datafari.security.client.model.PgsqlClientDetails;
import com.francelabs.datafari.security.client.repo.PgsqlClientDetailsRepository;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PgsqlClientDetailsService implements ClientDetailsService {

    private final PgsqlClientDetailsRepository repo;

    public PgsqlClientDetailsService(PgsqlClientDetailsRepository repo) {
        this.repo = repo;
    }

    @Override
    public ClientDetails loadClientByClientId(String clientId) {
        Optional<PgsqlClientDetails> clientOpt = repo.findByClientId(clientId);
        if (!clientOpt.isPresent()) {
            throw new IllegalArgumentException("Client not found: " + clientId);
        }
        PgsqlClientDetails client = clientOpt.get();
        BaseClientDetails details = new BaseClientDetails();
        details.setClientId(client.getClientId());
        details.setClientSecret(client.getClientSecret());
        if (client.getResourceIds() != null) details.setResourceIds(client.getResourceIds());
        if (client.getScope() != null) details.setScope(client.getScope());
        if (client.getAuthorizedGrantTypes() != null) details.setAuthorizedGrantTypes(client.getAuthorizedGrantTypes());
        if (client.getRegisteredRedirectUri() != null) details.setRegisteredRedirectUri(client.getRegisteredRedirectUri());
        if (client.getAuthorities() != null)
            details.setAuthorities(org.springframework.security.core.authority.AuthorityUtils.createAuthorityList(client.getAuthorities().toArray(new String[0])));
        details.setAccessTokenValiditySeconds(client.getAccessTokenValiditySeconds());
        details.setRefreshTokenValiditySeconds(client.getRefreshTokenValiditySeconds());
        // details.setAdditionalInformation(...) // add if you have extra fields
        // details.setAutoApproveScopes(client.getAutoApproveScopes()); // if needed
        return details;
    }
}