package com.francelabs.datafari.external.search.api;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.SearchBuilder;
import com.dropbox.core.v2.files.SearchMatch;
import com.dropbox.core.v2.files.SearchMode;
import com.dropbox.core.v2.files.SearchResult;

public class DropboxSource implements IExternalSource {

  private final String clientIdentifier;
  private final String accessToken;

  private static final String url_prefix = "https://www.dropbox.com/preview";

  private static final Logger LOGGER = LogManager.getLogger(DropboxSource.class.getName());

  public DropboxSource(final String clientIdentifier, final String accessToken, final JSONObject parameters) {
    this.clientIdentifier = clientIdentifier;
    this.accessToken = accessToken;
  }

  public static boolean validAccessToken(final String clientIdentifier, final String accessToken) {
    final DbxRequestConfig config = DbxRequestConfig.newBuilder("test").build();
    final DbxClientV2 client = new DbxClientV2(config, accessToken);
    final SearchBuilder sb = client.files().searchBuilder("", "*").withMode(SearchMode.FILENAME_AND_CONTENT).withStart((long) 0).withMaxResults((long) 1);
    try {
      sb.start();
    } catch (final DbxException e) {
      return false;
    }
    return true;
  }

  @Override
  public ResultDocumentList executeQuery(final String query, final int start, final int rows, final Map<String, String[]> additionnalParams) throws Exception {
    final DbxRequestConfig config = DbxRequestConfig.newBuilder(clientIdentifier).build();
    final DbxClientV2 client = new DbxClientV2(config, accessToken);
    final JSONParser parser = new JSONParser();
    final ResultDocumentList rdl = new ResultDocumentList();
    rdl.setStart(start);
    rdl.setRows(rows);

    final SearchBuilder sb = client.files().searchBuilder("", query).withMode(SearchMode.FILENAME_AND_CONTENT).withStart((long) start).withMaxResults((long) rows);
    try {
      final SearchResult result = sb.start();
      final List<SearchMatch> resultList = result.getMatches();
      int numFound = 0;
      for (final SearchMatch sm : resultList) {
        final Metadata metadata = sm.getMetadata();
        final JSONObject jMetadata = (JSONObject) parser.parse(metadata.toStringMultiline());
        if (jMetadata.get(".tag").toString().equals("file")) {
          numFound++;
          final String id = url_prefix + metadata.getPathDisplay();
          final String preview = "Last modified date: " + jMetadata.get("server_modified").toString();
          final String title = metadata.getName();
          final String url = id;
          String extension = "bin";
          if (title.lastIndexOf(".") != -1) {
            extension = title.substring(title.lastIndexOf(".") + 1);
          }
          final String highlighting = "";
          final ResultDocument rd = new ResultDocument(id, preview, title, url, extension, highlighting);
          rdl.addResultDocument(rd);
        }
      }
      rdl.setNumFound(numFound);
      rdl.addOtherField("more", result.getMore());
    } catch (DbxException | ParseException e) {
      LOGGER.error("Unable to execute Dropbox query", e);
      throw e;
    }
    return rdl;
  }

}
