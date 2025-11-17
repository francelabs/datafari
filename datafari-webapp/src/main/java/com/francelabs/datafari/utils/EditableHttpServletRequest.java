package com.francelabs.datafari.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EditableHttpServletRequest extends HttpServletRequestWrapper {

    private final HashMap<String,String[]> params = new HashMap<>();
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
    public String[] getParameterValues(String name) {
        // if we added parameters in this field, return those
        if ( params.get(name) != null) {
            return params.get(name);
        }
        // otherwise return what's in the original request
        HttpServletRequest req = (HttpServletRequest) super.getRequest();
        return req.getParameterValues( name );
    }

    @Override
    public Map<String, String[]> getParameterMap() {

        Map<String, String[]> map = new HashMap<>();
        map.putAll(super.getParameterMap());
        map.putAll(this.params);
        return map;
    }

    public void addParameter( String name, String value ) {
        if (params.get(name) != null) {
          // If the param is existing, we create a new array containing the old values and the new one
          String[] values = params.get(name);
          String[] newArray = Arrays.copyOf(values, values.length+1);
          newArray[values.length] = value;
          params.put( name, newArray);
        } else {
          params.put( name, new String[]{value});
        }
    }

    @Override
    public String getPathInfo() {
        return this.pathInfo;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

}
