package com.francelabs.datafari.elk;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.francelabs.datafari.utils.ScriptConfiguration;

@WebListener
public class ELKLauncher implements ServletContextListener {

	private static final String ELKACTIVATION = "ELKactivation";

	@Override
	public void contextDestroyed(final ServletContextEvent arg0) {
		ActivateELK.getInstance().disactivate();
	}

	@Override
	public void contextInitialized(final ServletContextEvent arg0) {
		boolean activated = false;
		try {
			activated = Boolean.parseBoolean(ScriptConfiguration.getProperty(ELKACTIVATION));
		} catch (final IOException e) {
			activated = false;
		}
		if (activated) {
			ActivateELK.getInstance().activate();
		}

	}

}