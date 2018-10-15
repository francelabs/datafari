/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.francelabs.datafari.solr.custom;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.V2RequestSupport;
import org.apache.solr.client.solrj.impl.BinaryResponseParser;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.request.V2Request;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;

/**
 * This class overrides and somewhat changes the behavior of the SolrJ
 * HttpSolrServer class. The point of all this is simply to get the right
 * information to Tika. When SolrJ uses GET or POST but not multipart-post, it
 * does not include multipart headers that Tika uses - specifically, the name of
 * the document and the length of the document. Patches have been submitted to
 * the SOLR ticket queue to address this problem in a method-insensitive way,
 * but so far there has been no sign that the Solr team is interested in
 * committing them.
 */
public class ModifiedHttpSolrClient extends HttpSolrClient {

  // Here we duplicate all the private fields we need

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private static final String DEFAULT_PATH = "/select";

  private static Charset UTF8_CHARSET;

  private final HttpClient httpClient;
  private final boolean useMultiPartPost = true;

  public ModifiedHttpSolrClient(final String baseURL, final HttpClient client, final ResponseParser parser, final boolean allowCompression) {
    super(baseURL, client, parser, allowCompression);
    httpClient = client;
  }

  @Override
  protected HttpRequestBase createMethod(SolrRequest request, final String collection) throws IOException, SolrServerException {
    if (request instanceof V2RequestSupport) {
      request = ((V2RequestSupport) request).getV2Request();
    }
    final SolrParams params = request.getParams();
    final RequestWriter.ContentWriter contentWriter = requestWriter.getContentWriter(request);
    Collection<ContentStream> streams = contentWriter == null ? requestWriter.getContentStreams(request) : null;
    String path = requestWriter.getPath(request);
    if (path == null || !path.startsWith("/")) {
      path = DEFAULT_PATH;
    }

    ResponseParser parser = request.getResponseParser();
    if (parser == null) {
      parser = this.parser;
    }

    // The parser 'wt=' and 'version=' params are used instead of the original
    // params
    final ModifiableSolrParams wparams = new ModifiableSolrParams(params);
    if (parser != null) {
      wparams.set(CommonParams.WT, parser.getWriterType());
      wparams.set(CommonParams.VERSION, parser.getVersion());
    }
    if (invariantParams != null) {
      wparams.add(invariantParams);
    }

    String basePath = baseUrl;
    if (collection != null)
      basePath += "/" + collection;

    if (request instanceof V2Request) {
      if (System.getProperty("solr.v2RealPath") == null) {
        basePath = baseUrl.replace("/solr", "/api");
      } else {
        basePath = baseUrl + "/____v2";
      }
    }

    if (SolrRequest.METHOD.GET == request.getMethod()) {
      if (streams != null || contentWriter != null) {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "GET can't send streams!");
      }

      return new HttpGet(basePath + path + toQueryString(wparams, false));
    }

    if (SolrRequest.METHOD.DELETE == request.getMethod()) {
      return new HttpDelete(basePath + path + toQueryString(wparams, false));
    }

    if (SolrRequest.METHOD.POST == request.getMethod() || SolrRequest.METHOD.PUT == request.getMethod()) {

      // System.out.println("Post or put");
      final String url = basePath + path;

      // UpdateRequest uses PUT now, and ContentStreamUpdateHandler uses POST.
      // We must override PUT with POST if multipart is required.
      // If useMultipart is on, we fall back to getting streams directly from
      // the request, for now.
      final String contentWriterUrl = url + toQueryString(wparams, false);

      final boolean isMultipart;
      if (this.useMultiPartPost) {
        final Collection<ContentStream> requestStreams = request.getContentStreams();
        // Do we have streams?
        if (requestStreams != null && requestStreams.size() > 0) {

          // Need to know if we have a stream name
          boolean hasNullStreamName = false;
          if (requestStreams != null) {
            for (final ContentStream cs : requestStreams) {
              if (cs.getName() == null) {
                hasNullStreamName = true;
                break;
              }
            }
          }

          // Also, is the contentWriter URL too big?
          final boolean urlTooBig = contentWriterUrl.length() > 4000;
          // System.out.println("RequestStreams present? "+(requestStreams !=
          // null && requestStreams.size() > 0)+"; hasNullStreamName?
          // "+hasNullStreamName+"; url length = "+contentWriterUrl.length());
          isMultipart = requestStreams != null && requestStreams.size() > 0 && ((request.getMethod() == SolrRequest.METHOD.POST && !hasNullStreamName) || urlTooBig);
          if (isMultipart) {
            // System.out.println("Overriding with multipart post");
            streams = requestStreams;
          }
        } else {
          isMultipart = false;
        }
      } else {
        isMultipart = false;
      }

      final SolrRequest.METHOD methodToUse = isMultipart ? SolrRequest.METHOD.POST : request.getMethod();

      /*
       * final boolean isMultipart = ((this.useMultiPartPost &&
       * SolrRequest.METHOD.POST == methodToUse) || (streams != null &&
       * streams.size() > 1)) && !hasNullStreamName;
       */
      // System.out.println("isMultipart = "+isMultipart);

      final LinkedList<NameValuePair> postOrPutParams = new LinkedList<>();

      if (contentWriter != null && !isMultipart) {
        // System.out.println(" using contentwriter");
        final String fullQueryUrl = contentWriterUrl;
        final HttpEntityEnclosingRequestBase postOrPut = SolrRequest.METHOD.POST == methodToUse ? new HttpPost(fullQueryUrl) : new HttpPut(fullQueryUrl);
        postOrPut.addHeader("Content-Type", contentWriter.getContentType());
        postOrPut.setEntity(new BasicHttpEntity() {
          @Override
          public boolean isStreaming() {
            return true;
          }

          @Override
          public void writeTo(final OutputStream outstream) throws IOException {
            contentWriter.write(outstream);
          }
        });
        return postOrPut;

      } else if (streams == null || isMultipart) {
        // send server list and request list as query string params
        final ModifiableSolrParams queryParams = calculateQueryParams(getQueryParams(), wparams);
        queryParams.add(calculateQueryParams(request.getQueryParams(), wparams));
        final String fullQueryUrl = url + toQueryString(queryParams, false);
        final HttpEntityEnclosingRequestBase postOrPut = fillContentStream(methodToUse, streams, wparams, isMultipart, postOrPutParams, fullQueryUrl);
        return postOrPut;
      }
      // It is has one stream, it is the post body, put the params in the URL
      else {
        final String fullQueryUrl = url + toQueryString(wparams, false);
        final HttpEntityEnclosingRequestBase postOrPut = SolrRequest.METHOD.POST == methodToUse ? new HttpPost(fullQueryUrl) : new HttpPut(fullQueryUrl);
        fillSingleContentStream(streams, postOrPut);

        return postOrPut;
      }
    }

