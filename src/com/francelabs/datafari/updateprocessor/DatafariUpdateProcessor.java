/*******************************************************************************
 /*******************************************************************************
 *  * Copyright 2015 France Labs
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.updateprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

public class DatafariUpdateProcessor extends UpdateRequestProcessor {

	public DatafariUpdateProcessor(final UpdateRequestProcessor next) {
		super(next);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void processAdd(final AddUpdateCommand cmd) throws IOException {
		final SolrInputDocument doc = cmd.getSolrInputDocument();

		final String url = (String) doc.getFieldValue("id");

		// Create path hierarchy for facet
		final List<String> urlHierarchy = new ArrayList<String>();

		final String path = url.replace("file:", "");
		int previousIndex = 1;
		int depth = 0;
		// Tokenize the path and add the depth as first character for each token
		// (like: 0/home, 1/home/project ...)
		for (int i = 0; i < path.split("/").length - 2; i++) {
			int endIndex = path.indexOf('/', previousIndex);
			if (endIndex == -1) {
				endIndex = path.length() - 1;
			}
			urlHierarchy.add(depth + path.substring(0, endIndex));
			depth++;
			previousIndex = endIndex + 1;
		}

		// Add the tokens to the urlHierarchy field
		doc.addField("urlHierarchy", urlHierarchy);

		doc.addField("url", url);

		if (url.startsWith("http")) {
			// web
			// SolrInputDocument(fields: [ignored_link=[text/css, stylesheet,
			// bootstrap/css/bootstrap.min.css,
			// http://fonts.googleapis.com/css?family=Open+Sans:300,400,600,800,
			// stylesheet, text/css, stylesheet, text/css,
			// js-plugin/animation-framework/animate.css, stylesheet, text/css,
			// js-plugin/pretty-photo/css/prettyPhoto.css, stylesheet, text/css,
			// js-plugin/flexslider/flexslider.css, text/css, stylesheet,
			// font-icons/custom-icons/css/custom-icons.css, text/css,
			// stylesheet, font-icons/custom-icons/css/custom-icons-ie7.css,
			// text/css, stylesheet, css/layout.css, text/css, stylesheet,
			// css/green.css, shortcut icon, images/favicon.ico,
			// apple-touch-icon, images/apple-touch-icon.png, apple-touch-icon,
			// images/apple-touch-icon-72x72.png, apple-touch-icon,
			// images/apple-touch-icon-114x114.png, apple-touch-icon,
			// images/apple-touch-icon-144x144.png], ignored_meta=[viewport,
			// width=device-width, initial-scale=1, maximum-scale=1, author,
			// France Labs, stream_source_info, docname, stream_content_type,
			// text/html, description, Datafari, the only open source search
			// solution in Apache license, stream_size, 19228, Content-Encoding,
			// UTF-8, stream_name, docname, Content-Type, text/html;
			// charset=UTF-8, resourceName, docname, dc:title, Datafari de
			// France Labs - Open Search], ignored_a=[rect, index.html, rect,
			// #homeApp, rect, #about, rect, #connect, rect, #screenshots, rect,
			// #contactSlice, rect, ./en/index.html, rect,
			// ftp://178.33.82.49/windows/Datafari_Setup.exe, rect,
			// ftp://178.33.82.49/debian/datafari.deb, rect, #connect, rect,
			// #homeApp, rect, http://code.google.com/p/datafari/, rect,
			// #screenshots, rect, #about, rect,
			// https://www.facebook.com/pages/France-Labs/307147449330206, rect,
			// https://twitter.com/francelabs, rect,
			// https://plus.google.com/112601775414040527820/posts, rect,
			// http://www.linkedin.com/company/france-labs, rect,
			// http://www.francelabs.com/contact.html, rect, #homeApp, rect,
			// http://www.francelabs.com, rect, https://twitter.com/francelabs,
			// rect, http://www.francelabs.com],
			// ignored_img=[images/Logo_Datafari_Couture_vert.png, Datafari
			// logo, images/theme-pics/app-mobile_datafari.png, Datafari logo,
			// images/slider/flex-slider/Datafari_Tablet_Slider-7.png, pic 1,
			// images/slider/flex-slider/Datafari_Tablet_Slider-8.png, pic 2,
			// images/slider/flex-slider/Datafari_Tablet_Slider-9.png, pic 1,
			// images/theme-pics/zebra-inverted-big.png, Altea one page app, ,
			// images/portfolio/manifoldcf_screen.png, ,
			// images/portfolio/search_screen.png, ,
			// images/portfolio/solr_admin_screen.png, ,
			// images/portfolio/results_screen.png, images/footer-logo-fl.png,
			// latest Little Neko news], ignored_header_accept_ranges=bytes,
			// ignored_header_connection=keep-alive,
			// ignored_header_content_encoding=gzip,
			// ignored_header_vary=Accept-Encoding, id=http://www.datafari.com,
			// ignored_header_server=Apache,
			// ignored_header_content_type=text/html,
			// ignored_header_content_length=4942,
			// ignored_viewport=width=device-width, initial-scale=1,
			// maximum-scale=1, ignored_author=France Labs,
			// ignored_stream_source_info=docname,
			// ignored_stream_content_type=text/html,
			// ignored_description=Datafari, the only open source search
			// solution in Apache license, ignored_stream_size=19228,
			// ignored_content_encoding=UTF-8, ignored_stream_name=docname,
			// ignored_content_type=text/html; charset=UTF-8,
			// ignored_resourcename=docname, ignored_dc_title=Datafari de France
			// Labs - Open Search, language=fr, content_fr=
			if (doc.get("title") == null) {
				final String filename = (String) doc.get("ignored_stream_name").getFirstValue();
				doc.addField("title", filename);
			}
			doc.addField("source", "web");
		}

		if (url.startsWith("file")) {

			// fileshare
			// SolrInputDocument(fields: [ignored_meta=[dcterms:modified,
			// 2014-04-10T16:31:13Z, meta:creation-date, 2014-04-10T16:31:13Z,
			// stream_source_info, Fiche_Formation_Objectifs_Solr_Initiale.pdf,
			// meta:save-date, 2014-04-10T16:31:13Z, dc:creator, Cedric Ulmer,
			// Last-Modified, 2014-04-10T16:31:13Z, dcterms:created,
			// 2014-04-10T16:31:13Z, Author, Cedric Ulmer, date,
			// 2014-04-10T16:31:13Z, modified, 2014-04-10T16:31:13Z, creator,
			// Cedric Ulmer, xmpTPg:NPages, 2, Creation-Date,
			// 2014-04-10T16:31:13Z, meta:author, Cedric Ulmer, created, Thu Apr
			// 10 18:31:13 CEST 2014, stream_content_type, application/pdf,
			// stream_size, 443226, producer, Microsoft� Word 2013, stream_name,
			// Fiche_Formation_Objectifs_Solr_Initiale.pdf, Content-Type,
			// application/pdf, xmp:CreatorTool, Microsoft� Word 2013,
			// resourceName, Fiche_Formation_Objectifs_Solr_Initiale.pdf,
			// Last-Save-Date, 2014-04-10T16:31:13Z], ignored_div=[page, page,
			// annotation], ignored_a=mailto:cedric.ulmer@francelabs.com,
			// id=file:/G:/testFiles/Fiche_Formation_Objectifs_Solr_Initiale.pdf,
			// ignored_uri=G:\testFiles\Fiche_Formation_Objectifs_Solr_Initiale.pdf,
			// ignored_dcterms_modified=2014-04-10T16:31:13Z,
			// ignored_meta_creation_date=2014-04-10T16:31:13Z,
			// ignored_stream_source_info=Fiche_Formation_Objectifs_Solr_Initiale.pdf,
			// ignored_meta_save_date=2014-04-10T16:31:13Z,
			// ignored_dc_creator=Cedric Ulmer,
			// last_modified=2014-04-10T16:31:13.000Z,
			// ignored_dcterms_created=2014-04-10T16:31:13Z,
			// ignored_author=Cedric Ulmer, ignored_date=2014-04-10T16:31:13Z,
			// ignored_modified=2014-04-10T16:31:13Z, ignored_creator=Cedric
			// Ulmer, ignored_xmptpg_npages=2,
			// ignored_creation_date=2014-04-10T16:31:13Z,
			// ignored_meta_author=Cedric Ulmer, ignored_created=Thu Apr 10
			// 18:31:13 CEST 2014, ignored_stream_content_type=application/pdf,
			// ignored_stream_size=443226, ignored_producer=Microsoft� Word
			// 2013,
			// ignored_stream_name=Fiche_Formation_Objectifs_Solr_Initiale.pdf,
			// ignored_content_type=application/pdf,
			// ignored_xmp_creatortool=Microsoft� Word 2013,
			// ignored_resourcename=Fiche_Formation_Objectifs_Solr_Initiale.pdf,
			// ignored_last_save_date=2014-04-10T16:31:13Z, language=fr,
			// content_fr=

			doc.removeField("title");
			final String filename = (String) doc.get("ignored_stream_name").getFirstValue();
			doc.addField("title", filename);
			doc.addField("source", "file");
		}

		final String mimeType = (String) doc.get("ignored_stream_content_type").getFirstValue();
		final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
		MimeType type;

		String extension = "";
		try {
			type = allTypes.forName(mimeType);
			extension = type.getExtension();
			if (extension.length() > 1) {
				extension = extension.substring(1);
			}
		} catch (final MimeTypeException e) {
			final Pattern pattern = Pattern.compile("[^\\.]*$");
			final Matcher matcher = pattern.matcher(url);
			if (matcher.find()) {
				extension = matcher.group();
			}
		}

		doc.addField("extension", extension.toLowerCase());

		super.processAdd(cmd);
	}
}