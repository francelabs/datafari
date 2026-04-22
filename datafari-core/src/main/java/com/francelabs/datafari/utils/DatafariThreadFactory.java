package com.francelabs.datafari.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DatafariThreadFactory implements ThreadFactory {
  private static final Logger thisLogger = LogManager.getLogger(DatafariThreadFactory.class);

  private final Logger callerLogger;

  private final String prefix;
  private final boolean daemon;
  private final AtomicInteger count = new AtomicInteger(1);
  private final ThreadFactory delegate = java.util.concurrent.Executors.defaultThreadFactory();

  public DatafariThreadFactory(String prefix, Logger logger) {
    this(prefix, false, logger);
  }
  public DatafariThreadFactory(String prefix, boolean daemon, Logger logger) {
    this.prefix = prefix;
    this.daemon = daemon;
    if (logger != null) {
      this.callerLogger = logger;
    } else {
      this.callerLogger = thisLogger;
    }
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = delegate.newThread(r);
    t.setName(prefix + "-" + count.getAndIncrement());
    t.setDaemon(daemon);

    callerLogger.debug("Thread created [{}]", t.getName());

    return t;
  }
}