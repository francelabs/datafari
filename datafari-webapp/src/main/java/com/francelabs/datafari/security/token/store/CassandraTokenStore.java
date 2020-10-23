package com.francelabs.datafari.security.token.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.InsertOptions.InsertOptionsBuilder;
import org.springframework.data.cassandra.core.cql.WriteOptions;
import org.springframework.data.cassandra.core.cql.WriteOptions.WriteOptionsBuilder;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;

import com.francelabs.datafari.security.token.model.CassandraAccessToken;
import com.francelabs.datafari.security.token.model.CassandraAuthToAccessToken;
import com.francelabs.datafari.security.token.model.CassandraAuthentication;
import com.francelabs.datafari.security.token.model.CassandraClientIdToAccessToken;
import com.francelabs.datafari.security.token.model.CassandraRefreshToken;
import com.francelabs.datafari.security.token.model.CassandraRefreshTokenAuthentication;
import com.francelabs.datafari.security.token.model.CassandraRefreshTokenToAccessToken;
import com.francelabs.datafari.security.token.model.CassandraUsernameToAccessToken;
import com.francelabs.datafari.security.token.repository.CassandraAccessTokenRepository;
import com.francelabs.datafari.security.token.repository.CassandraAuthToAccessTokenRepository;
import com.francelabs.datafari.security.token.repository.CassandraAuthenticationRepository;
import com.francelabs.datafari.security.token.repository.CassandraClientIdToAccessTokenRepository;
import com.francelabs.datafari.security.token.repository.CassandraRefreshTokenAuthenticationRepository;
import com.francelabs.datafari.security.token.repository.CassandraRefreshTokenRepository;
import com.francelabs.datafari.security.token.repository.CassandraRefreshTokenToAccessTokenRepository;
import com.francelabs.datafari.security.token.repository.CassandraUsernameToAccessTokenRepository;
import com.francelabs.datafari.security.token.utils.AccessTokenSerialization;
import com.francelabs.datafari.security.token.utils.AuthSerialization;
import com.francelabs.datafari.security.token.utils.RefreshTokenSerialization;
import com.francelabs.datafari.security.token.utils.UsernameUtils;

public class CassandraTokenStore implements TokenStore {

  @Autowired
  private CassandraOperations cassandraOperations;

  @Autowired
  private CassandraAccessTokenRepository accessTokenRepository;

  @Autowired
  private CassandraRefreshTokenRepository refreshTokenRepository;

  @Autowired
  private CassandraAuthenticationRepository authRepository;

  @Autowired
  private CassandraAuthToAccessTokenRepository authToAccessTokenRepository;

  @Autowired
  private CassandraRefreshTokenAuthenticationRepository refreshTokenAuthRepository;

  @Autowired
  private CassandraRefreshTokenToAccessTokenRepository refreshTokenToAccessTokenRepository;

  @Autowired
  private CassandraClientIdToAccessTokenRepository clientIdToAccessTokenRepository;

  @Autowired
  private CassandraUsernameToAccessTokenRepository usernameToAccessTokenRepository;

  private final AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

  @Override
  public OAuth2Authentication readAuthentication(final OAuth2AccessToken token) {
    return readAuthentication(token.getValue());
  }

  @Override
  public OAuth2Authentication readAuthentication(final String token) {
    final Optional<CassandraAuthentication> authentication = authRepository.findByAccessTokenId(token);
    if (authentication.isPresent()) {
      return AuthSerialization.deserialize(authentication.get().getAuthentication());
    } else {
      return null;
    }
  }

