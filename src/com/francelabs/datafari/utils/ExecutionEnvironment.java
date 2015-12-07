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
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = runtimeMxBean.getInputArguments();
		for (String s : arguments) {
			// Gets the D.solr.solr.home variable given in arguments to the VM
			if (s.startsWith("-Dsolr.solr.home")){
				env = s.substring(s.indexOf("=") + 1, s.indexOf("solr_home") - 5);
			}
		}
		// TODO Throw something like ConfigMissingException in case of null

		return env;
	}
}
