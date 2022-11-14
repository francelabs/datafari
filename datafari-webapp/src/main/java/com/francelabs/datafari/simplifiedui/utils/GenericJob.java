package com.francelabs.datafari.simplifiedui.utils;

public abstract class GenericJob {

  private String repositoryConnection;
  private String sourcename;
  private String timezone;
  private boolean security = false;
  private boolean duplicatesDetection = false;
  private boolean createOCR = false;
  private String tikaOCRHost;
  private String tikaOCRPort;
  private String tikaOCRName;
  private boolean createSpacy = false;
  private String spacyConnectorName;
  private String spacyServerAddress;
  private String spacyModelToUse;
  private String spacyEndpointToUse;
  private String spacyOutputFieldPrefix;

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

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(final String timezone) {
    this.timezone = timezone;
  }

  public String getSpacyConnectorName() {
    return spacyConnectorName;
  }

  public void setSpacyConnectorName(final String spacyConnectorName) {
    this.spacyConnectorName = spacyConnectorName;
  }

  public String getSpacyServerAddress() {
    return spacyServerAddress;
  }

  public void setSpacyServerAddress(final String spacyServerAddress) {
    this.spacyServerAddress = spacyServerAddress;
  }

  public String getSpacyModelToUse() {
    return spacyModelToUse;
  }

  public void setSpacyModelToUse(final String spacyModelToUse) {
    this.spacyModelToUse = spacyModelToUse;
  }

  public String getSpacyEndpointToUse() {
    return spacyEndpointToUse;
  }

  public void setSpacyEndpointToUse(final String spacyEndpointToUse) {
    this.spacyEndpointToUse = spacyEndpointToUse;
  }

  public String getSpacyOutputFieldPrefix() {
    return spacyOutputFieldPrefix;
  }

  public void setSpacyOutputFieldPrefix(final String spacyOutputFieldPrefix) {
    this.spacyOutputFieldPrefix = spacyOutputFieldPrefix;
  }

  public void setCreateSpacy(final boolean createSpacy) {
    this.createSpacy = createSpacy;
  }

  public boolean isSpacyEnabled() {
    return createSpacy;
  }

}
