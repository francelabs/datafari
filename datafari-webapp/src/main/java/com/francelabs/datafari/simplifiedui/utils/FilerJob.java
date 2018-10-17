package com.francelabs.datafari.simplifiedui.utils;

public class FilerJob {

  private String repositoryConnection;
  private String paths;
  private boolean security = false;

  public FilerJob() {

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

}
