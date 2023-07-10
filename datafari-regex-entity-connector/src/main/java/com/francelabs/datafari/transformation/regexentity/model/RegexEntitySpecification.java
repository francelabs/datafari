package com.francelabs.datafari.transformation.regexentity.model;

import java.util.Objects;

/**
 * This class contains specifications for the Regex Entity Connector. Each object matches a line in the Entity Regex specifications tab.
 */
public class RegexEntitySpecification {

    String sourceMetadata;
    String regexValue;
    String destinationMetadata;
    String valueIfTrue;
    String valueIfFalse;
    Boolean keepOnlyOne;

    boolean hasMatch;

    public RegexEntitySpecification(String sourceMetadata, String regexValue, String destinationMetadata, String valueIfTrue, String valueIfFalse, Boolean keepOnlyOne) {
        this.sourceMetadata = sourceMetadata;
        this.regexValue = regexValue;
        this.destinationMetadata = destinationMetadata;
        this.valueIfTrue = (valueIfTrue != null) ? valueIfTrue : "";
        this.valueIfFalse = (valueIfFalse != null) ? valueIfFalse : "";
        this.keepOnlyOne = (keepOnlyOne != null) && keepOnlyOne;
        this.hasMatch = false;
    }

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

    public Boolean getKeepOnlyOne() {
        return keepOnlyOne;
    }

    public void setKeepOnlyOne(Boolean keepOnlyOne) {
        this.keepOnlyOne = keepOnlyOne;
    }

    public boolean getHasMatch() {
        return hasMatch;
    }

    public void setHasMatch(boolean hasMatch) {
        this.hasMatch = hasMatch;
    }

    /**
     * The object is valid if the regexValue, sourceMetadata and destinationMetadata fields are not empty
     * @return true if the object is valid
     */
    public boolean isValidObject() {
        return this.regexValue != null && !"".equals(this.regexValue)
                && this.sourceMetadata != null && !"".equals(this.sourceMetadata)
                && this.destinationMetadata != null && !"".equals(this.destinationMetadata);
    }


    /**
     * The object is targeting content if the sourceMetadata field is empty or equals "content"
     * @return true if the object is valid
     */
    public boolean isTargetingContent() {
        return this.regexValue != null && !"".equals(this.regexValue)
                && this.destinationMetadata != null && !"".equals(this.destinationMetadata)
                && (this.sourceMetadata == null || "".equals(this.sourceMetadata)
                || "content".equals(this.sourceMetadata) || "content_en".equals(this.sourceMetadata)
                || "exactContent".equals(this.sourceMetadata) || "content_fr".equals(this.sourceMetadata));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegexEntitySpecification that = (RegexEntitySpecification) o;
        return Objects.equals(sourceMetadata, that.sourceMetadata) && Objects.equals(regexValue, that.regexValue) && Objects.equals(destinationMetadata, that.destinationMetadata) && Objects.equals(valueIfTrue, that.valueIfTrue) && Objects.equals(valueIfFalse, that.valueIfFalse) && Objects.equals(keepOnlyOne, that.keepOnlyOne);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceMetadata, regexValue, destinationMetadata, valueIfTrue, valueIfFalse, keepOnlyOne);
    }
}
