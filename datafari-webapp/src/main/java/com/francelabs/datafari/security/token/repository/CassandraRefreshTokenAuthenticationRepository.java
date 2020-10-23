package com.francelabs.datafari.security.token.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.CassandraRefreshTokenAuthentication;

@Repository
public interface CassandraRefreshTokenAuthenticationRepository extends CrudRepository<CassandraRefreshTokenAuthentication, String> {

  Optional<CassandraRefreshTokenAuthentication> findByRefreshTokenId(String refreshTokenId);
}
