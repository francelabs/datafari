package com.francelabs.datafari.licence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.licence.exception.LicenceException;

public class LicenceManagement {

  private static LicenceManagement instance = null;
  private static Logger LOGGER = LogManager.getLogger(LicenceManagement.class.getName());

  private LicenceManagement() {

  }

  public synchronized static LicenceManagement getInstance() {
    if (instance == null) {
      instance = new LicenceManagement();
    }
    return instance;
  }

  public synchronized void stop() {

  }

  public void removeUser(final String username) throws LicenceException {

  }

}
