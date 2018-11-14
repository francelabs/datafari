package com.francelabs.datafari.simplifiedui.utils;

import java.util.ArrayList;
import java.util.List;

import com.francelabs.datafari.simplifiedui.utils.FilerFilterRule.FilterType;
import com.francelabs.datafari.simplifiedui.utils.FilerFilterRule.RuleType;

public class FilerJob {

  private String repositoryConnection;
  private String paths;
  private boolean security = false;
  private final List<FilerFilterRule> orderedFilterRules = new ArrayList<>();

  public FilerJob() {
    // Set default exclude rules
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "/thumbs.db"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "/desktop.ini"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "/~*"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "*.lnk"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "*.mat"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "*.odb"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "*.zip"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "*.gz"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "*.rar"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "*.bz2"));
    orderedFilterRules.add(new FilerFilterRule(RuleType.EXCLUDE, FilterType.FILE, "*.7z"));
  }

  public String getRepositoryConnection() {
    return repositoryConnection;
  }

  public void setRepositoryConnection(final String repositoryConnection) {
    this.repositoryConnection = repositoryConnection;
  }

  public String getPaths() {
    return paths;
  }

  public void setPaths(final String paths) {
    this.paths = paths;
  }

  public boolean isSecurity() {
    return security;
  }

  public void setSecurity(final boolean security) {
    this.security = security;
  }

  public List<FilerFilterRule> getOrderedRules() {
    return orderedFilterRules;
  }

}
