package com.francelabs.datafari.simplifiedui.utils;

import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.system.ManifoldCF;

public class DbRepository extends GenericRepository {

  private String type;
  private String name;
  private String host;
  private String connectionStr;
  private String user;
  private String password;

  public DbRepository() {

  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getDbName() {
    return name;
  }

  public void setDbName(final String name) {
    this.name = name;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public String getConnectionStr() {
    return connectionStr;
  }

  public void setConnectionStr(final String connectionStr) {
    this.connectionStr = connectionStr;
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
