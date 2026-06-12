package com.francelabs.datafari.ai.services.common;

import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.socket.WebSocketSession;

import java.security.Principal;

// TODO : DELETE ?
public class AiRequestContext {

    private final HttpServletRequest httpRequest;
    private final Principal principal;
    private final String username;
    private final String lang;

    public static AiRequestContext fromHttp(
            HttpServletRequest request,
            AiRequest params
    ) {
        return new AiRequestContext(
                request,
                request.getUserPrincipal(),
                AuthenticatedUserName.getName(request),
                params.lang
        );
    }

    public static AiRequestContext fromWebSocket(
            WebSocketSession session,
            AiRequest params
    ) {
        return new AiRequestContext(
                null,
                session.getPrincipal(),
                AuthenticatedUserName.getName(session.getPrincipal()),
                params.lang
        );
    }

    public AiRequestContext(
            HttpServletRequest httpRequest,
            Principal principal,
            String username,
            String lang
    ) {
        this.httpRequest = httpRequest;
        this.principal = principal;
        this.username = username;
        this.lang = lang;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getUsername() {
        return username;
    }

    public String getLang() {
        return lang;
    }

    public boolean hasHttpRequest() {
        return httpRequest != null;
    }
}
