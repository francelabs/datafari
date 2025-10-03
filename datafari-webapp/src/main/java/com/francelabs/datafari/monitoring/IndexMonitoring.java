package com.francelabs.datafari.monitoring;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.francelabs.datafari.logs.MonitoringLevel;
import com.francelabs.datafari.service.indexer.IndexerFacetField;
import com.francelabs.datafari.service.indexer.IndexerFacetFieldCount;
import com.francelabs.datafari.service.indexer.IndexerQuery;
import com.francelabs.datafari.service.indexer.IndexerQueryResponse;
import com.francelabs.datafari.service.indexer.IndexerServer;
import com.francelabs.datafari.service.indexer.IndexerServerManager;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;

/**
 * Starts monitoring events which occurs at a fixed rate (once an hour by default).
 */
public class IndexMonitoring {

  /** Singleton instance. */
  private static IndexMonitoring instance;

  /** Scheduler for periodic task. */
  private volatile ScheduledExecutorService scheduler;

  /** Guard to make start/stop idempotent. */
  private volatile boolean started = false;

  /** Selected monitoring frequency unit. */
  private final MonitoringLogFrequencyUnit selectedMonitoringLogFrequencyUnit;

  /** Frequency value. */
  private final int frequencyValue;

  /** Monitoring Log Frequency Enum. */
  public static enum MonitoringLogFrequencyUnit {
    MINUTE, HOUR, DAY
  }

  /** Monitoring Event Frequency Enum. */
  public static enum MonitoringEventFrequency {
    MINUTELY, HOURLY, DAILY
  }

  private IndexMonitoring() {
    // Default: run every hour
    this.selectedMonitoringLogFrequencyUnit = MonitoringLogFrequencyUnit.HOUR;
    this.frequencyValue = 1;
  }

  /** Thread-safe singleton getter. */
  public static synchronized IndexMonitoring getInstance() {
    if (instance == null) {
      instance = new IndexMonitoring();
    }
    return instance;
  }

  /**
   * Start monitoring if not already running.
   * Idempotent: calling it multiple times won’t schedule duplicates.
   */
  public synchronized void startIndexMonitoring() {
    if (started && scheduler != null && !scheduler.isShutdown() && !scheduler.isTerminated()) {
      // Already running; do nothing
      return;
    }
    // (Re)create scheduler
    scheduler = Executors.newScheduledThreadPool(1);
    final FileShareMonitoringLog task = new FileShareMonitoringLog();

    long initialDelay = millisecondsBetweenNextEvent();
    if (initialDelay < 0) {
      // Safety: ensure non-negative initial delay
      initialDelay = 0L;
    }
    scheduler.scheduleAtFixedRate(task, initialDelay, getPeriod(), TimeUnit.MILLISECONDS);
    started = true;
  }

