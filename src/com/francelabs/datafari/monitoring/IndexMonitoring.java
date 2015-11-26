package com.francelabs.datafari.monitoring;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import com.francelabs.datafari.logs.MonitoringLevel;
import com.francelabs.datafari.service.search.SolrServers;
import com.francelabs.datafari.service.search.SolrServers.Core;

/**
 * Starts monitoring events which occurs at a fixed rate (once an hour by
 * default, but can be set in the admin UI of datafari //TODO)
 *
 */
public class IndexMonitoring {

	/** The instance. */
	private static IndexMonitoring instance;
	/** The scheduler. */
	private ScheduledExecutorService scheduler;
	/** The selected monitoring frequency unit. */
	private final MonitoringLogFrequencyUnit selectedMonitoringLogFrequencyUnit;
	/** The frequency value. */
	private final int frequencyValue;

	/** Monitoring Log Frequency Enum. */
	public static enum MonitoringLogFrequencyUnit {
		MINUTE, HOUR, DAY
	}

	/** Monitoring Event Frequency Enum. */
	public static enum MonitoringEventFrequency {
		MINUTELY, HOURLY, DAILY
	}

	/**
	 * Constructor
	 */
	private IndexMonitoring() {
		selectedMonitoringLogFrequencyUnit = MonitoringLogFrequencyUnit.HOUR;
		frequencyValue = 1;
	}

	/**
	 * Singleton
	 *
	 * @return the instance
	 */
	public static IndexMonitoring getInstance() {
		if (instance == null) {
			instance = new IndexMonitoring();
		}
		return instance;
	}

	/**
	 * Starts the monitoring and periodically generates logs
	 */
	public void startIndexMonitoring() {
		scheduler = Executors.newScheduledThreadPool(1);
		final FileShareMonitoringLog logger = new FileShareMonitoringLog();
		scheduler.scheduleAtFixedRate(logger, millisecondsBetweenNextEvent(), getPeriod(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Stops the monitoring process
	 */
	public void stopIndexMonitoring() {
		scheduler.shutdownNow();
	}

	/**
	 * Calculates the period according to the selected monitoring frequency unit
	 * and value
	 *
	 * @return the calculated period in milliseconds
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
		return 1000 * multiplicator * frequencyValue;
	}

	/**
	 * Calculates the number of milliseconds between the current date time and
	 * the date time of the next monitoring event
	 *
	 * @return the number of milliseconds
	 */
	private long millisecondsBetweenNextEvent() {
		final Calendar nextMinute = new GregorianCalendar();
		nextMinute.add(Calendar.MINUTE, frequencyValue);
		nextMinute.set(Calendar.SECOND, 0);
		nextMinute.set(Calendar.MILLISECOND, 0);
		final long diffInMilliseconds = nextMinute.getTimeInMillis() - new GregorianCalendar().getTimeInMillis();
		return diffInMilliseconds;
	}

	/**
	 * Thread that will log the state of the FileShare Solr Core
	 * <p>
	 * The goal is to have the number of documents indexed and how those
	 * documents are distributed. For this purpose, this thread will query the
	 * FileShare core with some facet fields like for example "extension",
	 * "language" or "source" The query response is then analyzed to extract the
	 * global number of docs and split the number of docs per facet. The results
	 * are then formated into logs (one log per facet value) like this:<br>
	 * [id]|[number_of_docs]|[facet_value]|[facet_type]
	 * <p>
	 * For example, if the FileShare core contains 28 documents, 14 of witch are
	 * in french, and 14 of witch are in english, and the "language" facet field
	 * is added to the query, the following logs will be generated:<br>
	 * Aefs646|28|no|no G5sfP|14|fr|language MtuL78|14|en|language
	 * <p>
	 * The log with the total number of documents is always generated. For this
	 * log, the facet value and the facet type are both set to "no" (like shown
	 * above)
	 *
	 */
	private final class FileShareMonitoringLog implements Runnable {

		/** The LOGGER. */
		private final Logger LOGGER = Logger.getLogger(FileShareMonitoringLog.class);

		/** facet fields to add to the query. */
		private final List<String> facetFields = new ArrayList<String>(Arrays.asList("extension", "language", "source"));

		private final MonitoringEventFrequency eventFrequency;

		/**
		 * Constructor
		 */
		public FileShareMonitoringLog() {
			this.eventFrequency = MonitoringEventFrequency.DAILY;
		}

		/**
		 * Constructor
		 *
		 * @param eventFrequency
		 */
		public FileShareMonitoringLog(final MonitoringEventFrequency eventFrequency) {
			this.eventFrequency = eventFrequency;
		}

		@Override
		public void run() {
			try {
				// Create an instance of SimpleDateFormat used for
				// formatting
				// the string representation of date
				final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
				final String currentDate = df.format(new Date());
				String log = "";
				final SolrClient solrServer = SolrServers.getSolrServer(Core.FILESHARE);
				final SolrQuery query = new SolrQuery();
				query.setQuery("*:*");
				// Add specified facet fields
				for (final String facetField : facetFields) {
					query.addFacetField(facetField);
				}
				final QueryResponse queryResponse = solrServer.query(query);
				final SolrDocumentList response = (SolrDocumentList) queryResponse.getResponse().get("response");
				// Generate the global num of docs log
				log = generateID("no", "no") + "|" + currentDate + "|" + response.getNumFound() + "|no|no";
				LOGGER.log(MonitoringLevel.MONITORING, log);

				// Then generate the logs for each facet
				for (final FacetField facetField : queryResponse.getFacetFields()) {
					final String facetType = facetField.getName();
					for (final Count facet : facetField.getValues()) {
						final String facetValue = facet.getName();
						final String id = generateID(facetValue, facetType);
						log = id + "|" + currentDate + "|" + facet.getCount() + "|" + facetValue + "|" + facetType;
						LOGGER.log(MonitoringLevel.MONITORING, log);
					}
				}

			} catch (final Exception e) {
				LOGGER.error("Unable to retrieve Index infos");
			}
		}

		/**
		 * Generates an ID for the log, which is needed to exploit the
		 * monitoring logs with ELK (Elasticsearch Logstash Kibana).
		 * <p>
		 * The ID must be UNIQUE per monitoring event, per facet value. This is
		 * the reason why the facet value and the facet type are required. The
		 * ID is a MD5 calculated on a string composed by the
		 * currentDate(midnight), the facet value and the facet type.
		 *
		 * @param facetValue
		 *            the facet value
		 * @param facetType
		 *            facet type
		 * @return the generated ID
		 * @throws NoSuchAlgorithmException .
		 */
		private String generateID(final String facetValue, final String facetType) throws NoSuchAlgorithmException {
			// Set the current date according to the selected monitoring event
			// frequency
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
			// Construct the String ID
			final String strID = String.valueOf(currentDate.getTimeInMillis()) + facetValue + facetType;
			// Calculate the MD5 of the String
			final MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(strID.getBytes());
			final byte[] digest = md.digest();
			final StringBuffer sb = new StringBuffer();
			for (final byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		}

	}

}
