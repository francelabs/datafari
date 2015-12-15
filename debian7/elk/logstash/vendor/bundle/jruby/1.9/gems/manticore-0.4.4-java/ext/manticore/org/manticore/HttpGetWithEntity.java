package org.manticore;

import java.net.URI;
import org.apache.http.client.methods.HttpPost;

public class HttpGetWithEntity extends HttpPost {
    public final static String METHOD_NAME = "GET";

    public HttpGetWithEntity(URI url) {
        super(url);
    }

    public HttpGetWithEntity(String url) {
        super(url);
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}