package com.francelabs.datafari.security;

import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.francelabs.datafari")
public class DatafariWebSecurityApplication extends SpringBootServletInitializer {

  private static final Logger LOGGER = LogManager.getLogger(DatafariWebSecurityApplication.class.getName());

  @Override
  protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
    return builder.sources(DatafariWebSecurityApplication.class);
  }

  public static void main(final String[] args) {
    SpringApplication.run(DatafariWebSecurityApplication.class, args);
  }

  @Override
  public void onStartup(final ServletContext servletContext) throws ServletException {
    super.onStartup(servletContext);
    // Force the session cookie to be created both with https and http protocol,
    // because some spring libraries override the default config to only enable
    // the session cookie with https protocol
    servletContext.getSessionCookieConfig().setSecure(false);

    // Only add listeners explicitly needed
    servletContext.addListener(new DatafariSessionListener());

    LOGGER.info("DatafariWebSecurityApplication started successfully, Spring will manage BootHooks initialization.");
  }

  @PreDestroy
  public void onExit() {
    try {
      // Wait a little to let shutdown hooks complete
      Thread.sleep(250);
    } catch (final InterruptedException e) {
      // Nothing
    }
    LOGGER.info("DatafariWebSecurityApplication shutdown complete.");
  }
}