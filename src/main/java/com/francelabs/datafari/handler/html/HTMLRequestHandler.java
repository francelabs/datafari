package com.francelabs.datafari.handler.html;

import org.apache.solr.handler.ContentStreamHandlerBase;
import org.apache.solr.handler.loader.ContentStreamLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.update.processor.UpdateRequestProcessor;


public class HTMLRequestHandler extends ContentStreamHandlerBase {

	@Override
	protected ContentStreamLoader newLoader(SolrQueryRequest req,
			UpdateRequestProcessor processor) {
		return new HTMLDocumentLoader();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSource() {
		// TODO Auto-generated method stub
		return null;
	}

}