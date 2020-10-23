package com.francelabs.datafari.security.token.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.CassandraUsernameToAccessToken;

@Repository
public interface CassandraUsernameToAccessTokenRepository extends CrudRepository<CassandraUsernameToAccessToken, String> {

  Optional<CassandraUsernameToAccessToken> findByApprovalKeyAndAccessToken(String approvalKey, String accessToken);

  Optional<List<CassandraUsernameToAccessToken>> findByApprovalKey(String approvalKey);
}
