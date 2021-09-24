/*******************************************************************************
 *  * Copyright 2020 France Labs
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
package com.francelabs.datafari.rest.v1_0.fields;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;

import com.francelabs.datafari.rest.v1_0.utils.RestAPIUtils;
import com.francelabs.datafari.service.indexer.IndexerServerManager.Core;
import com.francelabs.datafari.utils.AdvancedSearchConfiguration;
import com.francelabs.datafari.utils.SolrConfiguration;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@RestController
public class Info {
    private static final Logger logger = LogManager.getLogger(Info.class.getName());

    @GetMapping(value = {"/rest/v1.0/fields/info", "/rest/v1.0/fields/info/{fieldName}"}, produces = "application/json;charset=UTF-8")
    public String getFieldsInfo(final HttpServletRequest request, @PathVariable(required = false) String fieldName) {
        try {
            // Retrieve the Solr hostname, port and protocol
            final String solrserver = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRHOST);
            final String solrport = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPORT);
            final String protocol = SolrConfiguration.getInstance().getProperty(SolrConfiguration.SOLRPROTOCOL);

            final HttpClientBuilder builder = HttpClientBuilder.create();

            if (protocol.toLowerCase().equals("https")) {
                try {
                    // setup a Trust Strategy that allows all certificates.
                    final SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(final X509Certificate[] arg0, final String arg1)
                                throws CertificateException {
                            return true;
                        }
                    }).build();
                    final SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
                            NoopHostnameVerifier.INSTANCE);
                    builder.setSSLSocketFactory(sslConnectionFactory);

                    final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("https", sslConnectionFactory).build();

                    final HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);

                    builder.setConnectionManager(ccm);
                } catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
                    logger.warn("Unable to set trust all certificates startegy", e);
                }
            }

            // Use Solr Schema REST API to get the list of fields
            final HttpClient httpClient = builder.build();
            final HttpHost httpHost = new HttpHost(solrserver, Integer.parseInt(solrport), protocol);
            final HttpGet httpGet = new HttpGet("/solr/" + Core.FILESHARE.toString() + "/schema/fields");
            final HttpResponse httpResponse = httpClient.execute(httpHost, httpGet);

            // Construct the jsonResponse
            final JSONObject jsonResponse = new JSONObject();
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                // Status of the API response is OK
                final JSONParser parser = new JSONParser();
                final JSONObject json = (JSONObject) parser.parse(EntityUtils.toString(httpResponse.getEntity()));
                final JSONArray fieldsJson = (JSONArray) json.get("fields");
                final JSONArray fieldsArray = new JSONArray();
                // Load the list of denied fields
                final String strDeniedFieldsList = AdvancedSearchConfiguration.getInstance()
                        .getProperty(AdvancedSearchConfiguration.DENIED_FIELD_LIST);
                final Set<String> deniedFieldsSet = new HashSet<>(Arrays.asList(strDeniedFieldsList.split(",")));
                for (int i = 0; i < fieldsJson.size(); i++) {
                    final JSONObject field = (JSONObject) fieldsJson.get(i);
                    // If a fieldname has been provided, it means that this
                    // servlet only needs to return infos on this specific field
                    if (fieldName != null) {
                        if (field.get("name").toString().equals(fieldName)) {
                            fieldsArray.add(field);
                            break;
                        }
                    } else {
                        if (!deniedFieldsSet.contains(field.get("name").toString())
                                && (field.get("indexed") == null || field.get("indexed").toString().equals("true"))
                                && !field.get("name").toString().startsWith("allow_")
                                && !field.get("name").toString().startsWith("deny_")
                                && !field.get("name").toString().startsWith("_")) {
                            fieldsArray.add(field);
                        }
                    }
                }
                jsonResponse.put("fields", fieldsArray);
                return RestAPIUtils.buildOKResponse(jsonResponse);
            } else {
                // Status of the API response is an error
                logger.error("Error while retrieving the fields from the Schema API of Solr: "
                        + httpResponse.getStatusLine().toString());
                JSONObject extra = new JSONObject();
                extra.put("details",
                        "Error while retrieving the fields from the Schema API of Solr, please retry, if the problem persists contact your system administrator. Error Code : 69026");
                return RestAPIUtils.buildErrorResponse(500, "Internal Error", extra);
            }
        } catch (final IOException | ParseException e) {
            logger.error("Error while retrieving the fields from the Schema API of Solr", e);
            JSONObject extra = new JSONObject();
            extra.put("details",
                    "Error while retrieving the fields from the Schema API of Solr, please retry, if the problem persists contact your system administrator. Error Code : 69026");
            return RestAPIUtils.buildErrorResponse(500, "Internal Error", extra);
        }
    }

}
