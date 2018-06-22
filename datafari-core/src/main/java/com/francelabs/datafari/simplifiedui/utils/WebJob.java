package com.francelabs.datafari.simplifiedui.utils;

public class WebJob {

  private String repositoryConnection;
  private String seeds;
  private String exclusions;

  public WebJob() {

  }

  public String getRepositoryConnection() {
    return repositoryConnection;
  }

  public void setRepositoryConnection(final String repositoryConnection) {
    this.repositoryConnection = repositoryConnection;
  }

  public String getSeeds() {
    return seeds;
  }

  public void setSeeds(final String seeds) {
    this.seeds = seeds;
  }

  public String getExclusions() {
    return exclusions;
  }

  public void setExclusions(final String exclusions) {
    this.exclusions = exclusions;
  }

}
