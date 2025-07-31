package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.PgsqlRefreshToken;

@Repository
public interface PgsqlRefreshTokenRepository extends CrudRepository<PgsqlRefreshToken, String> {

    Optional<PgsqlRefreshToken> findByRefreshTokenId(String refreshTokenId);

}