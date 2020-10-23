package com.francelabs.datafari.utils.relevancy;

import org.json.simple.JSONObject;

public class RelevancyFixedParameter {
  
  private static final String ID_KEY = "id";
  private static final String NAME_KEY = "name";
  private static final String TYPE_KEY = "type";
  private static final String VALUE_KEY = "value";
  
  private static final int DEFAULT_VALUE = 1;

  private final JSONObject parameter;
  
  public RelevancyFixedParameter(final int id, final String paramName, final String paramType) {
    parameter = new JSONObject();
    parameter.put(ID_KEY, id);
    parameter.put(NAME_KEY, paramName);
    parameter.put(TYPE_KEY, paramType);
    parameter.put(VALUE_KEY, DEFAULT_VALUE);
  }
  
  public RelevancyFixedParameter(JSONObject parameter) {
    // FIXME: Need a stronger verification to allow the object creation.
    if (((Number) parameter.get(ID_KEY)).intValue() != Integer.MAX_VALUE &&
        (String) parameter.get(NAME_KEY) != null &&
        (String) parameter.get(TYPE_KEY) != null &&
        ((Number) parameter.get(VALUE_KEY)).intValue() != Integer.MAX_VALUE) {
      this.parameter = parameter;
      // get rid of string object that may still be in the underlying json object
      // and convert them into the expected types.
      this.parameter.put(ID_KEY, this.getId());
      this.setValue(this.getValue());
      return;
    }
    throw new RuntimeException("The given JSON object does not represent a proper relevancy parameter.");
  }
  
  public int getId() {
    return ((Number) this.parameter.get(ID_KEY)).intValue();
  }

  public int getValue() {
	  return ((Number) this.parameter.get(VALUE_KEY)).intValue();
  }
  
  public void setValue(int value) {
    this.parameter.put(VALUE_KEY, value);
  }

  public String getName() {
    return (String) this.parameter.get(NAME_KEY);
  }
  
  public void setName(String name) {
    this.parameter.put(NAME_KEY, name);
  }
  
  public String getType() {
	  return (String) this.parameter.get(TYPE_KEY);
  }
  
  public void setType(String type) {
    this.parameter.put(TYPE_KEY, type);
  }
  
  public JSONObject toJSON() {
    return this.parameter;
  }
  
  public String toJSONString() {
	  return this.toJSON().toString();
  }

  @Override
  public String toString() {
    return this.toJSONString();
  }

}
