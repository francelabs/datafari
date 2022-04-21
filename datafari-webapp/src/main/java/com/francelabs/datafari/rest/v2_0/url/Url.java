package com.francelabs.datafari.rest.v2_0.url;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.francelabs.datafari.servlets.URL;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Url {

    @GetMapping("/rest/v2.0/url")
    public void performRedirect(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        URL.performGet(request, response);
    }
}