  @Override
  public void storeAccessToken(final OAuth2AccessToken token, final OAuth2Authentication authentication) {
    final List<Object> entitiesList = new ArrayList<Object>();
    final WriteOptionsBuilder accessWriteOptionsBuilder = WriteOptions.builder();
    if (token.getExpiration() != null) {
      final int seconds = token.getExpiresIn();
      accessWriteOptionsBuilder.ttl(seconds);
    }
    final CassandraAccessToken cAccessToken = new CassandraAccessToken(token.getValue(), AccessTokenSerialization.serialize(token));
    entitiesList.add(cAccessToken);
    final CassandraAuthentication cAuth = new CassandraAuthentication(token.getValue(), AuthSerialization.serialize(authentication));
    entitiesList.add(cAuth);
    final CassandraAuthToAccessToken cAuthToAccessToken = new CassandraAuthToAccessToken(authenticationKeyGenerator.extractKey(authentication), AccessTokenSerialization.serialize(token));
    entitiesList.add(cAuthToAccessToken);

    final OAuth2RefreshToken oAuth2RefreshToken = token.getRefreshToken();
    if (oAuth2RefreshToken != null && oAuth2RefreshToken.getValue() != null) {
      final InsertOptionsBuilder refreshInsertOptionsBuilder = InsertOptions.builder();
      if (oAuth2RefreshToken instanceof ExpiringOAuth2RefreshToken) {
        final ExpiringOAuth2RefreshToken expiringRefreshToken = (ExpiringOAuth2RefreshToken) oAuth2RefreshToken;
        final Date expiration = expiringRefreshToken.getExpiration();
        if (expiration != null) {
          final int seconds = Long.valueOf((expiration.getTime() - System.currentTimeMillis()) / 1000L).intValue();
          refreshInsertOptionsBuilder.ttl(seconds);
        }
      }
      final CassandraRefreshTokenToAccessToken cRefreshTokenToAccessToken = new CassandraRefreshTokenToAccessToken(token.getValue(), token.getRefreshToken().getValue());
      // Insert the refresh token apart from batch as it has its own ttl
      cassandraOperations.insert(cRefreshTokenToAccessToken, refreshInsertOptionsBuilder.build());
    }

    final CassandraClientIdToAccessToken cClientIdToAccessToken = new CassandraClientIdToAccessToken(authentication.getOAuth2Request().getClientId(), AccessTokenSerialization.serialize(token));
    entitiesList.add(cClientIdToAccessToken);

    final CassandraUsernameToAccessToken cUsernameToAccessToken = new CassandraUsernameToAccessToken(UsernameUtils.getApprovalKey(authentication), AccessTokenSerialization.serialize(token));
    entitiesList.add(cUsernameToAccessToken);

    final CassandraBatchOperations batch = cassandraOperations.batchOps();
    batch.insert(entitiesList, accessWriteOptionsBuilder.build());
    batch.execute();
  }

  @Override
  public OAuth2AccessToken readAccessToken(final String tokenValue) {
    final Optional<CassandraAccessToken> accessToken = accessTokenRepository.findByAccessTokenId(tokenValue);
    if (accessToken.isPresent()) {
      return AccessTokenSerialization.deserialize(accessToken.get().getAccessToken());
    }
    return null;
  }

  @Override
  public void removeAccessToken(final OAuth2AccessToken token) {
    final String tokenId = token.getValue();
    final Optional<CassandraAccessToken> accessToken = accessTokenRepository.findByAccessTokenId(tokenId);
    if (accessToken.isPresent()) {
      final List<Object> entitiesList = new ArrayList<Object>();
      entitiesList.add(accessToken);

      final Optional<CassandraAuthentication> authentication = authRepository.findByAccessTokenId(tokenId);
      if (authentication.isPresent()) {
        entitiesList.add(authentication.get());
        final OAuth2Authentication oauth = AuthSerialization.deserialize(authentication.get().getAuthentication());
        final String authKey = authenticationKeyGenerator.extractKey(oauth);
        final Optional<CassandraAuthToAccessToken> authToAccessToken = authToAccessTokenRepository.findByAuthKey(authKey);
        if (authToAccessToken.isPresent()) {
          entitiesList.add(authToAccessToken.get());
        }
        final String usernameKey = UsernameUtils.getApprovalKey(oauth);
        final Optional<CassandraUsernameToAccessToken> usernameToAccessToken = usernameToAccessTokenRepository.findByApprovalKeyAndAccessToken(usernameKey, accessToken.get().getAccessTokenStr());
        if (usernameToAccessToken.isPresent()) {
          entitiesList.add(usernameToAccessToken.get());
        }

        final String clientId = oauth.getOAuth2Request().getClientId();
        final Optional<CassandraClientIdToAccessToken> clientIdToAccessToken = clientIdToAccessTokenRepository.findByClientIdAndAccessToken(clientId, accessToken.get().getAccessTokenStr());
        if (clientIdToAccessToken.isPresent()) {
          entitiesList.add(clientIdToAccessToken);
        }
      }

      // Batch delete
      final CassandraBatchOperations batch = cassandraOperations.batchOps();
      batch.delete(entitiesList);
      batch.execute();
    }

  }

