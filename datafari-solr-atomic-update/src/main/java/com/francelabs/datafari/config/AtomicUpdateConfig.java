package com.francelabs.datafari.config;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Map;

public class AtomicUpdateConfig {
  private Map<String, JobConfig> jobs;

  @JsonAnyGetter
  public Map<String, JobConfig> getJobs() {
    return jobs;
  }
  @JsonAnySetter
  public void setJobs(Map<String, JobConfig> jobs) {
    this.jobs = jobs;
  }
}
