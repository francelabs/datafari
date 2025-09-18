package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.PgsqlAuthToAccessToken;

@Repository
public interface PgsqlAuthToAccessTokenRepository extends JpaRepository<PgsqlAuthToAccessToken, String> {

    Optional<PgsqlAuthToAccessToken> findByAuthKey(String authKey);

}