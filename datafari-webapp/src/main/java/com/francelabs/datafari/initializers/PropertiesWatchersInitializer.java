package com.francelabs.datafari.initializers;

import com.francelabs.datafari.config.ConfigManager;
import com.francelabs.datafari.licence.LicenceManagement;

public class PropertiesWatchersInitializer implements IInitializer {

  @Override
  public void initialize() {
    ConfigManager.getInstance();
    LicenceManagement.getInstance();

  }

  @Override
  public void shutdown() {
    ConfigManager.getInstance().stopListeningChanges();
    LicenceManagement.getInstance().stop();

  }
}
