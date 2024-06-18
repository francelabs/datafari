package org.apache.manifoldcf.agents.output.solr;

import static org.apache.solr.common.params.CommonParams.ADMIN_PATHS;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient;
import org.apache.solr.client.solrj.request.IsUpdateRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.util.AsyncListener;
import org.apache.solr.client.solrj.util.Cancellable;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.slf4j.MDC;

public class ModifiedLBHttp2SolrClient extends ModifiedLBSolrClient {
  private static final long serialVersionUID = -1147138830059067321L;
  private final ModifiedHttp2SolrClient httpClient;

  public ModifiedLBHttp2SolrClient(final ModifiedHttp2SolrClient httpClient, final String... baseSolrUrls) {
    super(Arrays.asList(baseSolrUrls));
    this.httpClient = httpClient;
  }

  @Override
  protected SolrClient getClient(final String baseUrl) {
    return httpClient;
  }

  @Override
  public void setParser(final ResponseParser parser) {
    super.setParser(parser);
    this.httpClient.setParser(parser);
  }

  @Override
  public void setRequestWriter(final RequestWriter writer) {
    super.setRequestWriter(writer);
    this.httpClient.setRequestWriter(writer);
  }

  @Override
  public void setQueryParams(final Set<String> queryParams) {
    super.setQueryParams(queryParams);
    this.httpClient.setQueryParams(queryParams);
  }

  @Override
  public void addQueryParams(final String queryOnlyParam) {
    super.addQueryParams(queryOnlyParam);
    this.httpClient.setQueryParams(getQueryParams());
  }

  public Cancellable asyncReq(final Req req, final AsyncListener<Rsp> asyncListener) {
    final Rsp rsp = new Rsp();
    final boolean isNonRetryable = req.request instanceof IsUpdateRequest || ADMIN_PATHS.contains(req.request.getPath());
    final ServerIterator it = new ServerIterator(req, zombieServers);
    asyncListener.onStart();
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    final AtomicReference<Cancellable> currentCancellable = new AtomicReference<>();
    final RetryListener retryListener = new RetryListener() {

      @Override
      public void onSuccess(final Rsp rsp) {
        asyncListener.onSuccess(rsp);
      }

      @Override
      public void onFailure(final Exception e, final boolean retryReq) {
        if (retryReq) {
          String url;
          try {
            url = it.nextOrError(e);
          } catch (final SolrServerException ex) {
            asyncListener.onFailure(e);
            return;
          }
          try {
            MDC.put("ModifiedLBSolrClient.url", url);
            synchronized (cancelled) {
              if (cancelled.get()) {
                return;
              }
              final Cancellable cancellable = doRequest(url, req, rsp, isNonRetryable, it.isServingZombieServer(), this);
              currentCancellable.set(cancellable);
            }
          } finally {
            MDC.remove("ModifiedLBSolrClient.url");
          }
        } else {
          asyncListener.onFailure(e);
        }
      }
    };
    try {
      final Cancellable cancellable = doRequest(it.nextOrError(), req, rsp, isNonRetryable, it.isServingZombieServer(), retryListener);
      currentCancellable.set(cancellable);
    } catch (final SolrServerException e) {
      asyncListener.onFailure(e);
    }
    return () -> {
      synchronized (cancelled) {
        cancelled.set(true);
        if (currentCancellable.get() != null) {
          currentCancellable.get().cancel();
        }
      }
    };
  }

  private interface RetryListener {
    void onSuccess(Rsp rsp);

    void onFailure(Exception e, boolean retryReq);
  }

  private Cancellable doRequest(final String baseUrl, final Req req, final Rsp rsp, final boolean isNonRetryable, final boolean isZombie, final RetryListener listener) {
    rsp.server = baseUrl;
    req.getRequest().setBasePath(baseUrl);
    return ((ModifiedHttp2SolrClient) getClient(baseUrl)).asyncRequest(req.getRequest(), null, new AsyncListener<NamedList<Object>>() {
      @Override
      public void onSuccess(final NamedList<Object> result) {
        rsp.rsp = result;
        if (isZombie) {
          zombieServers.remove(baseUrl);
        }
        listener.onSuccess(rsp);
      }

      @Override
      public void onFailure(final Throwable oe) {
        try {
          throw (Exception) oe;
        } catch (final BaseHttpSolrClient.RemoteExecutionException e) {
          listener.onFailure(e, false);
        } catch (final SolrException e) {
          // we retry on 404 or 403 or 503 or 500
          // unless it's an update - then we only retry on connect exception
          if (!isNonRetryable && RETRY_CODES.contains(e.code())) {
            listener.onFailure((!isZombie) ? addZombie(baseUrl, e) : e, true);
          } else {
            // Server is alive but the request was likely malformed or invalid
            if (isZombie) {
              zombieServers.remove(baseUrl);
            }
            listener.onFailure(e, false);
          }
        } catch (final SocketException e) {
          if (!isNonRetryable || e instanceof ConnectException) {
            listener.onFailure((!isZombie) ? addZombie(baseUrl, e) : e, true);
          } else {
            listener.onFailure(e, false);
          }
        } catch (final SocketTimeoutException e) {
          if (!isNonRetryable) {
            listener.onFailure((!isZombie) ? addZombie(baseUrl, e) : e, true);
          } else {
            listener.onFailure(e, false);
          }
        } catch (final SolrServerException e) {
          final Throwable rootCause = e.getRootCause();
          if (!isNonRetryable && rootCause instanceof IOException) {
            listener.onFailure((!isZombie) ? addZombie(baseUrl, e) : e, true);
          } else if (isNonRetryable && rootCause instanceof ConnectException) {
            listener.onFailure((!isZombie) ? addZombie(baseUrl, e) : e, true);
          } else {
            listener.onFailure(e, false);
          }
        } catch (final Exception e) {
          listener.onFailure(new SolrServerException(e), false);
        }
      }
    });
  }

}
