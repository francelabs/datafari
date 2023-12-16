import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import solraccessors.DocumentsCollector;
import solraccessors.DocumentsUpdator;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public class SolrAtomicUpdateLauncher {
  final static int MAX_DOC_PER_QUERY = 1000;
  final static String FILESHARE = "FileShare";
  final static String OCR = "OCR";
  final static String SPACY = "Spacy";

  public static void main(String[] args) throws SolrServerException, IOException {

    //"https://dev.datafari.com/solr"
    DocumentsCollector docCollect = new DocumentsCollector("http://localhost:8983/solr", FILESHARE, MAX_DOC_PER_QUERY);

    //FIXME test code: retrieve parameters from config file.
    Calendar calendar = Calendar.getInstance(Locale.ROOT);
    calendar.add(Calendar.YEAR, -20); //20 pour Spacy et 6 pour les autres
    Date fromDate = calendar.getTime();
    //------------------------------------------------------
    List<String> docIDs = docCollect.collectDocuments(fromDate);

    //FIXME test code: -------------------------------------
    // print result
    //testCodeCollectDoc(docIDs, FILESHARE, fromDate);
    // Corrupt one ID to fail one update
    //docIDs.set(1, "titi");
    //------------------------------------------------------


    // Update documents
    UpdateResponse updateResponse = new DocumentsUpdator("http://localhost:8983/solr", FILESHARE, MAX_DOC_PER_QUERY)
        .updateDocuments(docIDs);
    System.out.println(updateResponse);
  }

  //FIXME test code: print result
  private static void testCodeCollectDoc(List<String> docIDs, String solrCollection, Date fromDate){
    long numDocsFound = docIDs.size();
    System.out.println("Found " + numDocsFound + " documents");
    if (fromDate == null) {
      if (FILESHARE.equals(solrCollection)) {
        assertEquals(9049, numDocsFound);
      } else if (OCR.equals(solrCollection)){
        assertEquals(1109, numDocsFound);
      } else if (SPACY.equals(solrCollection)) {
        assertEquals(205, numDocsFound);
      }
    } else {
      if (FILESHARE.equals(solrCollection)) {
        assertEquals(39, numDocsFound);
      } else if (OCR.equals(solrCollection)){
        assertEquals(8, numDocsFound);
      } else if (SPACY.equals(solrCollection)) {
        assertEquals(14, numDocsFound);
      }
    }

  }
}