  /**
   * Stop monitoring if running.
   * Idempotent and null-safe.
   */
  public synchronized void stopIndexMonitoring() {
    started = false; // mark as not started first
    try {
      if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdownNow();
      }
    } catch (Exception e) {
      // Log and swallow: shutdown must be robust
      LogManager.getLogger(IndexMonitoring.class).warn("stopIndexMonitoring ignored error", e);
    } finally {
      scheduler = null; // ensure GC and future starts create a fresh scheduler
    }
  }

  /**
   * Calculates the period according to the selected monitoring frequency unit and value.
   * @return period in milliseconds
   */
  private long getPeriod() {
    long multiplicator;
    switch (selectedMonitoringLogFrequencyUnit) {
      case HOUR:
        multiplicator = 60 * 60;
        break;
      case MINUTE:
        multiplicator = 60;
        break;
      case DAY:
        multiplicator = 60 * 60 * 24;
        break;
      default:
        multiplicator = 60 * 60;
        break;
    }
    return 1000L * multiplicator * frequencyValue;
  }

  /**
   * Calculates the number of milliseconds between now and the next monitoring tick.
   * @return non-negative delay in milliseconds
   */
  private long millisecondsBetweenNextEvent() {
    final Calendar nextMinute = new GregorianCalendar();
    nextMinute.add(Calendar.MINUTE, frequencyValue);
    nextMinute.set(Calendar.SECOND, 0);
    nextMinute.set(Calendar.MILLISECOND, 0);
    long diff = nextMinute.getTimeInMillis() - System.currentTimeMillis();
    return Math.max(diff, 0L);
  }

  /**
   * Periodic task that logs FileShare Solr Core stats.
   */
  private final class FileShareMonitoringLog implements Runnable {

    private final Logger LOGGER = LogManager.getLogger(FileShareMonitoringLog.class);

    /** facet fields to add to the query. */
    private final List<String> facetFields = new ArrayList<>(Arrays.asList("extension", "language", "repo_source"));

    private final MonitoringEventFrequency eventFrequency;

    public FileShareMonitoringLog() {
      this.eventFrequency = MonitoringEventFrequency.DAILY;
    }

    @Override
    public void run() {
      try {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        final String currentDate = df.format(new Date());

        final IndexerServer server = IndexerServerManager.getIndexerServer(Core.FILESHARE);
        if (server == null) {
          // Defensive: if not available yet, just skip this tick
          LOGGER.debug("IndexerServer FILESHARE not available yet; skipping monitoring tick.");
          return;
        }

        final IndexerQuery query = IndexerServerManager.createQuery();
        query.setQuery("*:*");
        query.setRequestHandler("/opensearch");
        for (final String facetField : facetFields) {
          query.addFacetField(facetField);
        }

        final IndexerQueryResponse queryResponse = server.executeQuery(query);

        // Global num docs log
        String log = generateID("no", "no") + "|" + currentDate + "|" + queryResponse.getNumFound() + "|no|no";
        LOGGER.log(MonitoringLevel.MONITORING, log);

        // Facet logs
        for (final IndexerFacetField facetField : queryResponse.getFacetFields().values()) {
          String facetType = facetField.getName();
          if ("repo_source".equals(facetType)) {
            facetType = "source";
          }
          for (final IndexerFacetFieldCount facet : facetField.getValues()) {
            final String facetValue = facet.getName();
            final String id = generateID(facetValue, facetType);
            log = id + "|" + currentDate + "|" + facet.getCount() + "|" + facetValue + "|" + facetType;
            LOGGER.log(MonitoringLevel.MONITORING, log);
          }
        }

      } catch (final Exception e) {
        // Keep task resilient: never let an exception kill the scheduler thread
        LOGGER.error("Unable to retrieve Index infos", e);
      }
    }

    /**
     * Generates a unique ID per (event window, facet value).
     */
    private String generateID(final String facetValue, final String facetType) throws NoSuchAlgorithmException {
      final Calendar currentDate = new GregorianCalendar();
      if (eventFrequency != MonitoringEventFrequency.HOURLY && eventFrequency != MonitoringEventFrequency.MINUTELY) {
        currentDate.set(Calendar.HOUR, 0);
      }
      if (eventFrequency != MonitoringEventFrequency.MINUTELY) {
        currentDate.set(Calendar.MINUTE, 0);
      }
      currentDate.set(Calendar.SECOND, 0);
      currentDate.set(Calendar.MILLISECOND, 0);
      currentDate.set(Calendar.HOUR_OF_DAY, 0);
      currentDate.set(Calendar.AM_PM, Calendar.AM);

      final String strID = String.valueOf(currentDate.getTimeInMillis()) + facetValue + facetType;
      final MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(strID.getBytes());
      final byte[] digest = md.digest();
      final StringBuilder sb = new StringBuilder();
      for (final byte b : digest) {
        sb.append(String.format("%02x", b & 0xff));
      }
      return sb.toString();
    }
  }
}