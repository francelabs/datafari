package com.francelabs.datafari.simplifiedui.utils;

public class DbJob extends GenericJob {

  private String seedingQ;
  private String versionQ;
  private String accessTokenQ;
  private String dataQ;

  public DbJob() {

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

}
