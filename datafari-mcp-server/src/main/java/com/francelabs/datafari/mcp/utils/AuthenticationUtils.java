package com.francelabs.datafari.mcp.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


public class AuthenticationUtils {

    private final boolean allowAnonymousUsers;

    public AuthenticationUtils(@Value("${mcp.allow.anonymous.users:false}") boolean allowAnonymousUsers) {
        this.allowAnonymousUsers = allowAnonymousUsers;
    }

    public HttpHeaders getForwardedAuthenticationHeaders() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder
                        .currentRequestAttributes())
                        .getRequest();

        HttpHeaders headers = new HttpHeaders();

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }

        // TODO : Currently, we are not able to retrieve the username in the module. We wan only check if there is an access token.
//        boolean authenticated = (request.getUserPrincipal() != null)
//                        || (authorization != null && !authorization.isBlank());
        boolean authenticated = (authorization != null && !authorization.isBlank());


        if (!authenticated && !allowAnonymousUsers) {
            throw new IllegalStateException(
                    "Authentication is required to use this MCP server."
            );
        }

        return headers;
    }

}
