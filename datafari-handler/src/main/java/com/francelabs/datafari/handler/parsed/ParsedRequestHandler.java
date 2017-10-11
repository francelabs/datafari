package com.francelabs.datafari.handler.parsed;

import org.apache.solr.handler.ContentStreamHandlerBase;
import org.apache.solr.handler.loader.ContentStreamLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.update.processor.UpdateRequestProcessor;

public class ParsedRequestHandler extends ContentStreamHandlerBase {

  @Override
  protected ContentStreamLoader newLoader(final SolrQueryRequest arg0, final UpdateRequestProcessor arg1) {
    // TODO Auto-generated method stub
    return new ParsedDocumentLoader();
  }

  @Override
  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

}
