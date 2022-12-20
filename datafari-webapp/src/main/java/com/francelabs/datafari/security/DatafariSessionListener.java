package com.francelabs.datafari.security;

import java.io.InputStream;
import java.util.Properties;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.francelabs.datafari.utils.DatafariMainConfiguration;

@WebListener
public class DatafariSessionListener implements HttpSessionListener {

  private final int defaultUnauthenticatedSessionTimeout = 60;

  @Override
  public void sessionCreated(final HttpSessionEvent se) {

    boolean keycloakEnabled = false;
    boolean samlEnabled = false;
    boolean casEnabled = false;
    boolean kerberosEnabled = false;

    try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("application.properties");) {
      final Properties appProps = new Properties();
      appProps.load(is);
      final String keycloakEnabledStr = appProps.getProperty("keycloak.enabled", "false");
      keycloakEnabled = Boolean.parseBoolean(keycloakEnabledStr);
      final String samlEnabledStr = appProps.getProperty("saml.enabled", "false");
      samlEnabled = Boolean.parseBoolean(samlEnabledStr);
      final String casEnabledStr = appProps.getProperty("cas.enabled", "false");
      casEnabled = Boolean.parseBoolean(casEnabledStr);
      final String kerberosEnabledStr = appProps.getProperty("kerberos.enabled", "false");
      kerberosEnabled = Boolean.parseBoolean(kerberosEnabledStr);

    } catch (final Exception e) {
      keycloakEnabled = false;
      samlEnabled = false;
      casEnabled = false;
      kerberosEnabled = false;
    }

    final HttpSession session = se.getSession();
    if (!keycloakEnabled) {
      // Set session timeout
      int sessionTimeout = defaultUnauthenticatedSessionTimeout;
      try {
        sessionTimeout = Integer.valueOf(DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SESSION_TIMEOUT_UNAUTH));
      } catch (final NumberFormatException e) {
        sessionTimeout = defaultUnauthenticatedSessionTimeout;
      }
      session.setMaxInactiveInterval(sessionTimeout);
    } else {
      try {
        session.setMaxInactiveInterval(Integer.valueOf(DatafariMainConfiguration.getInstance().getProperty(DatafariMainConfiguration.SESSION_TIMEOUT_AUTH)));
      } catch (final NumberFormatException e) {
        session.setMaxInactiveInterval(defaultUnauthenticatedSessionTimeout);
      }
    }
  }

}
