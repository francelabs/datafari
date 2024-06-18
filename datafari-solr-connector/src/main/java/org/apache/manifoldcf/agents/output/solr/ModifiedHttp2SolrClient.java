package org.apache.manifoldcf.agents.output.solr;

import static org.apache.solr.common.util.Utils.getObjectByPath;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.V2RequestSupport;
import org.apache.solr.client.solrj.embedded.SSLConfig;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteExecutionException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.BinaryResponseParser;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpListenerFactory;
import org.apache.solr.client.solrj.impl.InputStreamResponseParser;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.V2Request;
import org.apache.solr.client.solrj.util.AsyncListener;
import org.apache.solr.client.solrj.util.Cancellable;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.StringUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.UpdateParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.ObjectReleaseTracker;
import org.apache.solr.common.util.SolrNamedThreadFactory;
import org.apache.solr.common.util.Utils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.ProtocolHandlers;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModifiedHttp2SolrClient extends SolrClient {

  private static final long serialVersionUID = -869785058825555540L;

  public static final String REQ_PRINCIPAL_KEY = "solr-req-principal";
  private final boolean useMultiPartPost = true;

  private static volatile SSLConfig defaultSSLConfig;

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String AGENT = "Solr[" + ModifiedHttp2SolrClient.class.getName() + "] 2.0";
  private static final Charset FALLBACK_CHARSET = StandardCharsets.UTF_8;
  private static final String DEFAULT_PATH = "/select";
  private static final List<String> errPath = Arrays.asList("metadata", "error-class");

  private HttpClient httpClient;
  private volatile Set<String> queryParams = Collections.emptySet();
  private int idleTimeout;

  private ResponseParser parser = new BinaryResponseParser();
  private volatile RequestWriter requestWriter = new BinaryRequestWriter();
  private final List<HttpListenerFactory> listenerFactory = new LinkedList<>();
  private final AsyncTracker asyncTracker = new AsyncTracker();
  /** The URL of the Solr server. */
  private String serverBaseUrl;

  private boolean closeClient;
  private ExecutorService executor;
  private boolean shutdownExecutor;

  private final String basicAuthAuthorizationStr;

  protected ModifiedHttp2SolrClient(String serverBaseUrl, final Builder builder) {
    if (serverBaseUrl != null) {
      if (!serverBaseUrl.equals("/") && serverBaseUrl.endsWith("/")) {
        serverBaseUrl = serverBaseUrl.substring(0, serverBaseUrl.length() - 1);
      }

      if (serverBaseUrl.startsWith("//")) {
        serverBaseUrl = serverBaseUrl.substring(1, serverBaseUrl.length());
      }
      this.serverBaseUrl = serverBaseUrl;
    }

    if (builder.idleTimeout != null && builder.idleTimeout > 0)
      idleTimeout = builder.idleTimeout;
    else
      idleTimeout = HttpClientUtil.DEFAULT_SO_TIMEOUT;

    if (builder.http2SolrClient == null) {
      httpClient = createHttpClient(builder);
      closeClient = true;
    } else {
      httpClient = builder.http2SolrClient.httpClient;
    }
    if (builder.basicAuthUser != null && builder.basicAuthPassword != null) {
      basicAuthAuthorizationStr = basicAuthCredentialsToAuthorizationString(builder.basicAuthUser, builder.basicAuthPassword);
    } else {
      basicAuthAuthorizationStr = null;
    }
    assert ObjectReleaseTracker.track(this);
  }

  public void addListenerFactory(final HttpListenerFactory factory) {
    this.listenerFactory.add(factory);
  }

  // internal usage only
  HttpClient getHttpClient() {
    return httpClient;
  }

  // internal usage only
  ProtocolHandlers getProtocolHandlers() {
    return httpClient.getProtocolHandlers();
  }

  private HttpClient createHttpClient(final Builder builder) {
    HttpClient httpClient;

    executor = builder.executor;
    if (executor == null) {
      final BlockingArrayQueue<Runnable> queue = new BlockingArrayQueue<>(256, 256);
      this.executor = new ExecutorUtil.MDCAwareThreadPoolExecutor(32, 256, 60, TimeUnit.SECONDS, queue, new SolrNamedThreadFactory("h2sc"));
      shutdownExecutor = true;
    } else {
      shutdownExecutor = false;
    }

    SslContextFactory.Client sslContextFactory;
    boolean sslEnabled;
    if (builder.sslConfig == null) {
      sslEnabled = System.getProperty("javax.net.ssl.keyStore") != null || System.getProperty("javax.net.ssl.trustStore") != null;
      sslContextFactory = sslEnabled ? getDefaultSslContextFactory() : null;
    } else {
      sslContextFactory = builder.sslConfig.createClientContextFactory();
      sslEnabled = true;
    }

    HttpClientTransport transport;
    if (builder.useHttp1_1) {
      if (log.isDebugEnabled()) {
        log.debug("Create Http2SolrClient with HTTP/1.1 transport");
      }
      transport = new HttpClientTransportOverHTTP(2);
      httpClient = sslEnabled ? new HttpClient(transport, sslContextFactory) : new HttpClient(transport, null);
      if (builder.maxConnectionsPerHost != null)
        httpClient.setMaxConnectionsPerDestination(builder.maxConnectionsPerHost);
    } else {
      log.debug("Create Http2SolrClient with HTTP/2 transport");
      final HTTP2Client http2client = new HTTP2Client();
      transport = new HttpClientTransportOverHTTP2(http2client);
      httpClient = new HttpClient(transport, sslContextFactory);
      httpClient.setMaxConnectionsPerDestination(4);
    }

    httpClient.setExecutor(this.executor);
    httpClient.setStrictEventOrdering(false);
    httpClient.setConnectBlocking(true);
    httpClient.setFollowRedirects(false);
    httpClient.setMaxRequestsQueuedPerDestination(asyncTracker.getMaxRequestsQueuedPerDestination());
    httpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, AGENT));

    httpClient.setIdleTimeout(idleTimeout);
    if (builder.connectionTimeout != null)
      httpClient.setConnectTimeout(builder.connectionTimeout);
    try {
      httpClient.start();
    } catch (final Exception e) {
      close(); // make sure we clean up
      throw new RuntimeException(e);
    }

    return httpClient;
  }

  @Override
  public void close() {
    // we wait for async requests, so far devs don't want to give sugar for this
    asyncTracker.waitForComplete();
    try {
      if (closeClient) {
        httpClient.setStopTimeout(1000);
        httpClient.stop();
        httpClient.destroy();
      }
    } catch (final Exception e) {
      throw new RuntimeException("Exception on closing client", e);
    } finally {
      if (shutdownExecutor) {
        ExecutorUtil.shutdownAndAwaitTermination(executor);
      }
    }

    assert ObjectReleaseTracker.release(this);
  }

  public boolean isV2ApiRequest(final SolrRequest<?> request) {
    return request instanceof V2Request || request.getPath().contains("/____v2");
  }

  public long getIdleTimeout() {
    return idleTimeout;
  }

  public static class OutStream implements Closeable {
    private final String origCollection;
    private final ModifiableSolrParams origParams;
    private final OutputStreamContentProvider outProvider;
    private final InputStreamResponseListener responseListener;
    private final boolean isXml;

    public OutStream(final String origCollection, final ModifiableSolrParams origParams, final OutputStreamContentProvider outProvider, final InputStreamResponseListener responseListener,
        final boolean isXml) {
      this.origCollection = origCollection;
      this.origParams = origParams;
      this.outProvider = outProvider;
      this.responseListener = responseListener;
      this.isXml = isXml;
    }

    boolean belongToThisStream(final SolrRequest<?> solrRequest, final String collection) {
      final ModifiableSolrParams solrParams = new ModifiableSolrParams(solrRequest.getParams());
      if (!origParams.toNamedList().equals(solrParams.toNamedList()) || !StringUtils.equals(origCollection, collection)) {
        return false;
      }
      return true;
    }

    public void write(final byte b[]) throws IOException {
      this.outProvider.getOutputStream().write(b);
    }

    public void flush() throws IOException {
      this.outProvider.getOutputStream().flush();
    }

    @Override
    public void close() throws IOException {
      if (isXml) {
        write("</stream>".getBytes(FALLBACK_CHARSET));
      }
      this.outProvider.getOutputStream().close();
    }

    // TODO this class should be hidden
    public InputStreamResponseListener getResponseListener() {
      return responseListener;
    }
  }

  public OutStream initOutStream(final String baseUrl, final UpdateRequest updateRequest, final String collection) throws IOException {
    final String contentType = requestWriter.getUpdateContentType();
    final ModifiableSolrParams origParams = new ModifiableSolrParams(updateRequest.getParams());

    // The parser 'wt=' and 'version=' params are used instead of the
    // original params
    final ModifiableSolrParams requestParams = new ModifiableSolrParams(origParams);
    requestParams.set(CommonParams.WT, parser.getWriterType());
    requestParams.set(CommonParams.VERSION, parser.getVersion());

    String basePath = baseUrl;
    if (collection != null)
      basePath += "/" + collection;
    if (!basePath.endsWith("/"))
      basePath += "/";

    final OutputStreamContentProvider provider = new OutputStreamContentProvider();
    final Request postRequest = httpClient.newRequest(basePath + "update" + requestParams.toQueryString()).method(HttpMethod.POST).header(HttpHeader.CONTENT_TYPE, contentType).content(provider);
    decorateRequest(postRequest, updateRequest);
    final InputStreamResponseListener responseListener = new InputStreamResponseListener();
    postRequest.send(responseListener);

    final boolean isXml = ClientUtils.TEXT_XML.equals(requestWriter.getUpdateContentType());
    final OutStream outStream = new OutStream(collection, origParams, provider, responseListener, isXml);
    if (isXml) {
      outStream.write("<stream>".getBytes(FALLBACK_CHARSET));
    }
    return outStream;
  }

  public void send(final OutStream outStream, final SolrRequest<?> req, final String collection) throws IOException {
    assert outStream.belongToThisStream(req, collection);
    this.requestWriter.write(req, outStream.outProvider.getOutputStream());
    if (outStream.isXml) {
      // check for commit or optimize
      final SolrParams params = req.getParams();
      if (params != null) {
        String fmt = null;
        if (params.getBool(UpdateParams.OPTIMIZE, false)) {
          fmt = "<optimize waitSearcher=\"%s\" />";
        } else if (params.getBool(UpdateParams.COMMIT, false)) {
          fmt = "<commit waitSearcher=\"%s\" />";
        }
        if (fmt != null) {
          final byte[] content = String.format(Locale.ROOT, fmt, params.getBool(UpdateParams.WAIT_SEARCHER, false) + "").getBytes(FALLBACK_CHARSET);
          outStream.write(content);
        }
      }
    }
    outStream.flush();
  }

  @SuppressWarnings("StaticAssignmentOfThrowable")
  private static final Exception CANCELLED_EXCEPTION = new Exception();

  private static final Cancellable FAILED_MAKING_REQUEST_CANCELLABLE = () -> {
  };

  public Cancellable asyncRequest(final SolrRequest<?> solrRequest, final String collection, final AsyncListener<NamedList<Object>> asyncListener) {
    Request req;
    try {
      req = makeRequest(solrRequest, collection);
    } catch (SolrServerException | IOException e) {
      asyncListener.onFailure(e);
      return FAILED_MAKING_REQUEST_CANCELLABLE;
    }
    final ResponseParser parser = solrRequest.getResponseParser() == null ? this.parser : solrRequest.getResponseParser();
    req.onRequestQueued(asyncTracker.queuedListener).onComplete(asyncTracker.completeListener).send(new InputStreamResponseListener() {
      @Override
      public void onHeaders(final Response response) {
        super.onHeaders(response);
        final InputStreamResponseListener listener = this;
        executor.execute(() -> {
          final InputStream is = listener.getInputStream();
          assert ObjectReleaseTracker.track(is);
          try {
            final NamedList<Object> body = processErrorsAndResponse(solrRequest, parser, response, is);
            asyncListener.onSuccess(body);
          } catch (final RemoteSolrException e) {
            if (SolrException.getRootCause(e) != CANCELLED_EXCEPTION) {
              asyncListener.onFailure(e);
            }
          } catch (final SolrServerException e) {
            asyncListener.onFailure(e);
          }
        });
      }

      @Override
      public void onFailure(final Response response, final Throwable failure) {
        super.onFailure(response, failure);
        if (failure != CANCELLED_EXCEPTION) {
          asyncListener.onFailure(new SolrServerException(failure.getMessage(), failure));
        }
      }
    });
    return () -> req.abort(CANCELLED_EXCEPTION);
  }

  @Override
  public NamedList<Object> request(final SolrRequest<?> solrRequest, final String collection) throws SolrServerException, IOException {
    final Request req = makeRequest(solrRequest, collection);
    final ResponseParser parser = solrRequest.getResponseParser() == null ? this.parser : solrRequest.getResponseParser();

    Throwable abortCause = null;
    try {
      final InputStreamResponseListener listener = new InputStreamResponseListener();
      req.send(listener);
      final Response response = listener.get(idleTimeout, TimeUnit.MILLISECONDS);
      final InputStream is = listener.getInputStream();
      assert ObjectReleaseTracker.track(is);
      return processErrorsAndResponse(solrRequest, parser, response, is);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      abortCause = e;
      throw new RuntimeException(e);
    } catch (final TimeoutException e) {
      throw new SolrServerException("Timeout occured while waiting response from server at: " + req.getURI(), e);
    } catch (final ExecutionException e) {
      final Throwable cause = e.getCause();
      abortCause = cause;
      if (cause instanceof ConnectException) {
        throw new SolrServerException("Server refused connection at: " + req.getURI(), cause);
      }
      if (cause instanceof SolrServerException) {
        throw (SolrServerException) cause;
      } else if (cause instanceof IOException) {
        throw new SolrServerException("IOException occured when talking to server at: " + getBaseURL(), cause);
      }
      throw new SolrServerException(cause.getMessage(), cause);
    } catch (SolrServerException | RuntimeException sse) {
      abortCause = sse;
      throw sse;
    } finally {
      if (abortCause != null) {
        req.abort(abortCause);
      }
    }
  }

  private NamedList<Object> processErrorsAndResponse(final SolrRequest<?> solrRequest, final ResponseParser parser, final Response response, final InputStream is) throws SolrServerException {
    final String contentType = response.getHeaders().get(HttpHeader.CONTENT_TYPE);
    String mimeType = null;
    String encoding = null;
    if (contentType != null) {
      mimeType = MimeTypes.getContentTypeWithoutCharset(contentType);
      encoding = MimeTypes.getCharsetFromContentType(contentType);
    }
    return processErrorsAndResponse(response, parser, is, mimeType, encoding, isV2ApiRequest(solrRequest));
  }

  private void setBasicAuthHeader(final SolrRequest<?> solrRequest, final Request req) {
    if (solrRequest.getBasicAuthUser() != null && solrRequest.getBasicAuthPassword() != null) {
      final String encoded = basicAuthCredentialsToAuthorizationString(solrRequest.getBasicAuthUser(), solrRequest.getBasicAuthPassword());
      req.header("Authorization", encoded);
    } else if (basicAuthAuthorizationStr != null) {
      req.header("Authorization", basicAuthAuthorizationStr);
    }
  }

  private String basicAuthCredentialsToAuthorizationString(final String user, final String pass) {
    final String userPass = user + ":" + pass;
    return "Basic " + Base64.getEncoder().encodeToString(userPass.getBytes(FALLBACK_CHARSET));
  }

  private Request makeRequest(final SolrRequest<?> solrRequest, final String collection) throws SolrServerException, IOException {
    final Request req = createRequest(solrRequest, collection);
    decorateRequest(req, solrRequest);
    return req;
  }

  private void decorateRequest(final Request req, final SolrRequest<?> solrRequest) {
    req.header(HttpHeader.ACCEPT_ENCODING, null);
    req.timeout(idleTimeout, TimeUnit.MILLISECONDS);
    if (solrRequest.getUserPrincipal() != null) {
      req.attribute(REQ_PRINCIPAL_KEY, solrRequest.getUserPrincipal());
    }

    setBasicAuthHeader(solrRequest, req);
    for (final HttpListenerFactory factory : listenerFactory) {
      final HttpListenerFactory.RequestResponseListener listener = factory.get();
      listener.onQueued(req);
      req.onRequestBegin(listener);
      req.onComplete(listener);
    }

    final Map<String, String> headers = solrRequest.getHeaders();
    if (headers != null) {
      for (final Map.Entry<String, String> entry : headers.entrySet()) {
        req.header(entry.getKey(), entry.getValue());
      }
    }
  }

  private String changeV2RequestEndpoint(final String basePath) throws MalformedURLException {
    final URL oldURL = new URL(basePath);
    final String newPath = oldURL.getPath().replaceFirst("/solr", "/api");
    return new URL(oldURL.getProtocol(), oldURL.getHost(), oldURL.getPort(), newPath).toString();
  }

  private Request createRequest(SolrRequest<?> solrRequest, final String collection) throws IOException, SolrServerException {
    if (solrRequest.getBasePath() == null && serverBaseUrl == null)
      throw new IllegalArgumentException("Destination node is not provided!");

    if (solrRequest instanceof V2RequestSupport) {
      solrRequest = ((V2RequestSupport) solrRequest).getV2Request();
    }
    final SolrParams params = solrRequest.getParams();
    final RequestWriter.ContentWriter contentWriter = requestWriter.getContentWriter(solrRequest);
    Collection<ContentStream> streams = contentWriter == null ? requestWriter.getContentStreams(solrRequest) : null;
    String path = requestWriter.getPath(solrRequest);
    if (path == null || !path.startsWith("/")) {
      path = DEFAULT_PATH;
    }

    ResponseParser parser = solrRequest.getResponseParser();
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

    // TODO add invariantParams support

    String basePath = solrRequest.getBasePath() == null ? serverBaseUrl : solrRequest.getBasePath();
    if (collection != null)
      basePath += "/" + collection;

    if (solrRequest instanceof V2Request) {
      if (System.getProperty("solr.v2RealPath") == null) {
        basePath = changeV2RequestEndpoint(basePath);
      } else {
        basePath = serverBaseUrl + "/____v2";
      }
    }

    if (SolrRequest.METHOD.GET == solrRequest.getMethod()) {
      if (streams != null || contentWriter != null) {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "GET can't send streams!");
      }

      return httpClient.newRequest(basePath + path + wparams.toQueryString()).method(HttpMethod.GET);
    }

    if (SolrRequest.METHOD.DELETE == solrRequest.getMethod()) {
      return httpClient.newRequest(basePath + path + wparams.toQueryString()).method(HttpMethod.DELETE);
    }

    if (SolrRequest.METHOD.POST == solrRequest.getMethod() || SolrRequest.METHOD.PUT == solrRequest.getMethod()) {

      final String url = basePath + path;
      boolean hasNullStreamName = false;
      if (streams != null) {
        hasNullStreamName = streams.stream().anyMatch(cs -> cs.getName() == null);
      }
      final String contentWriterUrl = url + toQueryString(wparams, false);

//      final boolean isMultipart = streams != null && streams.size() > 1 && !hasNullStreamName;
      boolean isMultipart;
      // If the solrRequest is an UpdateRequest it means it is a commit or a delete request so we must use regular way (SolrJ default one) to set isMultipart
      if (this.useMultiPartPost && !(solrRequest instanceof UpdateRequest)) {
        final Collection<ContentStream> requestStreams = requestWriter.getContentStreams(solrRequest);
        // Do we have streams?
        if (requestStreams != null && requestStreams.size() > 0) {

          // Also, is the contentWriter URL too big?
          final boolean urlTooBig = contentWriterUrl.length() > 4000;
          // System.out.println("RequestStreams present? "+(requestStreams != null && requestStreams.size() > 0)+"; hasNullStreamName? "+hasNullStreamName+"; url length = "+contentWriterUrl.length());
          isMultipart = requestStreams != null && requestStreams.size() > 0 && ((solrRequest.getMethod() == SolrRequest.METHOD.POST && !hasNullStreamName) || urlTooBig);
          if (isMultipart) {
            // System.out.println("Overriding with multipart post");
            streams = requestStreams;
          }
        } else {
          isMultipart = false;
        }
      } else {
        // SolrJ default way to set isMultipart
        isMultipart = streams != null && streams.size() > 1 && !hasNullStreamName;
      }

      final HttpMethod method = SolrRequest.METHOD.POST == solrRequest.getMethod() ? HttpMethod.POST : HttpMethod.PUT;

      if (contentWriter != null && !isMultipart) {
        final Request req = httpClient.newRequest(url + wparams.toQueryString()).method(method);
        final BinaryRequestWriter.BAOS baos = new BinaryRequestWriter.BAOS();
        contentWriter.write(baos);

        // SOLR-16265: TODO reduce memory usage
        return req.content(
            // We're throwing this BAOS away, so no need to copy the byte[], just use the raw buf
            new ByteBufferContentProvider(contentWriter.getContentType(), ByteBuffer.wrap(baos.getbuf(), 0, baos.size())));
      } else if (streams == null || isMultipart) {
        // send server list and request list as query string params
        final ModifiableSolrParams queryParams = calculateQueryParams(this.queryParams, wparams);
        queryParams.add(calculateQueryParams(solrRequest.getQueryParams(), wparams));
        final Request req = httpClient.newRequest(url + queryParams.toQueryString()).method(method);
        return fillContentStream(req, streams, wparams, isMultipart);
      } else {
        // It is has one stream, it is the post body, put the params in the URL
        final ContentStream contentStream = streams.iterator().next();
        return httpClient.newRequest(url + wparams.toQueryString()).method(method).content(new InputStreamContentProvider(contentStream.getStream()), contentStream.getContentType());
      }
    }

    throw new SolrServerException("Unsupported method: " + solrRequest.getMethod());
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

  private Request fillContentStream(final Request req, final Collection<ContentStream> streams, final ModifiableSolrParams wparams, final boolean isMultipart) throws IOException {
    if (isMultipart) {
      // multipart/form-data
      final MultiPartContentProvider content = new MultiPartContentProvider();
      final Iterator<String> iter = wparams.getParameterNamesIterator();
      while (iter.hasNext()) {
        final String key = iter.next();
        final String[] vals = wparams.getParams(key);
        if (vals != null) {
          for (final String val : vals) {
            content.addFieldPart(key, new StringContentProvider(val), null);
          }
        }
      }
      if (streams != null) {
        for (final ContentStream contentStream : streams) {
          String contentType = contentStream.getContentType();
          if (contentType == null) {
            contentType = BinaryResponseParser.BINARY_CONTENT_TYPE; // default
          }
          String name = contentStream.getName();
          if (name == null) {
            name = "";
          }
          final HttpFields fields = new HttpFields();
          fields.add(HttpHeader.CONTENT_TYPE, contentType);
          content.addFilePart(name, contentStream.getName(), new InputStreamContentProvider(contentStream.getStream()), fields);
        }
      }
      req.content(content);
    } else {
      // application/x-www-form-urlencoded
      final Fields fields = new Fields();
      final Iterator<String> iter = wparams.getParameterNamesIterator();
      while (iter.hasNext()) {
        final String key = iter.next();
        final String[] vals = wparams.getParams(key);
        if (vals != null) {
          for (final String val : vals) {
            fields.add(key, val);
          }
        }
      }
      req.content(new FormContentProvider(fields, FALLBACK_CHARSET));
    }

    return req;
  }

  private boolean wantStream(final ResponseParser processor) {
    return processor == null || processor instanceof InputStreamResponseParser;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private NamedList<Object> processErrorsAndResponse(final Response response, final ResponseParser processor, final InputStream is, final String mimeType, final String encoding, final boolean isV2Api)
      throws SolrServerException {
    boolean shouldClose = true;
    try {
      // handle some http level checks before trying to parse the response
      final int httpStatus = response.getStatus();

      switch (httpStatus) {
      case HttpStatus.OK_200:
      case HttpStatus.BAD_REQUEST_400:
      case HttpStatus.CONFLICT_409:
        break;
      case HttpStatus.MOVED_PERMANENTLY_301:
      case HttpStatus.MOVED_TEMPORARILY_302:
        if (!httpClient.isFollowRedirects()) {
          throw new SolrServerException("Server at " + getBaseURL() + " sent back a redirect (" + httpStatus + ").");
        }
        break;
      default:
        if (processor == null || mimeType == null) {
          throw new RemoteSolrException(serverBaseUrl, httpStatus, "non ok status: " + httpStatus + ", message:" + response.getReason(), null);
        }
      }

      if (wantStream(parser)) {
        // no processor specified, return raw stream
        final NamedList<Object> rsp = new NamedList<>();
        rsp.add("stream", is);
        // Only case where stream should not be closed
        shouldClose = false;
        return rsp;
      }

      final String procCt = processor.getContentType();
      if (procCt != null) {
        final String procMimeType = MimeTypes.getContentTypeWithoutCharset(procCt).trim().toLowerCase(Locale.ROOT);
        if (!procMimeType.equals(mimeType)) {
          // unexpected mime type
          final String prefix = "Expected mime type " + procMimeType + " but got " + mimeType + ". ";
          final String exceptionEncoding = encoding != null ? encoding : FALLBACK_CHARSET.name();
          try {
            final ByteArrayOutputStream body = new ByteArrayOutputStream();
//            is.transferTo(body);
            IOUtils.copy(is, body);
            throw new RemoteSolrException(serverBaseUrl, httpStatus, prefix + body.toString(exceptionEncoding), null);
          } catch (final IOException e) {
            throw new RemoteSolrException(serverBaseUrl, httpStatus, "Could not parse response with encoding " + exceptionEncoding, e);
          }
        }
      }

      NamedList<Object> rsp;
      try {
        rsp = processor.processResponse(is, encoding);
      } catch (final Exception e) {
        throw new RemoteSolrException(serverBaseUrl, httpStatus, e.getMessage(), e);
      }

      final Object error = rsp == null ? null : rsp.get("error");
      if (error != null && (String.valueOf(getObjectByPath(error, true, errPath)).endsWith("ExceptionWithErrObject"))) {
        throw RemoteExecutionException.create(serverBaseUrl, rsp);
      }
      if (httpStatus != HttpStatus.OK_200 && !isV2Api) {
        NamedList<String> metadata = null;
        String reason = null;
        try {
          if (error != null) {
            reason = (String) Utils.getObjectByPath(error, false, Collections.singletonList("msg"));
            if (reason == null) {
              reason = (String) Utils.getObjectByPath(error, false, Collections.singletonList("trace"));
            }
            final Object metadataObj = Utils.getObjectByPath(error, false, Collections.singletonList("metadata"));
            if (metadataObj instanceof NamedList) {
              metadata = (NamedList<String>) metadataObj;
            } else if (metadataObj instanceof List) {
              // NamedList parsed as List convert to NamedList again
              final List<Object> list = (List<Object>) metadataObj;
              metadata = new NamedList<>(list.size() / 2);
              for (int i = 0; i < list.size(); i += 2) {
                metadata.add((String) list.get(i), (String) list.get(i + 1));
              }
            } else if (metadataObj instanceof Map) {
              metadata = new NamedList((Map) metadataObj);
            }
          }
        } catch (final Exception ex) {
          /* Ignored */
        }
        if (reason == null) {
          final StringBuilder msg = new StringBuilder();
          msg.append(response.getReason()).append("\n\n").append("request: ").append(response.getRequest().getMethod());
          try {
            reason = java.net.URLDecoder.decode(msg.toString(), FALLBACK_CHARSET.name());
          } catch (final UnsupportedEncodingException e) {
            // Quiet
          }
        }
        final RemoteSolrException rss = new RemoteSolrException(serverBaseUrl, httpStatus, reason, null);
        if (metadata != null)
          rss.setMetadata(metadata);
        throw rss;
      }
      return rsp;
    } finally {
      if (shouldClose) {
        try {
          is.close();
          assert ObjectReleaseTracker.release(is);
        } catch (final IOException e) {
          // quitely
        }
      }
    }
  }

  public void setRequestWriter(final RequestWriter requestWriter) {
    this.requestWriter = requestWriter;
  }

  protected RequestWriter getRequestWriter() {
    return requestWriter;
  }

  public void setFollowRedirects(final boolean follow) {
    httpClient.setFollowRedirects(follow);
  }

  public String getBaseURL() {
    return serverBaseUrl;
  }

  private static class AsyncTracker {
    private static final int MAX_OUTSTANDING_REQUESTS = 1000;

    // wait for async requests
    private final Phaser phaser;
    // maximum outstanding requests left
    private final Semaphore available;
    private final Request.QueuedListener queuedListener;
    private final Response.CompleteListener completeListener;

    AsyncTracker() {
      // TODO: what about shared instances?
      phaser = new Phaser(1);
      available = new Semaphore(MAX_OUTSTANDING_REQUESTS, false);
      queuedListener = request -> {
        phaser.register();
        try {
          available.acquire();
        } catch (final InterruptedException ignored) {

        }
      };
      completeListener = result -> {
        phaser.arriveAndDeregister();
        available.release();
      };
    }

    int getMaxRequestsQueuedPerDestination() {
      // comfortably above max outstanding requests
      return MAX_OUTSTANDING_REQUESTS * 3;
    }

    public void waitForComplete() {
      phaser.arriveAndAwaitAdvance();
      phaser.arriveAndDeregister();
    }
  }

  public static class Builder {

    private ModifiedHttp2SolrClient http2SolrClient;
    private SSLConfig sslConfig = defaultSSLConfig;
    private Integer idleTimeout;
    private Integer connectionTimeout;
    private Integer maxConnectionsPerHost;
    private String basicAuthUser;
    private String basicAuthPassword;
    private boolean useHttp1_1 = Boolean.getBoolean("solr.http1");
    protected String baseSolrUrl;
    private ExecutorService executor;

    public Builder() {
    }

    public Builder(final String baseSolrUrl) {
      this.baseSolrUrl = baseSolrUrl;
    }

    public ModifiedHttp2SolrClient build() {
      final ModifiedHttp2SolrClient client = new ModifiedHttp2SolrClient(baseSolrUrl, this);
      try {
        httpClientBuilderSetup(client);
      } catch (final RuntimeException e) {
        try {
          client.close();
        } catch (final Exception exceptionOnClose) {
          e.addSuppressed(exceptionOnClose);
        }
        throw e;
      }
      return client;
    }

    private void httpClientBuilderSetup(final ModifiedHttp2SolrClient client) {
      final String factoryClassName = System.getProperty(HttpClientUtil.SYS_PROP_HTTP_CLIENT_BUILDER_FACTORY);
      if (factoryClassName != null) {
        log.debug("Using Http Builder Factory: {}", factoryClassName);
        ModifiedHttpClientBuilderFactory factory;
        try {
          if (factoryClassName.contains("Krb5HttpClientBuilder")) {
            factory = new ModifiedKrb5HttpClientBuilder();
          } else if (factoryClassName.contains("PreemptiveBasicAuthClientBuilderFactory")) {
            factory = new ModifiedPreemptiveBasicAuthClientBuilderFactory();
          } else {
            throw new ClassNotFoundException("factoryClassName");
          }
        } catch (final ClassNotFoundException e) {
          throw new RuntimeException("Unable to instantiate " + Http2SolrClient.class.getName(), e);
        }
        factory.setup(client);
      }
    }

    /** Reuse {@code httpClient} connections pool */
    public Builder withHttpClient(final ModifiedHttp2SolrClient httpClient) {
      this.http2SolrClient = httpClient;
      return this;
    }

    public Builder withExecutor(final ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    public Builder withSSLConfig(final SSLConfig sslConfig) {
      this.sslConfig = sslConfig;
      return this;
    }

    public Builder withBasicAuthCredentials(final String user, final String pass) {
      if (user != null || pass != null) {
        if (user == null || pass == null) {
          throw new IllegalStateException("Invalid Authentication credentials. Either both username and password or none must be provided");
        }
      }
      this.basicAuthUser = user;
      this.basicAuthPassword = pass;
      return this;
    }

    /**
     * Set maxConnectionsPerHost for http1 connections, maximum number http2 connections is limited by 4
     */
    public Builder maxConnectionsPerHost(final int max) {
      this.maxConnectionsPerHost = max;
      return this;
    }

    public Builder idleTimeout(final int idleConnectionTimeout) {
      this.idleTimeout = idleConnectionTimeout;
      return this;
    }

    public Builder useHttp1_1(final boolean useHttp1_1) {
      this.useHttp1_1 = useHttp1_1;
      return this;
    }

    public Builder connectionTimeout(final int connectionTimeOut) {
      this.connectionTimeout = connectionTimeOut;
      return this;
    }
  }

  public Set<String> getQueryParams() {
    return queryParams;
  }

  /**
   * Expert Method
   *
   * @param queryParams set of param keys to only send via the query string Note that the param will be sent as a query string if the key is part of this Set or the SolrRequest's query params.
   * @see org.apache.solr.client.solrj.SolrRequest#getQueryParams
   */
  public void setQueryParams(final Set<String> queryParams) {
    this.queryParams = queryParams;
  }

  private ModifiableSolrParams calculateQueryParams(final Set<String> queryParamNames, final ModifiableSolrParams wparams) {
    final ModifiableSolrParams queryModParams = new ModifiableSolrParams();
    if (queryParamNames != null) {
      for (final String param : queryParamNames) {
        final String[] value = wparams.getParams(param);
        if (value != null) {
          for (final String v : value) {
            queryModParams.add(param, v);
          }
          wparams.remove(param);
        }
      }
    }
    return queryModParams;
  }

  public ResponseParser getParser() {
    return parser;
  }

  public void setParser(final ResponseParser processor) {
    parser = processor;
  }

  public static void setDefaultSSLConfig(final SSLConfig sslConfig) {
    ModifiedHttp2SolrClient.defaultSSLConfig = sslConfig;
  }

  // public for testing, only used by tests
  public static void resetSslContextFactory() {
    ModifiedHttp2SolrClient.defaultSSLConfig = null;
  }

  /* package-private for testing */
  static SslContextFactory.Client getDefaultSslContextFactory() {
    final String checkPeerNameStr = System.getProperty(HttpClientUtil.SYS_PROP_CHECK_PEER_NAME);
    boolean sslCheckPeerName = true;
    if (checkPeerNameStr == null || "false".equalsIgnoreCase(checkPeerNameStr)) {
      sslCheckPeerName = false;
    }

    final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(!sslCheckPeerName);

    if (null != System.getProperty("javax.net.ssl.keyStore")) {
      sslContextFactory.setKeyStorePath(System.getProperty("javax.net.ssl.keyStore"));
    }
    if (null != System.getProperty("javax.net.ssl.keyStorePassword")) {
      sslContextFactory.setKeyStorePassword(System.getProperty("javax.net.ssl.keyStorePassword"));
    }
    if (null != System.getProperty("javax.net.ssl.keyStoreType")) {
      sslContextFactory.setKeyStoreType(System.getProperty("javax.net.ssl.keyStoreType"));
    }
    if (null != System.getProperty("javax.net.ssl.trustStore")) {
      sslContextFactory.setTrustStorePath(System.getProperty("javax.net.ssl.trustStore"));
    }
    if (null != System.getProperty("javax.net.ssl.trustStorePassword")) {
      sslContextFactory.setTrustStorePassword(System.getProperty("javax.net.ssl.trustStorePassword"));
    }
    if (null != System.getProperty("javax.net.ssl.trustStoreType")) {
      sslContextFactory.setTrustStoreType(System.getProperty("javax.net.ssl.trustStoreType"));
    }

    sslContextFactory.setEndpointIdentificationAlgorithm(System.getProperty("solr.jetty.ssl.verifyClientHostName"));

    return sslContextFactory;
  }

}
