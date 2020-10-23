package com.francelabs.datafari.security.token.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.CassandraClientIdToAccessToken;

@Repository
public interface CassandraClientIdToAccessTokenRepository extends CrudRepository<CassandraClientIdToAccessToken, String> {

  Optional<CassandraClientIdToAccessToken> findByClientIdAndAccessToken(String clientId, String accessToken);

  Optional<List<CassandraClientIdToAccessToken>> findByClientId(String clientId);

}
