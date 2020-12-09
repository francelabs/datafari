package com.francelabs.datafari.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import com.francelabs.datafari.initializers.IInitializer;

@SpringBootApplication
@ComponentScan({"com.francelabs.datafari.security", "com.francelabs.datafari.rest.v1_0"})
public class DatafariWebSecurityApplication extends SpringBootServletInitializer {

  private static final Logger LOGGER = LogManager.getLogger(DatafariWebSecurityApplication.class.getName());

  private static final List<IInitializer> listInitializers = new ArrayList<IInitializer>();

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
    final Reflections reflections = new Reflections("com.francelabs.datafari.initializers");
    final Set<Class<? extends IInitializer>> initializers = reflections.getSubTypesOf(IInitializer.class);
    initializers.forEach(c -> {
      try {
        LOGGER.debug("Found initializer " + c.getClass().getSimpleName());
        final IInitializer initializer = c.newInstance();
        listInitializers.add(initializer);
        initializer.initialize();
        LOGGER.debug("Initializer " + c.getClass().getSimpleName() + " enabled !");
      } catch (InstantiationException | IllegalAccessException e) {
        LOGGER.error("Unable to instanciate the initializer class: " + c.getSimpleName(), e);
      }
    });
    servletContext.addListener(new DatafariSessionListener());
  }

  @PreDestroy
  public void onExit() {
    listInitializers.forEach(i -> {
      LOGGER.debug("Stopping initializer " + i.getClass().getSimpleName() + " ...");
      i.shutdown();
      LOGGER.debug("Initializer " + i.getClass().getSimpleName() + " stopped !");
    });
    try {
      // Wait for everything to stop
      Thread.sleep(250);
    } catch (final InterruptedException e) {
      // Nothing
    }
  }
}
