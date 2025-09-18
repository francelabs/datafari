package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.PgsqlRefreshTokenAuthentication;

@Repository
public interface PgsqlRefreshTokenAuthenticationRepository extends JpaRepository<PgsqlRefreshTokenAuthentication, String> {

    Optional<PgsqlRefreshTokenAuthentication> findByRefreshTokenId(String refreshTokenId);

}