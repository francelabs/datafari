package com.francelabs.datafari.security.token.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "oauth_refresh_token_to_access_token")
public class PgsqlRefreshTokenToAccessToken {

    @Id
    private String refreshTokenId;
    private String accessTokenId;

    public PgsqlRefreshTokenToAccessToken() {
        // Default constructor for JPA
    }

    public PgsqlRefreshTokenToAccessToken(String refreshTokenId, String accessTokenId) {
        this.refreshTokenId = refreshTokenId;
        this.accessTokenId = accessTokenId;
    }

    public String getRefreshTokenId() {
        return refreshTokenId;
    }

    public void setRefreshTokenId(String refreshTokenId) {
        this.refreshTokenId = refreshTokenId;
    }

    public String getAccessTokenId() {
        return accessTokenId;
    }

    public void setAccessTokenId(String accessTokenId) {
        this.accessTokenId = accessTokenId;
    }
}