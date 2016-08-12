package com.francelabs.datafari.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

public final class ExecutionEnvironment {

	private ExecutionEnvironment() {
	}

	public static String getDevExecutionEnvironment() {

		String env = null;

		// If in development environment
		final RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		final List<String> arguments = runtimeMxBean.getInputArguments();
		for (final String s : arguments) {
			// Gets the D.solr.solr.home variable given in arguments to the VM
			if (s.startsWith("-Dsolr.solr.home")) {
				env = s.substring(s.indexOf("=") + 1, s.indexOf("solr_home") - 5);
			}
		}

		if (env == null) {
			env = System.getProperty("DATAFARI_HOME");
		}

		// TODO Throw something like ConfigMissingException in case of null

		return env;
	}
}
