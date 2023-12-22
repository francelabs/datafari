package com.francelabs.datafari;

import com.francelabs.datafari.config.AtomicUpdateConfig;
import com.francelabs.datafari.config.ConfigLoader;
import com.francelabs.datafari.config.JobConfig;
import com.francelabs.datafari.save.JobSaver;
import com.francelabs.datafari.save.Status;
import com.francelabs.datafari.solraccessors.DocumentsCollector;
import com.francelabs.datafari.solraccessors.DocumentsUpdator;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class SolrAtomicUpdateLauncher {
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrAtomicUpdateLauncher.class);
  /**
   * Get the date from which to select documents. This date is intended to be used with the last_modified field of the document.
   * Uses arguments from main method, with:
   * <ul>
   *   <li> args[0] is the job name launched. Used to get the last start date of the job.</li>
   *   <li> args[1] (Optional) is the fromDate used. The last start date of the job is not taken into account.</li>
   *   <ul>
   *     <li>The expected date format is "yyyy-MM-dd HH:mm" with or without time specified, and french order is supported.</li>
   *     <li>Specify "full" (not case sensitive) to force full crawl</li>
   *     <li>If not specified, the date is retrieved from "atomicUpdateLastExec" file (generally used this way).</li>
   *   </ul>
   *
   * </ul>
   *
   * @param args arguments from main method.
   * @return the fromDate to be used to select documents. Is null to specify a full crawl is expected.
   * @throws ParseException if it is impossible to parse the date in args[1] if specified.
   */
  private static String getStartDateForDocumentsSelection(String[] args) throws ParseException {
    String fromDate;
    if (args.length == 1){
      // Read last start date of the job
      fromDate = JobSaver.getExecutionDate(args[0]);
    } else {
      fromDate = args[1];
      if (fromDate.equalsIgnoreCase("full")) {
        fromDate = null;
      } else {
        final String[] datePatterns = { "yyyy-MM-dd", "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy", "dd-MM-yyyy HH:mm"};
        try {
          Date dFromDate = DateUtils.parseDate(args[1],datePatterns);
          fromDate = dFromDate.toInstant().toString();
        } catch (Exception e){
          StringBuilder message = new StringBuilder("Unable to parse date for the \"fromDate\" = \"" + fromDate + "\". Formats expected: ");
          for (String pattern : datePatterns){
            message.append("\n- " );
            message.append(pattern);
          }
          LOGGER.error(message.toString());
          throw e;
        }
      }
    }

    LOGGER.info(args[0] + " Job: Select documents modified from: " + fromDate + " (null indicates a full crawl))");
    return fromDate;
  }
  public static void main(String[] args) throws ParseException {
    String jobName = args[0];
    //Read jobs config file
    AtomicUpdateConfig config = ConfigLoader.getConfig();
    JobConfig job = config.getJobs().get(jobName);
    LOGGER.info(jobName + " Job started !");

    String fromDate = getStartDateForDocumentsSelection(args);

    JobSaver jobSaver = new JobSaver(jobName);
    Status jobStatus = jobSaver.getJobLastStatus();
    if (Status.FAILED.equals(jobStatus)){
      fromDate = null;
      LOGGER.info(jobName + " Job: Last state was " + Status.FAILED + ", so a full crawl is done for this run.");
    } else if (Status.RUNNING.equals(jobStatus)){
      return;
    }


    jobSaver.notifyJobRunning();
    int nbDocsProcessed = 0;
    try ( DocumentsCollector docCollect = DocumentsCollector.getInstance(job);
          DocumentsUpdator docUpdator = DocumentsUpdator.getInstance(job)) {
      List<SolrDocument> docsList;
      do {
        docsList = docCollect.collectDocuments(fromDate);
        if (!docsList.isEmpty()) {
          // Update documents
          UpdateResponse updateResponse = docUpdator.updateDocuments(docsList);
          if (updateResponse.getStatus() == 0) {
            nbDocsProcessed = nbDocsProcessed + docsList.size();
          }
        }
      } while (!docsList.isEmpty());

      jobSaver.notifyJobDone();
    } catch (Exception e){
      LOGGER.error(jobName + " Job: Total number of documents processed: " + nbDocsProcessed, e);
      jobSaver.notifyJobFailed();
    }
  }
}
