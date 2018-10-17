package com.francelabs.datafari.utils;



public final class ExecutionEnvironment {

	private ExecutionEnvironment() {
	}

	public static String getDevExecutionEnvironment() {	
		//no more specific dev environmenent, return default value
		return "/opt/datafari";
	}
}
