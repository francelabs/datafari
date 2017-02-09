package com.francelabs.datafari.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.francelabs.datafari.utils.AdvancedSearchConfiguration;

@WebListener
public class PropertiesWatchersLauncher implements ServletContextListener {

	@Override
	public void contextDestroyed(final ServletContextEvent arg0) {
		AdvancedSearchConfiguration.getInstance().stopListeningChanges();
	}

	@Override
	public void contextInitialized(final ServletContextEvent arg0) {
		AdvancedSearchConfiguration.getInstance().listenChanges();
	}
}
