package com.francelabs.datafari.security.token.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "oauth_refresh_token_auth")
public class PgsqlRefreshTokenAuthentication {

    @Id
    private String refreshTokenId;

    private String authentication;

    public PgsqlRefreshTokenAuthentication() {}

    public PgsqlRefreshTokenAuthentication(String refreshTokenId, String authentication) {
        this.refreshTokenId = refreshTokenId;
        this.authentication = authentication;
    }

    public String getRefreshTokenId() {
        return refreshTokenId;
    }

    public void setRefreshTokenId(String refreshTokenId) {
        this.refreshTokenId = refreshTokenId;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    // For fidelity: alias getter if you want
    public String getAuth() {
        return authentication;
    }
}