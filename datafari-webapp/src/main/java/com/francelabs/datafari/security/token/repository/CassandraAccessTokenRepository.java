package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.CassandraAccessToken;

@Repository
public interface CassandraAccessTokenRepository extends CrudRepository<CassandraAccessToken, String> {

  Optional<CassandraAccessToken> findByAccessTokenId(String accessTokenId);

}
