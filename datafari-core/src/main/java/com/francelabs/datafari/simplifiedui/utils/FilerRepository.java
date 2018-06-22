package com.francelabs.datafari.simplifiedui.utils;

import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.system.ManifoldCF;

public class FilerRepository {

  private String server;
  private String user;
  private String password;

  public FilerRepository() {

  }

  public String getServer() {
    return server;
  }

  public void setServer(final String server) {
    this.server = server;
  }

  public String getUser() {
    return user;
  }

  public void setUser(final String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    try {
      this.password = ManifoldCF.obfuscate(password);
    } catch (final ManifoldCFException e) {
      this.password = password;
    }
  }

}
