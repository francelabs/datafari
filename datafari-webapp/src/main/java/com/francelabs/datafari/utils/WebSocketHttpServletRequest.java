package com.francelabs.datafari.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

public class WebSocketHttpServletRequest extends HttpServletRequestWrapper {

    private final Principal principal;
    private final HttpSession session;
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String[]> params = new HashMap<>();

    public WebSocketHttpServletRequest(HttpServletRequest request) {
        super(request);
        this.principal = request.getUserPrincipal();
        this.session = request.getSession(false);
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public String getRemoteUser() {
        return principal != null ? principal.getName() : null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return session;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.containsKey(name)
                ? attributes.get(name)
                : super.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void addParameter(String name, String value) {
        params.put(name, new String[]{value});
    }

    @Override
    public String getParameter(String name) {
        String[] values = params.get(name);
        return values != null && values.length > 0 ? values[0] : super.getParameter(name);
    }

    @Override
    public String[] getParameterValues(String name) {
        return params.containsKey(name) ? params.get(name) : super.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> result = new HashMap<>(super.getParameterMap());
        result.putAll(params);
        return result;
    }
}