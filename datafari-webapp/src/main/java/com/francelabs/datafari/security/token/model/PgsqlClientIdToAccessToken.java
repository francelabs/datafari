package com.francelabs.datafari.security.token.model;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "oauth_client_id_to_access_token")
@IdClass(PgsqlClientIdToAccessToken.PK.class)
public class PgsqlClientIdToAccessToken {

    @Id
    private String clientId;

    @Id
    private String accessToken;

    public PgsqlClientIdToAccessToken() {}

    public PgsqlClientIdToAccessToken(String clientId, String accessToken) {
        this.clientId = clientId;
        this.accessToken = accessToken;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public static class PK implements Serializable {
        private String clientId;
        private String accessToken;

        public PK() {}

        public PK(String clientId, String accessToken) {
            this.clientId = clientId;
            this.accessToken = accessToken;
        }

        // getters, setters, equals, hashCode
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK)) return false;
            PK pk = (PK) o;
            return clientId.equals(pk.clientId) && accessToken.equals(pk.accessToken);
        }
        @Override
        public int hashCode() {
            return clientId.hashCode() ^ accessToken.hashCode();
        }
    }
}