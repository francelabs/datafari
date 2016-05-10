package com.francelabs.datafari.handler.html;


import java.io.InputStream;


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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HTMLDocumentLoader extends ContentStreamLoader{

	public static final String FIELD_PARAM = "field";
	public static final String SELECTOR_PARAM = "selector";
	public static final String SEPARATOR_PARAM = "separator";
	public static final String DEFAULT_SEPARATOR = "";

	public String[] fields;

	@Override
	public void load(SolrQueryRequest req, SolrQueryResponse rsp,
			ContentStream stream, UpdateRequestProcessor processor)
					throws Exception {

		SolrParams params = req.getParams();
		AddUpdateCommand templateAdd = new AddUpdateCommand(req);
		templateAdd.overwrite = req.getParams().getBool(UpdateParams.OVERWRITE, true);
		templateAdd.commitWithin = req.getParams().getInt(UpdateParams.COMMIT_WITHIN, -1);
		templateAdd.solrDoc = new SolrInputDocument();

		InputStream is = stream.getStream();
		Document document = Jsoup.parse(is, "UTF-8", "");

		fields = params.getParams(FIELD_PARAM);
		for (String field : fields){
			if (field.startsWith("ATTR")){
				String selector = params.getFieldParam(field, SELECTOR_PARAM);
				String separator = params.getFieldParam(field, SEPARATOR_PARAM);
				if (separator == null){
					separator = DEFAULT_SEPARATOR;
				}

				try {
					Elements elements = document.select(selector);
					StringBuilder sBuilder = new StringBuilder();
					for (int i=0; i<elements.size(); i++){
						sBuilder.append(elements.get(i).attr("class"));
						if (i<elements.size()-1)
							sBuilder.append(separator);
					}
					templateAdd.solrDoc.addField(field.replaceAll("ATTR", ""), sBuilder.toString());
				}
				catch(Exception e){
					System.out.println("cannot extract data : "+e.getMessage());
					e.printStackTrace();
				}
			}
			else if (field.startsWith("KEY")){
				String selector = params.getFieldParam(field, SELECTOR_PARAM);
				String separator = params.getFieldParam(field, SEPARATOR_PARAM);
				if (separator == null){
					separator = DEFAULT_SEPARATOR;
				}

				try {
					Elements elements = document.select(selector);
					StringBuilder sBuilder = new StringBuilder();
					for (int i=0; i<elements.size(); i++){
						sBuilder.append(elements.get(i).attr("content"));
						if (i<elements.size()-1)
							sBuilder.append(separator);
					}
					templateAdd.solrDoc.addField(field.replaceAll("KEY", ""), sBuilder.toString());
					System.out.println("keywords"+ sBuilder.toString());
				}
				catch(Exception e){
					System.out.println("cannot extract data : "+e.getMessage());
					e.printStackTrace();
				}
			}


			else {
				String selector = params.getFieldParam(field, SELECTOR_PARAM);
				String separator = params.getFieldParam(field, SEPARATOR_PARAM);
				if (separator == null){
					separator = DEFAULT_SEPARATOR;
				}

				try {
					Elements elements = document.select(selector);
					StringBuilder sBuilder = new StringBuilder();
					for (int i=0; i<elements.size(); i++){
						sBuilder.append(elements.get(i).text());
						if (i<elements.size()-1)
							sBuilder.append(separator);
					}
					templateAdd.solrDoc.addField(field, sBuilder.toString());
				}
				catch(Exception e){
					System.out.println("cannot extract data : "+e.getMessage());
					e.printStackTrace();
				}
			}

		}

		try {
			templateAdd.solrDoc.addField("id",req.getParams().get("literal.id"));
		}
		catch(Exception e){
			System.out.println("cannot extract data : "+e.getMessage());
			e.printStackTrace();
		}


		IOUtils.closeQuietly(is);

		processor.processAdd(templateAdd);

	}

}