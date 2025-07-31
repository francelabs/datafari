package com.francelabs.datafari.security.token.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "oauth_access_tokens")
public class PgsqlAccessToken {

    @Id
    private String accessTokenId;

    private String accessToken;

    public PgsqlAccessToken() {}

    public PgsqlAccessToken(String accessTokenId, String accessToken) {
        this.accessTokenId = accessTokenId;
        this.accessToken = accessToken;
    }

    public String getAccessTokenId() {
        return accessTokenId;
    }

    public void setAccessTokenId(String accessTokenId) {
        this.accessTokenId = accessTokenId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessTokenStr() {
        return accessToken;
    }
}