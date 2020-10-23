package com.francelabs.datafari.initializers;

import com.francelabs.datafari.monitoring.IndexMonitoring;

public class IndexMonitoringInitializer implements IInitializer {

  @Override
  public void initialize() {
    IndexMonitoring.getInstance().startIndexMonitoring();

  }

  @Override
  public void shutdown() {
    IndexMonitoring.getInstance().stopIndexMonitoring();

  }

}
