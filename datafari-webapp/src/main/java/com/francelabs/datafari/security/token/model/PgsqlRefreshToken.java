package com.francelabs.datafari.security.token.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "oauth_refresh_tokens")
public class PgsqlRefreshToken {

    @Id
    private String refreshTokenId;
    private String refreshToken;

    // Default constructor required by JPA
    public PgsqlRefreshToken() {
    }

    public PgsqlRefreshToken(final String refreshTokenId, final String refreshToken) {
        this.refreshTokenId = refreshTokenId;
        this.refreshToken = refreshToken;
    }

    public String getRefreshTokenId() {
        return refreshTokenId;
    }

    public void setRefreshTokenId(String refreshTokenId) {
        this.refreshTokenId = refreshTokenId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}