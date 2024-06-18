package com.francelabs.datafari.simplifiedui.utils;

public class FilerJob extends GenericJob {

  private String paths;

  private String mode;

  public FilerJob() {

  }

  public String getPaths() {
    return paths;
  }

  public void setPaths(final String paths) {
    this.paths = paths;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

}
