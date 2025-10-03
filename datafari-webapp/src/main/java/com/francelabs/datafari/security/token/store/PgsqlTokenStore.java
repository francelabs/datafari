package com.francelabs.datafari.security.token.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;

import com.francelabs.datafari.security.token.model.*;
import com.francelabs.datafari.security.token.repository.*;
import com.francelabs.datafari.security.token.utils.*;

import java.util.*;

@Service
public class PgsqlTokenStore implements TokenStore {

    @Autowired
    private PgsqlAccessTokenRepository accessTokenRepository;
    @Autowired
    private PgsqlRefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PgsqlAuthenticationRepository authRepository;
    @Autowired
    private PgsqlAuthToAccessTokenRepository authToAccessTokenRepository;
    @Autowired
    private PgsqlRefreshTokenAuthenticationRepository refreshTokenAuthRepository;
    @Autowired
    private PgsqlRefreshTokenToAccessTokenRepository refreshTokenToAccessTokenRepository;
    @Autowired
    private PgsqlClientIdToAccessTokenRepository clientIdToAccessTokenRepository;
    @Autowired
    private PgsqlUsernameToAccessTokenRepository usernameToAccessTokenRepository;

    private final AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

    @Override
    public OAuth2Authentication readAuthentication(final OAuth2AccessToken token) {
        return readAuthentication(token.getValue());
    }

    @Override
    public OAuth2Authentication readAuthentication(final String token) {
        Optional<PgsqlAuthentication> authentication = authRepository.findByAccessTokenId(token);
        return authentication.map(a -> AuthSerialization.deserialize(a.getAuthentication())).orElse(null);
    }

    @Override
    public void storeAccessToken(final OAuth2AccessToken token, final OAuth2Authentication authentication) {
        PgsqlAccessToken pgsqlAccessToken = new PgsqlAccessToken(token.getValue(), AccessTokenSerialization.serialize(token));
        accessTokenRepository.save(pgsqlAccessToken);

        PgsqlAuthentication pgsqlAuth = new PgsqlAuthentication(token.getValue(), AuthSerialization.serialize(authentication));
        authRepository.save(pgsqlAuth);

        String authKey = authenticationKeyGenerator.extractKey(authentication);
        PgsqlAuthToAccessToken authToAccessToken = new PgsqlAuthToAccessToken(authKey, AccessTokenSerialization.serialize(token));
        authToAccessTokenRepository.save(authToAccessToken);

        OAuth2RefreshToken refreshToken = token.getRefreshToken();
        if (refreshToken != null && refreshToken.getValue() != null) {
            PgsqlRefreshTokenToAccessToken refreshTokenToAccessToken = new PgsqlRefreshTokenToAccessToken(token.getValue(), refreshToken.getValue());
            refreshTokenToAccessTokenRepository.save(refreshTokenToAccessToken);
        }

        PgsqlClientIdToAccessToken clientIdToAccessToken = new PgsqlClientIdToAccessToken(authentication.getOAuth2Request().getClientId(), AccessTokenSerialization.serialize(token));
        clientIdToAccessTokenRepository.save(clientIdToAccessToken);

        PgsqlUsernameToAccessToken usernameToAccessToken = new PgsqlUsernameToAccessToken(UsernameUtils.getApprovalKey(authentication), AccessTokenSerialization.serialize(token));
        usernameToAccessTokenRepository.save(usernameToAccessToken);
    }

    @Override
    public OAuth2AccessToken readAccessToken(final String tokenValue) {
        Optional<PgsqlAccessToken> accessToken = accessTokenRepository.findByAccessTokenId(tokenValue);
        return accessToken.map(t -> AccessTokenSerialization.deserialize(t.getAccessToken())).orElse(null);
    }

    @Override
    public void removeAccessToken(final OAuth2AccessToken token) {
        final String tokenId = token.getValue();
        accessTokenRepository.deleteById(tokenId);
        authRepository.deleteById(tokenId);

        Optional<PgsqlAuthentication> authOpt = authRepository.findByAccessTokenId(tokenId);
        if (authOpt.isPresent()) {
            OAuth2Authentication oauth = AuthSerialization.deserialize(authOpt.get().getAuthentication());
            String authKey = authenticationKeyGenerator.extractKey(oauth);

            authToAccessTokenRepository.findByAuthKey(authKey).ifPresent(authToAccessTokenRepository::delete);

            String usernameKey = UsernameUtils.getApprovalKey(oauth);
            usernameToAccessTokenRepository.findByApprovalKeyAndAccessToken(usernameKey, tokenId)
                .ifPresent(usernameToAccessTokenRepository::delete);

            String clientId = oauth.getOAuth2Request().getClientId();
            clientIdToAccessTokenRepository.findByClientIdAndAccessToken(clientId, tokenId)
                .ifPresent(clientIdToAccessTokenRepository::delete);
        }
        refreshTokenToAccessTokenRepository.findByRefreshTokenId(tokenId)
            .ifPresent(refreshTokenToAccessTokenRepository::delete);
    }

