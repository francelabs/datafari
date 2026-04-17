package com.francelabs.datafari.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.PathPatternRequestMatcherBuilderFactoryBean;

@Configuration
@EnableWebSecurity
public class DatafariWebSecurityEnabler {
  /**
   * This bean instructs the Spring Security DSL to use PathPatternRequestMatcher for all request matchers that it constructs itself,
   * in particular, to configure the PathPatternRequestMatcher.Builder
   *
   * @return
   */
  @Bean
  PathPatternRequestMatcherBuilderFactoryBean usePathPattern() {
    return new PathPatternRequestMatcherBuilderFactoryBean();
  }
}
