package com.francelabs.datafari.security;

import com.francelabs.datafari.security.auth.DatafariAuthenticationSuccessHandler;
import com.francelabs.datafari.utils.DatafariMainConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared HTTP security support for Datafari CE and EE.
 *
 * <p>This abstract class centralizes the reusable Spring Security HTTP rules
 * shared by the different authentication modes: common URL matchers, standard
 * authorization rules, login configuration, logout configuration, CSRF rules
 * and session concurrency handling.</p>
 *
 * <p>Concrete security configuration classes can extend this support class in
 * order to keep a homogeneous security filter chain structure while only
 * declaring their mechanism-specific behavior.</p>
 */
public abstract class DatafariHttpSecuritySupport {
  protected static final int MAX_CONCURRENT_SESSIONS = Integer.parseInt(DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.MAX_CONCURRENT_SESSIONS));

  protected static RequestMatcher antAny(String... patterns) {
    final List<RequestMatcher> matchers = new ArrayList<>(patterns.length);
    for (String pattern : patterns) {
      matchers.add(new AntPathRequestMatcher(pattern));
    }
    return new OrRequestMatcher(matchers);
  }

  protected static final RequestMatcher ADMIN_OR_EXPERT =
      antAny("/admin/**", "/SearchExpert/**");

  protected static final RequestMatcher ADMIN_ONLY =
      antAny("/SearchAdministrator/**", "/rest/v2.0/files/**", "/rest/v2.0/management/**");

  /**
   * Public URLs that must remain reachable without authentication.
   *
   * <p>These routes mainly correspond to public application features, technical
   * entry points and static resources. The default authorization model of
   * Datafari remains permissive: unmatched requests are also permitted unless a
   * dedicated rule requires authentication or roles.</p>
   */
  protected static final RequestMatcher PERMIT_ALL =
      antAny(
          "/RefreshSession",
          "/rest/**",
          "/applyLang",
          "/resources/**",
          "/error",
          "/oauth/**",
          "/login",
          "/login*",
          "/logout",
          "/index.jsp", "/WEB-INF/view/login.jsp");

  /**
   * Applies the standard Datafari authorization rules with a permissive
   * fallback policy.
   *
   * <p>Only a small subset of endpoints requires authentication or elevated
   * roles. All remaining requests are explicitly allowed.</p>
   *
   * @param registry the authorization registry to configure
   */
  protected static void applyStandardRequestMatchers(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {

    registry.requestMatchers(antAny("/rest/v1.0/auth*")).authenticated();
    registry.requestMatchers(ADMIN_OR_EXPERT).hasAnyRole("SearchExpert", "SearchAdministrator");
    registry.requestMatchers(ADMIN_ONLY).hasRole("SearchAdministrator");
    registry.anyRequest().permitAll();
  }

  /**
   * Applies the standard Datafari logout configuration.
   *
   * @param logoutConfigurer the logout configurer to customize
   */
  protected static void applyLogoutConfig(LogoutConfigurer<HttpSecurity> logoutConfigurer){
    logoutConfigurer.logoutRequestMatcher(new AntPathRequestMatcher("/logout"));
    logoutConfigurer.logoutSuccessUrl("/index.jsp");
    logoutConfigurer.invalidateHttpSession(true);
    logoutConfigurer.clearAuthentication(true);
    logoutConfigurer.deleteCookies("JSESSIONID");
  }

  /**
   * Applies the standard Datafari form login configuration.
   *
   * @param formLoginConfigurer the form login configurer to customize
   */
  protected static void applyLoginConfig(FormLoginConfigurer<HttpSecurity> formLoginConfigurer){
    formLoginConfigurer.loginPage("/login");
    formLoginConfigurer.defaultSuccessUrl("/index.jsp", false);
    formLoginConfigurer.successHandler(new DatafariAuthenticationSuccessHandler());
  }

  /**
   * Applies the standard Datafari CSRF exclusions.
   *
   * @param csrfConfigurer the CSRF configurer to customize
   */
  protected static void applyCsrfSecurity(CsrfConfigurer<HttpSecurity> csrfConfigurer) {
    csrfConfigurer.ignoringRequestMatchers(
            new AntPathRequestMatcher("/rest/**"),
            new AntPathRequestMatcher("/resources/**"),
            new AntPathRequestMatcher("/error"),
            new AntPathRequestMatcher("/applyLang"));
  }

  /**
   * Applies the standard Datafari session concurrency policy.
   *
   * @param sessionConfigurer the session management configurer to customize
   */
  protected static void applyStandardSessionManagement(
      SessionManagementConfigurer<HttpSecurity> sessionConfigurer) {

    sessionConfigurer.sessionConcurrency(
        concurrency -> concurrency.maximumSessions(MAX_CONCURRENT_SESSIONS)
    );
  }
}