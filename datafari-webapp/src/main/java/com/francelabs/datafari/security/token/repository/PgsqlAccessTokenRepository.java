package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.PgsqlAccessToken;

@Repository
public interface PgsqlAccessTokenRepository extends JpaRepository<PgsqlAccessToken, String> {

    Optional<PgsqlAccessToken> findByAccessTokenId(String accessTokenId);

}