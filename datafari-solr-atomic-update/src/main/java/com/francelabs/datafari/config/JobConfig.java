package com.francelabs.datafari.config;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class JobConfig {
  private CollectionPathConfig source;
  private CollectionPathConfig destination;
  private Map<String, String> fieldsOperation;
  private Map<String, String> fieldsMapping;
  private int nbDocsPerBatch;
  @JsonIgnore
  private String jobName;

  public CollectionPathConfig getSource() {
    return source;
  }

  public void setSource(CollectionPathConfig source) {
    this.source = source;
  }

  public CollectionPathConfig getDestination() {
    return destination;
  }

  public void setDestination(CollectionPathConfig destination) {
    this.destination = destination;
  }

  @JsonAnyGetter
  public Map<String, String> getFieldsOperation() {
    return fieldsOperation;
  }

  @JsonAnySetter
  public void setFieldsOperation(Map<String, String> fieldsOperation) {
    this.fieldsOperation = fieldsOperation;
  }

  @JsonAnyGetter
  public Map<String, String> getFieldsMapping() {
    return fieldsMapping;
  }

  @JsonAnySetter
  public void setFieldsMapping(Map<String, String> fieldsMapping) {
    this.fieldsMapping = fieldsMapping;
  }

  public int getNbDocsPerBatch() {
    return nbDocsPerBatch;
  }

  public void setNbDocsPerBatch(int nbDocsPerBatch) {
    this.nbDocsPerBatch = nbDocsPerBatch;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }
}
