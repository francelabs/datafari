package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.CassandraRefreshToken;

@Repository
public interface CassandraRefreshTokenRepository extends CrudRepository<CassandraRefreshToken, String> {

  Optional<CassandraRefreshToken> findByRefreshTokenId(String refreshTokenId);

}
