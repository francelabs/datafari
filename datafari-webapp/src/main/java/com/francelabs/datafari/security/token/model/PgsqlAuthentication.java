package com.francelabs.datafari.security.token.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "oauth_authentications")
public class PgsqlAuthentication {

    @Id
    private String accessTokenId;

    private String authentication;

    public PgsqlAuthentication() {}

    public PgsqlAuthentication(String accessTokenId, String authentication) {
        this.accessTokenId = accessTokenId;
        this.authentication = authentication;
    }

    public String getAccessTokenId() {
        return accessTokenId;
    }

    public void setAccessTokenId(String accessTokenId) {
        this.accessTokenId = accessTokenId;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }
}