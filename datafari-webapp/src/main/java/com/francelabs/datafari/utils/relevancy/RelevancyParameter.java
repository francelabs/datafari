package com.francelabs.datafari.utils.relevancy;

import org.json.simple.JSONObject;

public class RelevancyParameter {
  
  private static final String ID_KEY = "id";
  private static final String NAME_KEY = "name";
  private static final String TYPE_KEY = "type";
  private static final String MIN_KEY = "start";
  private static final String MAX_KEY = "end";
  
  private static final int DEFAULT_MAX = 50;
  private static final int DEFAULT_MIN = 0;

  private final JSONObject parameter;
  
  public RelevancyParameter(final int id, final String paramName, final String paramType) {
    parameter = new JSONObject();
    parameter.put(ID_KEY, id);
    parameter.put(NAME_KEY, paramName);
    parameter.put(TYPE_KEY, paramType);
    parameter.put(MIN_KEY, DEFAULT_MIN);
    parameter.put(MAX_KEY, DEFAULT_MAX);
  }
  
  public RelevancyParameter(JSONObject parameter) {
    // FIXME: Need a stronger verification to allow the object creation.
    if (((Number) parameter.get(ID_KEY)).intValue() != Integer.MAX_VALUE &&
        (String) parameter.get(NAME_KEY) != null &&
        (String) parameter.get(TYPE_KEY) != null &&
        ((Number) parameter.get(MIN_KEY)).intValue() != Integer.MAX_VALUE &&
        ((Number) parameter.get(MAX_KEY)).intValue() != Integer.MAX_VALUE) {
      this.parameter = parameter;
      // get rid of string object that may still be in the underlying json object
      // and convert them into the expected types.
      this.parameter.put(ID_KEY, this.getId());
      this.setMin(this.getMin());
      int max = this.getMax();
      this.setMax(this.getMin()+1);
      this.setMax(max);
      return;
    }
    throw new RuntimeException("The given JSON object does not represent a proper relevancy parameter.");
  }
  
  public int getId() {
    return ((Number) this.parameter.get(ID_KEY)).intValue();
  }
  
  public String getName() {
    return (String) this.parameter.get(NAME_KEY);
  }
  
  public String getType() {
    return (String) this.parameter.get(TYPE_KEY);
  }

  public int getMin() {
	  return ((Number) this.parameter.get(MIN_KEY)).intValue();
  }
  
  public int getMax() {
	  return ((Number) this.parameter.get(MAX_KEY)).intValue();
  }
  
  public void setType(String type) {
    this.parameter.put(TYPE_KEY, type);
  }
  
  public void setName(String name) {
    this.parameter.put(NAME_KEY, name);
  }
  
  public void setMin(int min) {
    if (min < this.getMax()) {
      this.parameter.put(MIN_KEY, min);
    }
  }
  
  public void setMax(int max) {
    if (max > this.getMin()) {
      this.parameter.put(MAX_KEY, max);
    }
   }
  
  public JSONObject toJSON() {
    return this.parameter;
  }
  
  public String toJSONString() {
	  return this.toJSON().toJSONString();
  }

  @Override
  public String toString() {
    return this.toJSONString();
  }

}
