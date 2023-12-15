import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public class SolrAtomicUpdateLauncher {

  public static void main(String[] args) throws SolrServerException, IOException {
    int maxDocPerQuery = 1000;
    final String FILESHARE = "FileShare";
    final String OCR = "OCR";
    final String SPACY = "Spacy";

    String solrCollection = SPACY;
    DocumentsCollector docCollect = new DocumentsCollector("https://dev.datafari.com/solr", solrCollection, maxDocPerQuery);

    //FIXME test code: retrieve parameters from config file.
    Calendar calendar = Calendar.getInstance(Locale.ROOT);
    calendar.add(Calendar.YEAR, -20); //20 pour Spacy et 6 pour les autres
    Date fromDate = calendar.getTime();
    //------------------------------------------------------
    List<String> docIDs = docCollect.collectDocuments(fromDate);

    //FIXME test code: print result
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

/*    SolrInputDocument doc = new SolrInputDocument();
    doc.addField("numero","50");
    Long num1 = 3L;
    doc.addField("kikoo", num1);*/
  }

    /*

    String sDate1="";
    String test = null;
    long kikoo = 0L;
    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z", Locale.ROOT);
    test = DATE_FORMAT.format(new Date(0));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);

    SolrQuery query = (new SolrQuery("*:*")).addFilterQuery("name:oliv").setRows(50).setSort("id", SolrQuery.ORDER.asc).addFilterQuery("last_modified:"+"["+(test)+" TO NOW]");
    String cursorMark = CursorMarkParams.CURSOR_MARK_START;
    ArrayList<String> list = null;
    boolean done = false;
    while (! done) {
      query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
      QueryResponse response = client.query("techproducts",query);
      String nextCursorMark = response.getNextCursorMark();
     // System.out.println(response);
      SolrDocumentList documents = response.getResults();
      for(SolrDocument document : documents) {
       System.out.println(document.getFieldValue("id"));
        //System.out.println(document.getFieldValue("security_allow"));
        list =  (ArrayList<String>) document.getFieldValue("security_allow");

      }
      if (cursorMark.equals(nextCursorMark)) {
        done = true;
      }
      cursorMark = nextCursorMark;
    }
    Iterator itr=list.iterator();
    while(itr.hasNext()){
     System.out.println(itr.next());
    }


      /*
     *
     * final SolrClient client = getSolrClient();
        final Map<String, String> queryParamMap = new HashMap<String, String>();
        queryParamMap.put("q", "*:*");
        queryParamMap.put("fl", "id, name");
        MapSolrParams queryParams = new MapSolrParams(queryParamMap);

        final QueryResponse response = client.query("techproducts", queryParams);
        final SolrDocumentList documents = response.getResults();
        System.out.println(response);


        for(SolrDocument document : documents) {
          System.out.println(document.getFieldValue("id"));

        }
     */
    /*
		String test = "{responseHeader={status=0,QTime=11},success={207.154.216.240:8983_solr={responseHeader={status=0,QTime=0}},207.154.216.134:8983_solr={responseHeader={status=0,QTime=9}}},awkpepxwaabv4laj44hcd9w6716918229781122={responseHeader={status=0,QTime=1},STATUS=failed,Response=Failed to backup core=datafari_shard1_replica_n1 because org.apache.solr.common.SolrException: Directory to contain snapshots doesn't exist: file:///var/backup_datafari/hjbn},awkpepxwaabv4laj44hcd9w6716918229468239={responseHeader={status=0,QTime=0},STATUS=completed,Response=TaskId: awkpepxwaabv4laj44hcd9w6716918229468239 webapp=null path=/admin/cores params={core=datafari_shard2_replica_n3&async=awkpepxwaabv4laj44hcd9w6716918229468239&qt=/admin/cores&name=shard2&action=BACKUPCORE&location=file:///var/backup_datafari/hjbn&wt=javabin&version=2} status=0 QTime=0},status={state=completed,msg=found [awkpepxwaabv4laj44hcd9w67] in completed tasks}}";
		if (test.contains("STATUS=failed"))
			System.out.println("kikoo");
     */


}
