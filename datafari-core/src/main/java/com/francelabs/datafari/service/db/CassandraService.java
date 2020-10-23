package com.francelabs.datafari.service.db;

import com.datastax.oss.driver.api.core.CqlSession;

public abstract class CassandraService {

  protected CqlSession session;

  protected void refreshSession() {
    if (session == null || session.isClosed()) {
      session = CassandraManager.getInstance().getSession();
    }
  }

}
