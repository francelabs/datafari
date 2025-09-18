package com.francelabs.datafari.security.token.model;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "oauth_username_to_access_token")
@IdClass(PgsqlUsernameToAccessToken.PK.class)
public class PgsqlUsernameToAccessToken {

    @Id
    private String approvalKey;

    @Id
    private String accessToken;

    public PgsqlUsernameToAccessToken() {}

    public PgsqlUsernameToAccessToken(String approvalKey, String accessToken) {
        this.approvalKey = approvalKey;
        this.accessToken = accessToken;
    }

    public String getApprovalKey() {
        return approvalKey;
    }

    public void setApprovalKey(String approvalKey) {
        this.approvalKey = approvalKey;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public static class PK implements Serializable {
        private String approvalKey;
        private String accessToken;

        public PK() {}

        public PK(String approvalKey, String accessToken) {
            this.approvalKey = approvalKey;
            this.accessToken = accessToken;
        }

        public String getApprovalKey() { return approvalKey; }
        public void setApprovalKey(String approvalKey) { this.approvalKey = approvalKey; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK)) return false;
            PK pk = (PK) o;
            return approvalKey.equals(pk.approvalKey) && accessToken.equals(pk.accessToken);
        }
        @Override
        public int hashCode() {
            return approvalKey.hashCode() ^ accessToken.hashCode();
        }
    }
}