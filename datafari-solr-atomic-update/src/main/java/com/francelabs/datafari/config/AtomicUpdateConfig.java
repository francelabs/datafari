package com.francelabs.datafari.config;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Map;

/**
 * Represents the whole configuration of this module, with elements like:
 * <ul>
 *   <li>Log config file location</li>
 *   <li>Atomic Update jobs available and their configuration (see {@link JobConfig})</li>
 * </ul>
 *
 */
public class AtomicUpdateConfig {
  private Map<String, JobConfig> jobs;
  private String logConfigFile;

  @JsonAnyGetter
  public Map<String, JobConfig> getJobs() {
    return jobs;
  }
  @JsonAnySetter
  public void setJobs(Map<String, JobConfig> jobs) {
    this.jobs = jobs;
    for (Map.Entry<String, JobConfig> jobEntry : this.jobs.entrySet()){
      jobEntry.getValue().setJobName(jobEntry.getKey());
    }
  }

  public String getLogConfigFile() {
    return logConfigFile;
  }
  public void setLogConfigFile(String logConfigFile) {
    this.logConfigFile = logConfigFile;
  }


}
