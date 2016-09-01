package com.francelabs.datafari.elk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.francelabs.datafari.logs.ELKLevel;
import com.francelabs.datafari.utils.Environment;

public class ActivateELK {
	private static String startScriptPath;
	private static String stopScriptPath;
	private static ActivateELK instance;
	private final static Logger logger = Logger.getLogger(ActivateELK.class);

	/**
	 * Constructor
	 *
	 * Retrieve the start-elk and stop-elk scripts paths
	 */
	private ActivateELK() {
		startScriptPath = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "elk" + File.separator + "scripts"
				+ File.separator + "start-elk.sh";
		stopScriptPath = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "elk" + File.separator + "scripts"
				+ File.separator + "stop-elk.sh";
	}

	/**
	 * Singleton
	 *
	 * @return the instance
	 */
	public static ActivateELK getInstance() {
		if (instance == null) {
			return instance = new ActivateELK();
		}
		return instance;
	}

	/**
	 * Start ELK
	 *
	 * @return
	 */
	public void activate() {
		final String[] cmd = new String[] { "/bin/bash", startScriptPath };
		final Thread t = new Thread(new RunnableBashScript(cmd));
		t.start();
	}

	/**
	 * Stop ELK
	 *
	 * @return
	 */
	public void deactivate() {
		final String[] cmd = new String[] { "/bin/bash", stopScriptPath };
		final Thread t = new Thread(new RunnableBashScript(cmd));
		t.start();
	}

	/**
	 * Start ELK remotely
	 *
	 * @param elkServer
	 * @param elkScriptsDir
	 * @return
	 */
	public void activateRemote(final String elkServer, final String elkScriptsDir) {
		final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "start-elk.sh" };
		final Thread t = new Thread(new RunnableBashScript(cmd));
		t.start();
	}

	/**
	 * Stop ELK remotely
	 *
	 * @param elkServer
	 *            the ELK server address
	 * @param elkScriptsDir
	 *            the ELK 'scripts' directory absolute path on the server which
	 *            contains the scripts to start and stop ELK
	 * @return
	 */
	public void deactivateRemote(final String elkServer, final String elkScriptsDir) {
		final String[] cmd = new String[] { "ssh", "datafari@" + elkServer, "/bin/bash", formatDir(elkScriptsDir) + "stop-elk.sh" };
		final Thread t = new Thread(new RunnableBashScript(cmd));
		t.start();
	}

	/**
	 * Format the dir path in order that it ends with a '/'
	 *
	 * @param dir
	 *            the dir path
	 * @return the dir path which ends by a '/'
	 */
	private String formatDir(final String dir) {
		if (dir.endsWith("/")) {
			return dir;
		} else {
			return dir + "/";
		}
	}

	/**
	 * RunnableBashScript
	 *
	 */
	private class RunnableBashScript implements Runnable {
		private final String[] cmd;

		public RunnableBashScript(final String[] cmd) {
			this.cmd = cmd;
		}

		@Override
		public void run() {
			Process p = null;
			try {
				p = new ProcessBuilder(cmd).start();

				if (p != null) {
					final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

					final BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

					String s = null;
					while ((s = stdInput.readLine()) != null) {
						logger.log(ELKLevel.ELK, s);

					}

					while ((s = stdError.readLine()) != null) {
						logger.log(ELKLevel.ELK, s);
					}
				}
			} catch (final IOException e) {
				logger.log(ELKLevel.ELK, e.getMessage());
			}

		}
	}
}
