package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.PgsqlAuthentication;

@Repository
public interface PgsqlAuthenticationRepository extends JpaRepository<PgsqlAuthentication, String> {

    Optional<PgsqlAuthentication> findByAccessTokenId(String accessTokenId);
}