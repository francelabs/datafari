package com.francelabs.datafari.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

@Configuration
@EnableResourceServer
/**
 * Declare that any /rest/** API is a oauth resource being able to handle oauth2 tokens
 *
 */
public class OauthResourceServerConfiguration extends ResourceServerConfigurerAdapter {

  @Override
  public void configure(final HttpSecurity http) throws Exception {
    // Only handles requests that contain baerer authorization header ONLY !
    http.antMatcher("/rest/**").requestMatcher(request -> {
      final String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
      boolean handles = false;
      if (auth != null && auth.toLowerCase().startsWith("bearer")) {
        handles = true;
      }
      return handles;
    })
    .authorizeRequests()
    .antMatchers("/rest/v2.0/files/**").hasRole("SearchAdministrator")
    .antMatchers("/rest/v2.0/management/**").hasRole("SearchAdministrator")
    .anyRequest().permitAll();
  }

}
