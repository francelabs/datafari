package com.francelabs.handler.html;


import java.io.InputStream;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
				if(field.equals("last_modified")){									//Get the field last_modified and format it to a valid date before adding it
					if(req.getParams().get("literal.id").contains("video-")){
						String tostring = sBuilder.toString();
						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						Date d = dateFormat.parse(tostring.substring(16, 20)+"-"+tostring.substring(13, 15)+"-"+tostring.substring(10, 12)+"T00:00:00");
						templateAdd.solrDoc.addField(field, d);
					}
					else{
						String tostring = sBuilder.toString();
						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						String month = tostring.substring(12, tostring.indexOf(",")).replaceAll("\\s","");
						String day = tostring.substring(10, 12).replaceAll("\\s","");
						if(day.length()==1)
							day = "0"+day;
						Date d = dateFormat.parse(tostring.substring(tostring.indexOf(",")+2, tostring.indexOf(",")+6)+"-"+whichMonth(month)+"-"+day
								+"T"+tostring.substring(tostring.indexOf("-")+2, tostring.indexOf("-")+7)+":00");
						templateAdd.solrDoc.addField(field, d);
					}
				}
				else{
					templateAdd.solrDoc.addField(field, sBuilder.toString());
				}

			}
			catch(Exception e){
				System.out.println("cannot extract data : "+e.getMessage());
				e.printStackTrace();
			}

		}

		try {
			templateAdd.solrDoc.addField("id",req.getParams().get("literal.id"));
			templateAdd.solrDoc.addField("url",req.getParams().get("literal.id"));
		}
		catch(Exception e){
			System.out.println("cannot extract data : "+e.getMessage());
			e.printStackTrace();
		}


		IOUtils.closeQuietly(is);

		processor.processAdd(templateAdd);

	}
	public String whichMonth(String month){
		switch(month){
		case "janvier" :
			return "01";
		case "February" :
			return "02";
		case "mars" :
			return "03";
		case "April" :
			return "04";
		case "mai" :
			return "05";
		case "juin" :
			return "06";
		case "juillet" :
			return "07";
		case "août" :
			return "08";
		case "septembre" :
			return "09";
		case "octobre" :
			return "10";
		case "novembre" :
			return "11";
		case "décembre" :
			return "12";
		default :
			return null;
		}

	}

}
