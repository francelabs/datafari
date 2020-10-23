package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.CassandraAuthToAccessToken;

@Repository
public interface CassandraAuthToAccessTokenRepository extends CrudRepository<CassandraAuthToAccessToken, String> {

  Optional<CassandraAuthToAccessToken> findByAuthKey(String authKey);
}
