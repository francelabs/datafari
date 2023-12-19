package com.francelabs.datafari.save;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class JobSaver {
  private static final String FILENAME = "./atomicUpdateLastExec";
  private static final Properties jobsInfo = new Properties();
  private final static Logger LOGGER = LogManager.getLogger(JobSaver.class.getName());

  static {
    try {
      jobsInfo.load(new FileInputStream(FILENAME));
    } catch (IOException e) {
      LOGGER.info("The Last execution file doesn't exists yet. It will be created by the first Atomic Update Job execution.");
    }
  }
  public static void writeExecutionDate(String jobName, Date lastStartExecDate){
    jobsInfo.setProperty(jobName, lastStartExecDate.toInstant().toString());
    try {
      jobsInfo.store(new FileWriter(FILENAME), "Last execution date of Atomic Update Jobs");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getExecutionDate(String jobName){
    return jobsInfo.getProperty(jobName);
  }
}
