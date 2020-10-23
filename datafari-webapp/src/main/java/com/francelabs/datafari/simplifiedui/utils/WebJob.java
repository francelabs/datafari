package com.francelabs.datafari.simplifiedui.utils;

public class WebJob {

  private String repositoryConnection;
  private String seeds;
  private String sourcename;

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

  public String getSourcename() {
    return sourcename;
  }

  public void setSourcename(final String sourcename) {
    this.sourcename = sourcename;
  }

}
