package com.francelabs.datafari.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Cookie;

import java.security.Principal;
import java.util.*;

public class WebSocketHttpServletRequest extends HttpServletRequestWrapper {

    private final Principal principal;
    private final HttpSession session;
    private final Cookie[] cookies;
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String[]> params = new HashMap<>();
    private final Map<String, List<String>> headers = new HashMap<>();

    private final String contextPath;
    private final String servletPath;
    private final String pathInfo;
    private final String requestUri;
    private final String method;
    private final String scheme;
    private final String serverName;
    private final int serverPort;
    private final String remoteAddr;

    public WebSocketHttpServletRequest(HttpServletRequest request) {
        super(request);

        this.principal = request.getUserPrincipal();
        this.session = request.getSession(false);
        this.cookies = request.getCookies();

        this.contextPath = request.getContextPath();
        this.servletPath = request.getServletPath();
        this.pathInfo = request.getPathInfo();
        this.requestUri = request.getRequestURI();
        this.method = request.getMethod();
        this.scheme = request.getScheme();
        this.serverName = request.getServerName();
        this.serverPort = request.getServerPort();
        this.remoteAddr = request.getRemoteAddr();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name.toLowerCase(Locale.ROOT), Collections.list(request.getHeaders(name)));
        }

        Map<String, String[]> originalParams = request.getParameterMap();
        if (originalParams != null) {
            params.putAll(originalParams);
        }
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
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return Collections.enumeration(values != null ? values : List.of());
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void addParameter(String name, String value) {
        if (value != null) {
            params.put(name, new String[]{value});
        }
    }

    public void addParameters(String name, String[] values) {
        if (values != null) {
            params.put(name, values);
        }
    }

    @Override
    public String getParameter(String name) {
        String[] values = params.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return params.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return new HashMap<>(params);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(params.keySet());
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getRequestURI() {
        return requestUri;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }
}