    throw new SolrServerException("Unsupported method: " + request.getMethod());

  }

  private void fillSingleContentStream(final Collection<ContentStream> streams, final HttpEntityEnclosingRequestBase postOrPut) throws IOException {
    // Single stream as body
    // Using a loop just to get the first one
    final ContentStream[] contentStream = new ContentStream[1];
    for (final ContentStream content : streams) {
      contentStream[0] = content;
      break;
    }
    final Long size = contentStream[0].getSize();
    postOrPut.setEntity(new InputStreamEntity(contentStream[0].getStream(), size == null ? -1 : size) {
      @Override
      public Header getContentType() {
        return new BasicHeader("Content-Type", contentStream[0].getContentType());
      }

      @Override
      public boolean isRepeatable() {
        return false;
      }
    });

  }

  private HttpEntityEnclosingRequestBase fillContentStream(final SolrRequest.METHOD methodToUse, final Collection<ContentStream> streams, final ModifiableSolrParams wparams, final boolean isMultipart, final LinkedList<NameValuePair> postOrPutParams,
      final String fullQueryUrl) throws IOException {
    final HttpEntityEnclosingRequestBase postOrPut = SolrRequest.METHOD.POST == methodToUse ? new HttpPost(fullQueryUrl) : new HttpPut(fullQueryUrl);

    if (!isMultipart) {
      postOrPut.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }

    final List<FormBodyPart> parts = new LinkedList<>();
    final Iterator<String> iter = wparams.getParameterNamesIterator();
    while (iter.hasNext()) {
      final String p = iter.next();
      final String[] vals = wparams.getParams(p);
      if (vals != null) {
        for (final String v : vals) {
          if (isMultipart) {
            parts.add(new FormBodyPart(p, new StringBody(v, StandardCharsets.UTF_8)));
          } else {
            postOrPutParams.add(new BasicNameValuePair(p, v));
          }
        }
      }
    }

    // TODO: remove deprecated - first simple attempt failed, see {@link
    // MultipartEntityBuilder}
    if (isMultipart && streams != null) {
      for (final ContentStream content : streams) {
        String contentType = content.getContentType();
        if (contentType == null) {
          contentType = BinaryResponseParser.BINARY_CONTENT_TYPE; // default
        }
        String name = content.getName();
        if (name == null) {
          name = "";
        }
        parts.add(new FormBodyPart(encodeForHeader(name), new InputStreamBody(content.getStream(), ContentType.parse(contentType), encodeForHeader(content.getName()))));
      }
    }

    // System.out.println("Using multipart post!");
    if (parts.size() > 0) {
      final ModifiedMultipartEntity entity = new ModifiedMultipartEntity(HttpMultipartMode.STRICT, null, StandardCharsets.UTF_8);
      // MultipartEntity entity = new MultipartEntity(HttpMultipartMode.STRICT);
      for (final FormBodyPart p : parts) {
        entity.addPart(p);
      }
      postOrPut.setEntity(entity);
    } else {
      // not using multipart
      postOrPut.setEntity(new UrlEncodedFormEntity(postOrPutParams, StandardCharsets.UTF_8));
    }
    return postOrPut;
  }

  public static String toQueryString(final SolrParams params, final boolean xml) {
    final StringBuilder sb = new StringBuilder(128);
    try {
      final String amp = xml ? "&amp;" : "&";
      boolean first = true;
      final Iterator<String> names = params.getParameterNamesIterator();
      while (names.hasNext()) {
        final String key = names.next();
        final String[] valarr = params.getParams(key);
        if (valarr == null) {
          sb.append(first ? "?" : amp);
          sb.append(URLEncoder.encode(key, "UTF-8"));
          first = false;
        } else {
          for (final String val : valarr) {
            sb.append(first ? "?" : amp);
            sb.append(key);
            if (val != null) {
              sb.append('=');
              sb.append(URLEncoder.encode(val, "UTF-8"));
            }
            first = false;
          }
        }
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    } // can't happen
    return sb.toString();
  }

  // This is a hack added by KDW on 6/21/2017 because HttpClient doesn't do any
  // character
  // escaping when it puts together header and file names
  private static String encodeForHeader(final String headerName) {
    if (headerName == null) {
      return null;
    }
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < headerName.length(); i++) {
      final char x = headerName.charAt(i);
      if (x == '"' || x == '\\' || x == '\r') {
        sb.append("\\");
      }
      sb.append(x);
    }
    return sb.toString();
  }

}
