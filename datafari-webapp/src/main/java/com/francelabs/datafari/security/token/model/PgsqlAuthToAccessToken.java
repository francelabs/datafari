package com.francelabs.datafari.security.token.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "oauth_auth_to_access_token")
public class PgsqlAuthToAccessToken {

    @Id
    private String authKey;

    private String accessToken;

    public PgsqlAuthToAccessToken() {}

    public PgsqlAuthToAccessToken(String authKey, String accessToken) {
        this.authKey = authKey;
        this.accessToken = accessToken;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}