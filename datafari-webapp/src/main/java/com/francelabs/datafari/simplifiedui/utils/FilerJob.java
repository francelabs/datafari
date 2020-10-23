package com.francelabs.datafari.simplifiedui.utils;

public class FilerJob {

  private String repositoryConnection;
  private String paths;
  private String sourcename;
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

  public String getSourcename() {
    return sourcename;
  }

  public void setSourcename(final String sourcename) {
    this.sourcename = sourcename;
  }

  public boolean isSecurity() {
    return security;
  }

  public void setSecurity(final boolean security) {
    this.security = security;
  }

}
