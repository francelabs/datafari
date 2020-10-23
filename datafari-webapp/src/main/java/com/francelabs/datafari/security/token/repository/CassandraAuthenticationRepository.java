package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.CassandraAuthentication;

@Repository
public interface CassandraAuthenticationRepository extends CrudRepository<CassandraAuthentication, String> {

  Optional<CassandraAuthentication> findByAccessTokenId(String accessTokenId);
}