    @Override
    public void storeRefreshToken(final OAuth2RefreshToken refreshToken, final OAuth2Authentication authentication) {
        PgsqlRefreshToken pgsqlRefreshToken = new PgsqlRefreshToken(refreshToken.getValue(), RefreshTokenSerialization.serialize(refreshToken));
        refreshTokenRepository.save(pgsqlRefreshToken);

        PgsqlRefreshTokenAuthentication pgsqlRefreshTokenAuth = new PgsqlRefreshTokenAuthentication(refreshToken.getValue(), AuthSerialization.serialize(authentication));
        refreshTokenAuthRepository.save(pgsqlRefreshTokenAuth);
    }

    @Override
    public OAuth2RefreshToken readRefreshToken(final String tokenValue) {
        Optional<PgsqlRefreshToken> refreshToken = refreshTokenRepository.findByRefreshTokenId(tokenValue);
        return refreshToken.map(t -> RefreshTokenSerialization.deserialize(t.getRefreshToken())).orElse(null);
    }

    @Override
    public OAuth2Authentication readAuthenticationForRefreshToken(final OAuth2RefreshToken token) {
        Optional<PgsqlRefreshTokenAuthentication> refreshTokenAuth = refreshTokenAuthRepository.findByRefreshTokenId(token.getValue());
        return refreshTokenAuth.map(a -> AuthSerialization.deserialize(a.getAuth())).orElse(null);
    }

    @Override
    public void removeRefreshToken(final OAuth2RefreshToken token) {
        String tokenId = token.getValue();
        refreshTokenRepository.deleteById(tokenId);
        refreshTokenAuthRepository.deleteById(tokenId);
        refreshTokenToAccessTokenRepository.findByRefreshTokenId(tokenId)
            .ifPresent(refreshTokenToAccessTokenRepository::delete);
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(final OAuth2RefreshToken refreshToken) {
        String refreshTokenId = refreshToken.getValue();
        Optional<PgsqlRefreshTokenToAccessToken> oRefreshTokenToAccessToken = refreshTokenToAccessTokenRepository.findByRefreshTokenId(refreshTokenId);
        if (oRefreshTokenToAccessToken.isPresent()) {
            PgsqlRefreshTokenToAccessToken refreshTokenToAccessToken = oRefreshTokenToAccessToken.get();
            String accessTokenId = refreshTokenToAccessToken.getRefreshTokenId();
            Optional<PgsqlAccessToken> oAccessToken = accessTokenRepository.findByAccessTokenId(accessTokenId);
            if (oAccessToken.isPresent()) {
                removeAccessToken(AccessTokenSerialization.deserialize(oAccessToken.get().getAccessToken()));
            }
            refreshTokenToAccessTokenRepository.delete(refreshTokenToAccessToken);
        }
    }

    @Override
    public OAuth2AccessToken getAccessToken(final OAuth2Authentication authentication) {
        String authenticationId = authenticationKeyGenerator.extractKey(authentication);
        Optional<PgsqlAuthToAccessToken> oAuthToAccessToken = authToAccessTokenRepository.findByAuthKey(authenticationId);
        if (oAuthToAccessToken.isPresent()) {
            OAuth2AccessToken accessToken = AccessTokenSerialization.deserialize(oAuthToAccessToken.get().getAccessToken());
            if (accessToken != null && !authenticationId.equals(this.authenticationKeyGenerator.extractKey(this.readAuthentication(accessToken)))) {
                this.storeAccessToken(accessToken, authentication);
            }
            return accessToken;
        }
        return null;
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(final String clientId, final String userName) {
        String approvalKey = UsernameUtils.getApprovalKey(clientId, userName);
        Optional<List<PgsqlUsernameToAccessToken>> optionalUsernameToAccessTokenSet = usernameToAccessTokenRepository.findByApprovalKey(approvalKey);
        Collection<OAuth2AccessToken> tokens = new ArrayList<>();
        if (optionalUsernameToAccessTokenSet.isPresent()) {
            optionalUsernameToAccessTokenSet.get().forEach(u -> {
                tokens.add(AccessTokenSerialization.deserialize(u.getAccessToken()));
            });
        }
        return tokens;
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientId(final String clientId) {
      List<PgsqlClientIdToAccessToken> clientIdToAccessTokenList = clientIdToAccessTokenRepository.findByClientId(clientId);
      Collection<OAuth2AccessToken> tokens = new ArrayList<>();
      if (clientIdToAccessTokenList != null && !clientIdToAccessTokenList.isEmpty()) {
          clientIdToAccessTokenList.forEach(c -> {
              tokens.add(AccessTokenSerialization.deserialize(c.getAccessToken()));
          });
      }
      return tokens;
  }
}