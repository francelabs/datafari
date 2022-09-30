package com.francelabs.datafari.simplifiedui.utils;

public class DbJob {

  private String repositoryConnection;
  private String seedingQ;
  private String versionQ;
  private String accessTokenQ;
  private String dataQ;
  private String sourcename;
  private boolean security = false;
  private boolean duplicatesDetection = false;

  public DbJob() {

  }

  public String getRepositoryConnection() {
    return repositoryConnection;
  }

  public void setRepositoryConnection(final String repositoryConnection) {
    this.repositoryConnection = repositoryConnection;
  }

  public String getSeedingQ() {
    return seedingQ;
  }

  public void setSeedingQ(final String seedingQ) {
    this.seedingQ = seedingQ;
  }

  public String getVersionQ() {
    return versionQ;
  }

  public void setVersionQ(final String versionQ) {
    this.versionQ = versionQ;
  }

  public String getAccessTokenQ() {
    return accessTokenQ;
  }

  public void setAccessTokenQ(final String accessTokenQ) {
    this.accessTokenQ = accessTokenQ;
  }

  public String getDataQ() {
    return dataQ;
  }

  public void setDataQ(final String dataQ) {
    this.dataQ = dataQ;
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

  public boolean isDuplicatesDetectionEnabled() {
    return duplicatesDetection;
  }

  public void setDuplicatesDetection(final boolean duplicatesDetection) {
    this.duplicatesDetection = duplicatesDetection;
  }

}
