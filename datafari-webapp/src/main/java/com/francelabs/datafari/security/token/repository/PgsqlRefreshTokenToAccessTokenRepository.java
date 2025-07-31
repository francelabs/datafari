package com.francelabs.datafari.security.token.repository;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.francelabs.datafari.security.token.model.PgsqlRefreshTokenToAccessToken;

@Repository
public interface PgsqlRefreshTokenToAccessTokenRepository extends CrudRepository<PgsqlRefreshTokenToAccessToken, String> {
    Optional<PgsqlRefreshTokenToAccessToken> findByRefreshTokenId(String refreshTokenId);
}