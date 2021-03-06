package com.francelabs.datafari.config;

import java.io.IOException;

public interface IConfigClass {

  public String getProperty(final String key);

  public String getProperty(final String key, final String defaultValue);

  public void setProperty(final String key, final String value);

  public void saveProperties() throws IOException;

  public void onPropertiesReloaded();

}
