package com.francelabs.datafari.simplifiedui.utils;

public abstract class GenericJob {

  private String repositoryConnection;
  private String sourcename;
  private boolean security = false;
  private boolean duplicatesDetection = false;
  private boolean createOCR = false;
  private String tikaOCRHost;
  private String tikaOCRPort;
  private String tikaOCRName;

  public GenericJob() {

  }

  public String getRepositoryConnection() {
    return repositoryConnection;
  }

  public void setRepositoryConnection(final String repositoryConnection) {
    this.repositoryConnection = repositoryConnection;
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

  public String getTikaOCRHost() {
    return tikaOCRHost;
  }

  public void setTikaOCRHost(final String tikaOCRHost) {
    this.tikaOCRHost = tikaOCRHost;
  }

  public String getTikaOCRPort() {
    return tikaOCRPort;
  }

  public void setTikaOCRPort(final String tikaOCRPort) {
    this.tikaOCRPort = tikaOCRPort;
  }

  public String getTikaOCRName() {
    return tikaOCRName;
  }

  public void setTikaOCRName(final String tikaOCRName) {
    this.tikaOCRName = tikaOCRName;
  }

  public void setCreateOCR(final boolean createOCR) {
    this.createOCR = createOCR;
  }

  public boolean isOCREnabled() {
    return createOCR;
  }

}
