package com.francelabs.datafari.save;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * This class is used to store and retrieve job status in the "atomicUpdateLastExec" file.
 * The term "status" here covers information such as job running, done or failed, as well as the date of the last successful run.
 */
public class JobSaver {
  private static final String FILENAME = "./atomicUpdateLastExec";
  private static final Properties jobsInfo = new Properties();
  private static final Logger LOGGER = LoggerFactory.getLogger(JobSaver.class);
  private static final String STATUS_KEY = ".STATUS";
  private static final String LAST_EXEC_KEY = ".LAST_EXEC";
  private String currentJobName;
  private Date startedDate;

  static {
    try {
      jobsInfo.load(new FileInputStream(FILENAME));
    } catch (IOException e) {
      LOGGER.info("The Last execution file doesn't exists yet. It will be created by the first Atomic Update Job execution.");
    }
  }

  public static String getExecutionDate(String jobName){
    String fromDate = jobsInfo.getProperty(jobName+LAST_EXEC_KEY);

    if (StringUtils.isBlank(fromDate)){
      fromDate = null;
    }
    return fromDate;
  }

  public JobSaver(String jobName){
    this.currentJobName = jobName;
  }
  public void notifyJobRunning(){
    startedDate = new Date();
    writeStatus(Status.RUNNING);
  }

  public void notifyJobDone(){
    writeStatus(Status.DONE);
    writeExecutionDate();
  }

  public void notifyJobFailed(){
    writeStatus(Status.FAILED);
  }

  public Status getJobLastStatus(){
    String status = jobsInfo.getProperty(currentJobName+STATUS_KEY);
    if (status == null){
      return Status.DONE;
    }
    return Status.valueOf(status);
  }
  private void writeStatus(Status status){
    writeProperty(currentJobName+STATUS_KEY, status.toString());
    LOGGER.info("Job " + currentJobName + " " + status);
  }
  private void writeExecutionDate(){
    writeProperty(currentJobName+LAST_EXEC_KEY, startedDate.toInstant().toString());
  }

  private void writeProperty(String propKey, String propValue){
    jobsInfo.setProperty(propKey, propValue);
    try {
      jobsInfo.store(new FileWriter(FILENAME), "");
    } catch (IOException e) {
      LOGGER.error("Error trying to write to: " + FILENAME + " file.");
      throw new RuntimeException(e);
    }

  }
}
