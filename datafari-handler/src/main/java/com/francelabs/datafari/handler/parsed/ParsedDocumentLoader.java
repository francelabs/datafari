package com.francelabs.datafari.handler.parsed;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.params.UpdateParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.handler.loader.ContentStreamLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

public class ParsedDocumentLoader extends ContentStreamLoader {

  @Override
  public void load(final SolrQueryRequest req, final SolrQueryResponse rsp, final ContentStream stream, final UpdateRequestProcessor processor)
      throws Exception {
    // TODO Auto-generated method stub
    final InputStream is = stream.getStream();
    try {
      final ParsedContentHandler tch = new ParsedContentHandler(req.getParams(), req.getSchema(), is);

      final AddUpdateCommand templateAdd = new AddUpdateCommand(req);
      templateAdd.overwrite = req.getParams().getBool(UpdateParams.OVERWRITE, true);
      templateAdd.commitWithin = req.getParams().getInt(UpdateParams.COMMIT_WITHIN, -1);
      templateAdd.solrDoc = tch.newDocument();

      processor.processAdd(templateAdd);

    } finally {
      IOUtils.closeQuietly(is);
    }

  }

}
