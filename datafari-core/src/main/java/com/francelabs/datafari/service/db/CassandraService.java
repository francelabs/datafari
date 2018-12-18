package com.francelabs.datafari.service.db;

import com.datastax.driver.core.Session;

public abstract class CassandraService {

  protected Session session;

  protected void refreshSession() {
    if (session == null || session.isClosed()) {
      session = CassandraManager.getInstance().getSession();
    }
  }

}
