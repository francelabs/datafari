package com.francelabs.datafari.elk;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.francelabs.datafari.utils.ELKConfiguration;

@WebListener
public class ELKLauncher implements ServletContextListener {

	@Override
	public void contextDestroyed(final ServletContextEvent arg0) {
		ActivateELK.getInstance().deactivate();
	}

	@Override
	public void contextInitialized(final ServletContextEvent arg0) {
		boolean activated = false;
		try {
			activated = Boolean.parseBoolean(ELKConfiguration.getProperty(ELKConfiguration.ELK_ACTIVATION));
		} catch (final IOException e) {
			activated = false;
		}
		if (activated) {
			ActivateELK.getInstance().activate();
		}

	}

}