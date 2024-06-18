package com.francelabs.datafari.updateprocessor.regexentity;

import com.opencsv.bean.CsvBindByPosition;

import java.util.Objects;

public class RegexEntitySpecification {
  @CsvBindByPosition(position = 0, required = true)
  private String sourceMetadata;
  @CsvBindByPosition(position = 1)
  private String regexValue;
  @CsvBindByPosition(position = 2, required = true)
  private String destinationMetadata;
  @CsvBindByPosition(position = 3)
  private String valueIfTrue;
  @CsvBindByPosition(position = 4)
  private String valueIfFalse;
  @CsvBindByPosition(position = 5)
  private boolean regexActive;


  public String getSourceMetadata() {
    return sourceMetadata;
  }

  public void setSourceMetadata(String sourceMetadata) {
    this.sourceMetadata = sourceMetadata;
  }

  public String getRegexValue() {
    return regexValue;
  }

  public void setRegexValue(String regexValue) {
    this.regexValue = regexValue;
  }

  public String getDestinationMetadata() {
    return destinationMetadata;
  }

  public void setDestinationMetadata(String destinationMetadata) {
    this.destinationMetadata = destinationMetadata;
  }

  public String getValueIfTrue() {
    return valueIfTrue;
  }

  public void setValueIfTrue(String valueIfTrue) {
    this.valueIfTrue = valueIfTrue;
  }

  public String getValueIfFalse() {
    return valueIfFalse;
  }

  public void setValueIfFalse(String valueIfFalse) {
    this.valueIfFalse = valueIfFalse;
  }

  public boolean isRegexActive() {
    return regexActive;
  }

  public void setRegexActive(boolean regexActive) {
    this.regexActive = regexActive;
  }

  public RegexEntitySpecification() {}


  @Override
  public String toString() {
    return "RegexEntitySpecification{" +
        "sourceMetadata='" + sourceMetadata + '\'' +
        ", regexValue='" + regexValue + '\'' +
        ", destinationMetadata='" + destinationMetadata + '\'' +
        ", valueIfTrue='" + valueIfTrue + '\'' +
        ", valueIfFalse='" + valueIfFalse + '\'' +
        ", regexActive=" + regexActive +
        '}';
  }
}
