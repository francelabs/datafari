package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.CassandraRefreshTokenToAccessToken;

@Repository
public interface CassandraRefreshTokenToAccessTokenRepository extends CrudRepository<CassandraRefreshTokenToAccessToken, String> {

  Optional<CassandraRefreshTokenToAccessToken> findByRefreshTokenId(String refreshTokenId);
}
