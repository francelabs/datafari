package com.francelabs.datafari.security.token.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.francelabs.datafari.security.token.model.PgsqlUsernameToAccessToken;

@Repository
public interface PgsqlUsernameToAccessTokenRepository extends JpaRepository<PgsqlUsernameToAccessToken, String> {

  Optional<PgsqlUsernameToAccessToken> findByApprovalKeyAndAccessToken(String approvalKey, String accessToken);

  Optional<List<PgsqlUsernameToAccessToken>> findByApprovalKey(String approvalKey);
}