package com.francelabs.datafari.simplifiedui.utils;

public class WebJob extends GenericJob {

  private String seeds;
  private String mode;

  public WebJob() {

  }

  public String getSeeds() {
    return seeds;
  }

  public void setSeeds(final String seeds) {
    this.seeds = seeds;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }
}
