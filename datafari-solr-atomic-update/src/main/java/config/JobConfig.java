package config;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Map;

public class JobConfig {
  private CollectionPathConfig source;
  private CollectionPathConfig destination;
  private Map<String, String> fieldsOperation;

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
}
