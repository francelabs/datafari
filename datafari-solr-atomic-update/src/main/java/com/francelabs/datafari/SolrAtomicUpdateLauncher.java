package com.francelabs.datafari;

import com.francelabs.datafari.config.AtomicUpdateConfig;
import com.francelabs.datafari.config.ConfigLoader;
import com.francelabs.datafari.config.JobConfig;
import com.francelabs.datafari.save.JobSaver;
import com.francelabs.datafari.solraccessors.DocumentsCollector;
import com.francelabs.datafari.solraccessors.DocumentsUpdator;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class SolrAtomicUpdateLauncher {
  final static int MAX_DOC_PER_QUERY = 1000;

  public static void main(String[] args) throws SolrServerException, IOException {
    //Read jobs config file
    AtomicUpdateConfig config = ConfigLoader.getConfig();
    JobConfig job = config.getJobs().get(args[0]);

    // Read last start date of the job
    String fromDate = JobSaver.getExecutionDate(args[0]);
    System.out.println(fromDate);
    Date startDate = new Date();

    DocumentsCollector docCollect = new DocumentsCollector(job, MAX_DOC_PER_QUERY);
    List<SolrDocument> docsList = docCollect.collectDocuments(fromDate);

    // Update documents
    UpdateResponse updateResponse = new DocumentsUpdator(job, MAX_DOC_PER_QUERY)
        .updateDocuments(docsList);
    System.out.println(updateResponse);

    // Write the start execution date of the job
    JobSaver.writeExecutionDate(args[0], startDate);
  }
}
