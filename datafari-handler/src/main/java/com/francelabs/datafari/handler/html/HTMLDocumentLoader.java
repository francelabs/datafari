package com.francelabs.datafari.handler.html;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.UpdateParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.handler.loader.ContentStreamLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTMLDocumentLoader extends ContentStreamLoader {

	private static final String FIELD_PARAM = "field";
	private static final String SELECTOR_PARAM = "selector";
	private static final String SEPARATOR_PARAM = "separator";
	private static final String DEFAULT_SEPARATOR = "";

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private String[] fields;

	@Override
	public void load(SolrQueryRequest req, SolrQueryResponse rsp, ContentStream stream,
			UpdateRequestProcessor processor) throws Exception {

		SolrParams params = req.getParams();
		AddUpdateCommand templateAdd = new AddUpdateCommand(req);
		templateAdd.overwrite = req.getParams().getBool(UpdateParams.OVERWRITE, true);
		templateAdd.commitWithin = req.getParams().getInt(UpdateParams.COMMIT_WITHIN, -1);
		templateAdd.solrDoc = new SolrInputDocument();

		InputStream is = stream.getStream();
		Document document = Jsoup.parse(is, "UTF-8", "");

		fields = params.getParams(FIELD_PARAM);
		
		// for each fields in the request handler configuration, fetch the data that matches
		// the selector (also specified in request handler) through Jsoup and put it the solr input document
		for (String field : fields) {
			if (field.startsWith("ATTR") || field.startsWith("KEY")) {
				String selector = params.getFieldParam(field, SELECTOR_PARAM);
				String separator = params.getFieldParam(field, SEPARATOR_PARAM);
				if (separator == null) {
					separator = DEFAULT_SEPARATOR;
				}

				try {
					Elements elements = document.select(selector);
					StringBuilder sBuilder = new StringBuilder();
					for (int i = 0; i < elements.size(); i++) {
						if (field.startsWith("ATTR")) {
							sBuilder.append(elements.get(i).attr("class"));
							if (i < elements.size() - 1)
								sBuilder.append(separator);
							templateAdd.solrDoc.addField(field.replaceAll("ATTR", ""), sBuilder.toString());
						} else {
							sBuilder.append(elements.get(i).attr("content"));
							if (i < elements.size() - 1)
								sBuilder.append(separator);
							templateAdd.solrDoc.addField(field.replaceAll("KEY", ""), sBuilder.toString());
						}

					}
				} catch (Exception e) {
					logger.error("cannot extract data : " + e.getMessage(), e);
				}
			} else {
				String selector = params.getFieldParam(field, SELECTOR_PARAM);
				String separator = params.getFieldParam(field, SEPARATOR_PARAM);
				if (separator == null) {
					separator = DEFAULT_SEPARATOR;
				}

				try {
					Elements elements = document.select(selector);
					StringBuilder sBuilder = new StringBuilder();
					for (int i = 0; i < elements.size(); i++) {
						sBuilder.append(elements.get(i).text());
						if (i < elements.size() - 1)
							sBuilder.append(separator);
					}
					templateAdd.solrDoc.addField(field, sBuilder.toString());
				} catch (Exception e) {
					logger.error("cannot extract data : " + e.getMessage(), e);

				}
			}

		}

		try {
			templateAdd.solrDoc.addField("id", req.getParams().get("literal.id"));
		} catch (Exception e) {
			logger.error("cannot extract data : " + e.getMessage(), e);
		}

		IOUtils.closeQuietly(is);

		processor.processAdd(templateAdd);

	}

}
