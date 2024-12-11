package com.francelabs.datafari.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;

public class EditableHttpServletRequest extends HttpServletRequestWrapper {

    private final HashMap<String,String[]> params = new HashMap<>();
    private final HashMap<String,String> attributes = new HashMap<>();
    private String pathInfo;

    public EditableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        // if we added one, return that one
        if ( params.get(name) != null && params.get(name)[0] != null) {
            return params.get(name)[0];
        }
        // otherwise return what's in the original request
        HttpServletRequest req = (HttpServletRequest) super.getRequest();
        return req.getParameter( name );
    }

    @Override
    public Map<String, String[]> getParameterMap() {

        Map<String, String[]> map = new HashMap<>();
        map.putAll(super.getParameterMap());
        map.putAll(this.params);
        return map;
    }

    @Override
    public Object getAttribute(String name) {
        // if we added one, return that one
        if ( attributes.get(name) != null ) {
            return attributes.get(name);
        }
        // otherwise return what's in the original request
        HttpServletRequest req = (HttpServletRequest) super.getRequest();
        return req.getAttribute(name);
    }

    public void addParameter( String name, String value ) {
        params.put( name, new String[]{value});
    }

    public void addAttribute( String name, String value ) {
        attributes.put( name, value );
    }

    @Override
    public String getPathInfo() {
        return this.pathInfo;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

}
