/*******************************************************************************
 * Copyright 2019 France Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
