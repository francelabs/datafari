package com.francelabs.datafari.simplifiedui.utils;

public abstract class GenericRepository {

  private String reponame;

  public GenericRepository() {

  }

  public String getReponame() {
    return reponame;
  }

  public void setReponame(final String reponame) {
    this.reponame = reponame;
  }

}
