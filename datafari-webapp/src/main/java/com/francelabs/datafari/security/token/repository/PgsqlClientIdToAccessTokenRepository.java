package com.francelabs.datafari.security.token.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.PgsqlClientIdToAccessToken;

@Repository
public interface PgsqlClientIdToAccessTokenRepository extends JpaRepository<PgsqlClientIdToAccessToken, String> {

    Optional<PgsqlClientIdToAccessToken> findByClientIdAndAccessToken(String clientId, String accessToken);

    List<PgsqlClientIdToAccessToken> findByClientId(String clientId);

}