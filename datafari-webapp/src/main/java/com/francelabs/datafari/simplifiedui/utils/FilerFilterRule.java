package com.francelabs.datafari.simplifiedui.utils;

public class FilerFilterRule {

  private final String ruleType;
  private final String filterType;
  private final String filterValue;

  public FilerFilterRule(final RuleType ruleType, final FilterType filterType, final String filter) {
    this.ruleType = ruleType.toString();
    this.filterType = filterType.toString();
    this.filterValue = filter;
  }

  public String getRuleType() {
    return ruleType;
  }

  public String getFilterType() {
    return filterType;
  }

  public String getFilterValue() {
    return filterValue;
  }

  public static enum RuleType {
    INCLUDE("include"), EXCLUDE("exclude");

    private final String name;

    private RuleType(final String s) {
      name = s;
    }

    public boolean equalsName(final String otherName) {
      // (otherName == null) check is not needed because name.equals(null) returns
      // false
      return name.equals(otherName);
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public static enum FilterType {
    FILE("file"), DIRECTORY("directory");

    private final String name;

    private FilterType(final String s) {
      name = s;
    }

    public boolean equalsName(final String otherName) {
      // (otherName == null) check is not needed because name.equals(null) returns
      // false
      return name.equals(otherName);
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

}
