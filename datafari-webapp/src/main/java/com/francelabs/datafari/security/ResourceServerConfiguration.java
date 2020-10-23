package com.francelabs.datafari.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.francelabs.datafari.api.SearchAPI;
import com.francelabs.datafari.api.SuggesterAPI;
import com.francelabs.datafari.security.client.repo.CassandraClientDetailsRepository;
import com.francelabs.datafari.utils.AuthenticatedUserName;

@Configuration
@EnableResourceServer
@RestController
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

  @Autowired
  CassandraClientDetailsRepository clientDetailsRepo;

  @Autowired
  PasswordEncoder passwordEncoder;

  @Override
  public void configure(final HttpSecurity http) throws Exception {
    http.antMatcher("/api/**").authorizeRequests().anyRequest().permitAll();
  }

  @RequestMapping("/api/test")
  public String test(final HttpServletRequest request) {
    final String authenticatedUserName = AuthenticatedUserName.getName(request);
    if (authenticatedUserName != null) {
      String role = "";
      if (request.isUserInRole("SearchAdministrator")) {
        role = " is a SearchAdministrator";
      } else if (request.isUserInRole("SearchExpert")) {
        role = " is a SearchExpert";
      }
      return authenticatedUserName + role;
    } else {
      return "no user";
    }
  }

  @RequestMapping(value = "/api/search/*", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
  public String search(final HttpServletRequest request) {
    String searchResponse = "";
    final String action = request.getParameter("action");
    if (action != null) {
      switch (action) {
        case "suggest":
          searchResponse = SuggesterAPI.suggest(request);
          break;
        case "search":
        default:
          searchResponse = SearchAPI.search(request);
      }
    } else {
      searchResponse = SearchAPI.search(request);
    }
    return searchResponse;
  }

}
