package com.francelabs.datafari.utils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timer {


    private static final Logger logger = LogManager.getLogger(Timer.class.getName());
    long startTime;
    long endTime;
    String originClass;
    String originMethod;

    public Timer(String originClass, String originMethod) {
        start();
        this.originMethod = originMethod;
        this.originClass = originClass;
    }

    public void start() {
        startTime = System.currentTimeMillis ();
    }

    public void top(String position) {
        endTime = System.currentTimeMillis ();
        long totalTime = endTime - startTime;
        logger.info("Monitoring class {}. Execution of method {} in progress. Top for position {}: {} ms", originClass, originMethod, position, totalTime);
    }

    public void stop() {
        endTime = System.currentTimeMillis ();
        long totalTime = endTime - startTime;
        logger.info("Monitoring class {}. Execution of method {} took {} ms", originClass, originMethod, totalTime);
    }
}
