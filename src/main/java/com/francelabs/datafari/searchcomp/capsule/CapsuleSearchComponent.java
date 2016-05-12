package com.francelabs.datafari.searchcomp.capsule;

import java.io.IOException;
import java.util.Date;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.francelabs.datafari.searchcomp.utils.LoadProperties;

public class CapsuleSearchComponent extends SearchComponent {
	private static Logger LOGGER = LoggerFactory.getLogger(CapsuleSearchComponent.class);
	volatile long numRequests;
	volatile long numErrors;
	volatile long totalRequestsTime;
	volatile String lastnewSearcher;
	volatile String lastOptimizeEvent;
	protected String defaultFile;

	public void init(NamedList args) {
		super.init(args);
		this.defaultFile = ((String) args.get("file"));
		if (this.defaultFile == null) {
			throw new SolrException(SolrException.ErrorCode.NOT_FOUND,
					"Need to specify the default file for Capsule component config");
		}
		LoadProperties.setConfigPropertiesFileName(this.defaultFile);
	}

	public void prepare(ResponseBuilder rb) throws IOException {
	}

	public void process(ResponseBuilder rb) throws IOException {
		this.numRequests += 1L;

		SolrParams params = rb.req.getParams();
		long lstartTime = System.currentTimeMillis();
		SolrIndexSearcher searcher = rb.req.getSearcher();

		NamedList response = new SimpleOrderedMap();

		String query = params.get("q");
		String queryCapsule = null;
		queryCapsule = query;
		if (queryCapsule != null) {
			queryCapsule = query.replaceAll(" ", "\\\\ ");
			queryCapsule = query.toLowerCase();

			String capsule = null;
			String capsuleTitle = null;
			String capsuleBody = null;
			// TODO : tempoorary hardcoded path
			String solrhome = "/opt/datafari/solr/solrcloud/FileShare/conf/";
			LoadProperties.setPathPropertiesFileName(solrhome);
			DocList docs = rb.getResults().docList;
			if ((docs == null) || (docs.size() == 0)) {
				LOGGER.debug("No results");
			}
			LOGGER.debug("Doing This many docs:\t" + docs.size());
			capsule = LoadProperties.getProperty(queryCapsule);
			if (capsule != null) {
				capsule = capsule.replaceAll("\\\\ ", " ");
				capsuleTitle = capsule.split(";")[0];
				capsuleBody = capsule.split(";")[1];
				response.add("title", capsuleTitle);
				response.add("body", capsuleBody);
			}
		}
		rb.rsp.add("capsuleSearchComponent", response);
		this.totalRequestsTime += System.currentTimeMillis() - lstartTime;
	}

	public void postCommit() {
	}

	public void postSoftCommit() {
	}

	public void newSearcher(SolrIndexSearcher newSearcher, SolrIndexSearcher currentSearcher) {
		if (currentSearcher != null) {
			this.lastnewSearcher = new Date().toString();
			if (newSearcher.getIndexReader().leaves().size() == 1) {
				this.lastOptimizeEvent = new Date().toString();
			}
		}
	}

	public String getDescription() {
		return "CapsuleSearchComponent";
	}

	public String getVersion() {
		return "1.0";
	}

	public String getSource() {
		return "http://www.francelabs.com";
	}

	public NamedList<Object> getStatistics() {
		NamedList all = new SimpleOrderedMap();
		all.add("requests", "" + this.numRequests);
		all.add("errors", "" + this.numErrors);
		all.add("totalRequestTime(ms)", "" + this.totalRequestsTime);
		if (this.lastnewSearcher == null) {
			this.lastnewSearcher = "N/A";
		}
		all.add("lastNewSearchEvent", this.lastnewSearcher);
		if (this.lastOptimizeEvent == null) {
			this.lastOptimizeEvent = "N/A";
		}
		all.add("lastOptimizeEvent", this.lastOptimizeEvent);

		return all;
	}
}
