package com.francelabs.datafari.utils;

public class Environment {
    public static String getEnvironmentVariable(String variable) {
        return System.getenv(variable); 
    }
    
    public static String getProperty(String key){
    	return System.getProperty(key);
    }
}
