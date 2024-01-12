package com.francelabs.datafari.config;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Represents an Atomic Update job. A job uses documents of a first Solr collection to update documents of a second collection
 * in Atomic Update mode. Here are the elements to be configured:
 * <ul>
 *   <li>Solr Collection location ({@link CollectionPathConfig}) for:
 *   <ul>
 *     <li>Source ({@link JobConfig#source}): the collection used to update fields in the destination collection</li>
 *     <li>Destination ({@link JobConfig#destination}): the collection to be updated</li>
 *   </ul>
 *   </li>
 *   <li>Fields operation ({@link JobConfig#fieldsOperation}): a key:value pair for the fields from the Source Collection
 *   that will be used to update the Destination Collection. It is assumed that the two collections have same field name.
 *   If not, a field mapping is specified (see below).
 *   The key is the field name in source collection. The value is the update operation (set, add, remove, ect...)</li>
 *   <li>Fields mapping ({@link JobConfig#fieldsMapping}): a key:value pair for the fields with different names for the 2 collections.
 *   The key is the field name in the source collection. The value is the field name in the destination collection</li>
 *   <li>Number of Solr documents extracted per batch with the Select query on source collection ({@link JobConfig#nbDocsPerBatch}).
 *   (See {@link com.francelabs.datafari.solraccessors.DocumentsCollector DocumentsCollector} for more about extracting strategy)</li>
 * </ul>
 */
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
