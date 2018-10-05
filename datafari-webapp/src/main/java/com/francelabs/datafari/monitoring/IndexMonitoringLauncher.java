package com.francelabs.datafari.monitoring;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class IndexMonitoringLauncher implements ServletContextListener {

	@Override
	public void contextDestroyed(final ServletContextEvent arg0) {
		IndexMonitoring.getInstance().stopIndexMonitoring();

	}

	@Override
	public void contextInitialized(final ServletContextEvent arg0) {
		IndexMonitoring.getInstance().startIndexMonitoring();

	}

}