  @Override
  public void storeRefreshToken(final OAuth2RefreshToken refreshToken, final OAuth2Authentication authentication) {
    final List<Object> entitiesList = new ArrayList<Object>();
    final WriteOptionsBuilder refreshWriteOptionsBuilder = WriteOptions.builder();
    if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
      final ExpiringOAuth2RefreshToken expiringRefreshToken = (ExpiringOAuth2RefreshToken) refreshToken;
      final Date expiration = expiringRefreshToken.getExpiration();
      if (expiration != null) {
        final int seconds = Long.valueOf((expiration.getTime() - System.currentTimeMillis()) / 1000L).intValue();
        refreshWriteOptionsBuilder.ttl(seconds);
      }
    }
    final CassandraRefreshToken cRefreshToken = new CassandraRefreshToken(refreshToken.getValue(), RefreshTokenSerialization.serialize(refreshToken));
    entitiesList.add(cRefreshToken);
    final CassandraRefreshTokenAuthentication cRefreshTokenAuth = new CassandraRefreshTokenAuthentication(refreshToken.getValue(), AuthSerialization.serialize(authentication));
    entitiesList.add(cRefreshTokenAuth);
    final CassandraBatchOperations batch = cassandraOperations.batchOps();
    batch.insert(entitiesList, refreshWriteOptionsBuilder.build());
    batch.execute();
  }

  @Override
  public OAuth2RefreshToken readRefreshToken(final String tokenValue) {
    final Optional<CassandraRefreshToken> refreshToken = refreshTokenRepository.findByRefreshTokenId(tokenValue);
    if (refreshToken.isPresent()) {
      return RefreshTokenSerialization.deserialize(refreshToken.get().getRefreshToken());
    }
    return null;
  }

  @Override
  public OAuth2Authentication readAuthenticationForRefreshToken(final OAuth2RefreshToken token) {
    final Optional<CassandraRefreshTokenAuthentication> refreshTokenAuth = refreshTokenAuthRepository.findByRefreshTokenId(token.getValue());
    if (refreshTokenAuth.isPresent()) {
      return AuthSerialization.deserialize(refreshTokenAuth.get().getAuth());
    }
    return null;
  }

  @Override
  public void removeRefreshToken(final OAuth2RefreshToken token) {
    final String refreshTokenId = token.getValue();
    final Optional<CassandraRefreshToken> refreshToken = refreshTokenRepository.findByRefreshTokenId(refreshTokenId);
    if (refreshToken.isPresent()) {
      final List<Object> entitiesList = new ArrayList<Object>();
      entitiesList.add(refreshToken.get());
      final Optional<CassandraRefreshTokenAuthentication> refreshTokenAuth = refreshTokenAuthRepository.findByRefreshTokenId(refreshTokenId);
      if (refreshTokenAuth.isPresent()) {
        entitiesList.add(refreshTokenAuth.get());
      }
      final Optional<CassandraRefreshTokenToAccessToken> refreshTokenToAccessToken = refreshTokenToAccessTokenRepository.findByRefreshTokenId(refreshTokenId);
      if (refreshTokenToAccessToken.isPresent()) {
        entitiesList.add(refreshTokenToAccessToken.get());
      }
      // Batch delete
      final CassandraBatchOperations batch = cassandraOperations.batchOps();
      batch.delete(entitiesList);
      batch.execute();
    }
  }

  @Override
  public void removeAccessTokenUsingRefreshToken(final OAuth2RefreshToken refreshToken) {
    final String refreshTokenId = refreshToken.getValue();
    final Optional<CassandraRefreshTokenToAccessToken> oRefreshTokenToAccessToken = refreshTokenToAccessTokenRepository.findByRefreshTokenId(refreshTokenId);
    if (oRefreshTokenToAccessToken.isPresent()) {
      final CassandraRefreshTokenToAccessToken refreshTokenToAccessToken = oRefreshTokenToAccessToken.get();
      final List<Object> entitiesList = new ArrayList<Object>();
      // Delete refresh token to access token
      entitiesList.add(refreshTokenToAccessToken);
      final String accessTokenId = refreshTokenToAccessToken.getRefreshTokenId();
      final Optional<CassandraAccessToken> oAccessToken = accessTokenRepository.findByAccessTokenId(accessTokenId);
      if (!oAccessToken.isPresent()) {
        return;
      }
      // Delete access token
      entitiesList.add(oAccessToken.get());
      final Optional<CassandraAuthentication> oAuthentication = authRepository.findByAccessTokenId(accessTokenId);
      if (oAuthentication.isPresent()) {
        entitiesList.add(oAuthentication.get());
        final OAuth2Authentication oauth = AuthSerialization.deserialize(oAuthentication.get().getAuthentication());
        final String authKey = authenticationKeyGenerator.extractKey(oauth);
        final Optional<CassandraAuthToAccessToken> authToAccessToken = authToAccessTokenRepository.findByAuthKey(authKey);
        if (authToAccessToken.isPresent()) {
          entitiesList.add(authToAccessToken.get());
        }
        final String usernameKey = UsernameUtils.getApprovalKey(oauth);
        final Optional<CassandraUsernameToAccessToken> usernameToAccessToken = usernameToAccessTokenRepository.findByApprovalKeyAndAccessToken(usernameKey, oAccessToken.get().getAccessTokenStr());
        if (usernameToAccessToken.isPresent()) {
          entitiesList.add(usernameToAccessToken.get());
        }

        final String clientId = oauth.getOAuth2Request().getClientId();
        final Optional<CassandraClientIdToAccessToken> clientIdToAccessToken = clientIdToAccessTokenRepository.findByClientIdAndAccessToken(clientId, oAccessToken.get().getAccessTokenStr());
        if (clientIdToAccessToken.isPresent()) {
          entitiesList.add(clientIdToAccessToken);
        }
      }
      final CassandraBatchOperations batch = cassandraOperations.batchOps();
      batch.delete(entitiesList);
      batch.execute();
    }

  }

  @Override
  public OAuth2AccessToken getAccessToken(final OAuth2Authentication authentication) {
    final String authenticationId = authenticationKeyGenerator.extractKey(authentication);
    final Optional<CassandraAuthToAccessToken> oAuthToAccessToken = authToAccessTokenRepository.findByAuthKey(authenticationId);
    if (oAuthToAccessToken.isPresent()) {
      final OAuth2AccessToken accessToken = AccessTokenSerialization.deserialize(oAuthToAccessToken.get().getAccessToken());
      if (accessToken != null && !authenticationId.equals(this.authenticationKeyGenerator.extractKey(this.readAuthentication(accessToken)))) {
        this.storeAccessToken(accessToken, authentication);
      }
    }
    return null;
  }

  @Override
  public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(final String clientId, final String userName) {
    final String approvalKey = UsernameUtils.getApprovalKey(clientId, userName);
    final Optional<List<CassandraUsernameToAccessToken>> optionalUsernameToAccessTokenSet = usernameToAccessTokenRepository.findByApprovalKey(approvalKey);
    final Collection<OAuth2AccessToken> tokens = new ArrayList<OAuth2AccessToken>();
    if (optionalUsernameToAccessTokenSet.isPresent()) {
      optionalUsernameToAccessTokenSet.get().forEach(cUsernameAccessToken -> {
        tokens.add(AccessTokenSerialization.deserialize(cUsernameAccessToken.getAccessToken()));
      });
    }
    return tokens;
  }

  @Override
  public Collection<OAuth2AccessToken> findTokensByClientId(final String clientId) {
    final Optional<List<CassandraClientIdToAccessToken>> optionalClientIdToAccessTokenSet = clientIdToAccessTokenRepository.findByClientId(clientId);
    final Collection<OAuth2AccessToken> tokens = new ArrayList<OAuth2AccessToken>();
    if (optionalClientIdToAccessTokenSet.isPresent()) {
      optionalClientIdToAccessTokenSet.get().forEach(clientIdToAccessToken -> {
        tokens.add(AccessTokenSerialization.deserialize(clientIdToAccessToken.getAccessToken()));
      });
    }
    return tokens;
  }

}
