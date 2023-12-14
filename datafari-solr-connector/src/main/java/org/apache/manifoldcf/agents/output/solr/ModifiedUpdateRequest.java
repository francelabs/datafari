package org.apache.manifoldcf.agents.output.solr;

import static org.apache.solr.common.params.ShardParams._ROUTE_;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.DocRouter;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.UpdateParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.XML;

public class ModifiedUpdateRequest extends AbstractUpdateRequest {

  public static final String REPFACT = "rf";
  public static final String VER = "ver";
  public static final String OVERWRITE = "ow";
  public static final String COMMIT_WITHIN = "cw";
  private Map<SolrInputDocument, Map<String, Object>> documents = null;
  private Iterator<SolrInputDocument> docIterator = null;
  private Map<String, Map<String, Object>> deleteById = null;
  private List<String> deleteQuery = null;

  private boolean isLastDocInBatch = false;

  public ModifiedUpdateRequest() {
    super(METHOD.POST, "/update");
  }

  public ModifiedUpdateRequest(final String url) {
    super(METHOD.POST, url);
  }

  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------

  /** clear the pending documents and delete commands */
  public void clear() {
    if (documents != null) {
      documents.clear();
    }
    if (deleteById != null) {
      deleteById.clear();
    }
    if (deleteQuery != null) {
      deleteQuery.clear();
    }
  }

  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------

  /**
   * Add a SolrInputDocument to this request
   *
   * @throws NullPointerException if the document is null
   */
  public ModifiedUpdateRequest add(final SolrInputDocument doc) {
    Objects.requireNonNull(doc, "Cannot add a null SolrInputDocument");
    if (documents == null) {
      documents = new LinkedHashMap<>();
    }
    documents.put(doc, null);
    return this;
  }

  public ModifiedUpdateRequest add(final String... fields) {
    return add(new SolrInputDocument(fields));
  }

  /**
   * Add a SolrInputDocument to this request
   *
   * @param doc       the document
   * @param overwrite true if the document should overwrite existing docs with the same id
   * @throws NullPointerException if the document is null
   */
  public ModifiedUpdateRequest add(final SolrInputDocument doc, final Boolean overwrite) {
    return add(doc, null, overwrite);
  }

  /**
   * Add a SolrInputDocument to this request
   *
   * @param doc          the document
   * @param commitWithin the time horizon by which the document should be committed (in ms)
   * @throws NullPointerException if the document is null
   */
  public ModifiedUpdateRequest add(final SolrInputDocument doc, final Integer commitWithin) {
    return add(doc, commitWithin, null);
  }

  /**
   * Add a SolrInputDocument to this request
   *
   * @param doc          the document
   * @param commitWithin the time horizon by which the document should be committed (in ms)
   * @param overwrite    true if the document should overwrite existing docs with the same id
   * @throws NullPointerException if the document is null
   */
  public ModifiedUpdateRequest add(final SolrInputDocument doc, final Integer commitWithin, final Boolean overwrite) {
    Objects.requireNonNull(doc, "Cannot add a null SolrInputDocument");
    if (documents == null) {
      documents = new LinkedHashMap<>();
    }
    final Map<String, Object> params = new HashMap<>(2);
    if (commitWithin != null)
      params.put(COMMIT_WITHIN, commitWithin);
    if (overwrite != null)
      params.put(OVERWRITE, overwrite);

    documents.put(doc, params);

    return this;
  }

  /**
   * Add a collection of SolrInputDocuments to this request
   *
   * @throws NullPointerException if any of the documents in the collection are null
   */
  public ModifiedUpdateRequest add(final Collection<SolrInputDocument> docs) {
    if (documents == null) {
      documents = new LinkedHashMap<>();
    }
    for (final SolrInputDocument doc : docs) {
      Objects.requireNonNull(doc, "Cannot add a null SolrInputDocument");
      documents.put(doc, null);
    }
    return this;
  }

  public ModifiedUpdateRequest deleteById(final String id) {
    if (deleteById == null) {
      deleteById = new LinkedHashMap<>();
    }
    deleteById.put(id, null);
    return this;
  }

  public ModifiedUpdateRequest deleteById(final String id, final String route) {
    return deleteById(id, route, null);
  }

  public ModifiedUpdateRequest deleteById(final String id, final String route, final Long version) {
    if (deleteById == null) {
      deleteById = new LinkedHashMap<>();
    }
    final Map<String, Object> params = (route == null && version == null) ? null : new HashMap<>(1);
    if (version != null)
      params.put(VER, version);
    if (route != null)
      params.put(_ROUTE_, route);
    deleteById.put(id, params);
    return this;
  }

  public ModifiedUpdateRequest deleteById(final List<String> ids) {
    if (deleteById == null) {
      deleteById = new LinkedHashMap<>();
    }

    for (final String id : ids) {
      deleteById.put(id, null);
    }

    return this;
  }

  public ModifiedUpdateRequest deleteById(final String id, final Long version) {
    return deleteById(id, null, version);
  }

  public ModifiedUpdateRequest deleteByQuery(final String q) {
    if (deleteQuery == null) {
      deleteQuery = new ArrayList<>();
    }
    deleteQuery.add(q);
    return this;
  }

  public ModifiedUpdateRequest withRoute(final String route) {
    if (params == null)
      params = new ModifiableSolrParams();
    params.set(_ROUTE_, route);
    return this;
  }

  public UpdateResponse commit(final SolrClient client, final String collection) throws IOException, SolrServerException {
    if (params == null)
      params = new ModifiableSolrParams();
    params.set(UpdateParams.COMMIT, "true");
    return process(client, collection);
  }

  private interface ReqSupplier<T extends ModifiedLBSolrClient.Req> {
    T get(ModifiedUpdateRequest request, List<String> servers);
  }

  private <T extends ModifiedLBSolrClient.Req> Map<String, T> getRoutes(final DocRouter router, final DocCollection col, final Map<String, List<String>> urlMap, final ModifiableSolrParams params,
      final String idField, final ReqSupplier<T> reqSupplier) {
    if ((documents == null || documents.size() == 0) && (deleteById == null || deleteById.size() == 0)) {
      return null;
    }

    final Map<String, T> routes = new HashMap<>();
    if (documents != null) {
      final Set<Entry<SolrInputDocument, Map<String, Object>>> entries = documents.entrySet();
      for (final Entry<SolrInputDocument, Map<String, Object>> entry : entries) {
        final SolrInputDocument doc = entry.getKey();
        final Object id = doc.getFieldValue(idField);
        if (id == null) {
          return null;
        }
        final Slice slice = router.getTargetSlice(id.toString(), doc, null, null, col);
        if (slice == null) {
          return null;
        }
        final List<String> urls = urlMap.get(slice.getName());
        if (urls == null) {
          return null;
        }
        final String leaderUrl = urls.get(0);
        T request = routes.get(leaderUrl);
        if (request == null) {
          final ModifiedUpdateRequest updateRequest = new ModifiedUpdateRequest();
          updateRequest.setMethod(getMethod());
          updateRequest.setCommitWithin(getCommitWithin());
          updateRequest.setParams(params);
          updateRequest.setPath(getPath());
          updateRequest.setBasicAuthCredentials(getBasicAuthUser(), getBasicAuthPassword());
          updateRequest.setResponseParser(getResponseParser());
          request = reqSupplier.get(updateRequest, urls);
          routes.put(leaderUrl, request);
        }
        final ModifiedUpdateRequest urequest = (ModifiedUpdateRequest) request.getRequest();
        final Map<String, Object> value = entry.getValue();
        Boolean ow = null;
        if (value != null) {
          ow = (Boolean) value.get(OVERWRITE);
        }
        if (ow != null) {
          urequest.add(doc, ow);
        } else {
          urequest.add(doc);
        }
      }
    }

    // Route the deleteById's

    if (deleteById != null) {

      final Iterator<Map.Entry<String, Map<String, Object>>> entries = deleteById.entrySet().iterator();
      while (entries.hasNext()) {

        final Map.Entry<String, Map<String, Object>> entry = entries.next();

        final String deleteId = entry.getKey();
        final Map<String, Object> map = entry.getValue();
        Long version = null;
        String route = null;
        if (map != null) {
          version = (Long) map.get(VER);
          route = (String) map.get(_ROUTE_);
        }
        final Slice slice = router.getTargetSlice(deleteId, null, route, null, col);
        if (slice == null) {
          return null;
        }
        final List<String> urls = urlMap.get(slice.getName());
        if (urls == null) {
          return null;
        }
        final String leaderUrl = urls.get(0);
        T request = routes.get(leaderUrl);
        if (request != null) {
          final ModifiedUpdateRequest urequest = (ModifiedUpdateRequest) request.getRequest();
          urequest.deleteById(deleteId, route, version);
        } else {
          final ModifiedUpdateRequest urequest = new ModifiedUpdateRequest();
          urequest.setParams(params);
          urequest.deleteById(deleteId, route, version);
          urequest.setCommitWithin(getCommitWithin());
          urequest.setBasicAuthCredentials(getBasicAuthUser(), getBasicAuthPassword());
          request = reqSupplier.get(urequest, urls);
          routes.put(leaderUrl, request);
        }
      }
    }

    return routes;
  }

  /**
   * @param router  to route updates with
   * @param col     DocCollection for the updates
   * @param urlMap  of the cluster
   * @param params  params to use
   * @param idField the id field
   * @return a Map of urls to requests
   */
  public Map<String, ModifiedLBSolrClient.Req> getRoutesToCollection(final DocRouter router, final DocCollection col, final Map<String, List<String>> urlMap, final ModifiableSolrParams params,
      final String idField) {
    return getRoutes(router, col, urlMap, params, idField, ModifiedLBSolrClient.Req::new);
  }

  public void setDocIterator(final Iterator<SolrInputDocument> docIterator) {
    this.docIterator = docIterator;
  }

  public void setDeleteQuery(final List<String> deleteQuery) {
    this.deleteQuery = deleteQuery;
  }

  // --------------------------------------------------------------------------
  // --------------------------------------------------------------------------

  @Override
  public Collection<ContentStream> getContentStreams() throws IOException {
    return ClientUtils.toContentStreams(getXML(), ClientUtils.TEXT_XML);
  }

  public String getXML() throws IOException {
    final StringWriter writer = new StringWriter();
    writeXML(writer);
    writer.flush();

    // If action is COMMIT or OPTIMIZE, it is sent with params
    final String xml = writer.toString();
    // System.out.println( "SEND:"+xml );
    return (xml.length() > 0) ? xml : null;
  }

  private List<Map<SolrInputDocument, Map<String, Object>>> getDocLists(final Map<SolrInputDocument, Map<String, Object>> documents) {
    final List<Map<SolrInputDocument, Map<String, Object>>> docLists = new ArrayList<>();
    Map<SolrInputDocument, Map<String, Object>> docList = null;
    if (this.documents != null) {

      Boolean lastOverwrite = true;
      Integer lastCommitWithin = -1;

      final Set<Entry<SolrInputDocument, Map<String, Object>>> entries = this.documents.entrySet();
      for (final Entry<SolrInputDocument, Map<String, Object>> entry : entries) {
        final Map<String, Object> map = entry.getValue();
        Boolean overwrite = null;
        Integer commitWithin = null;
        if (map != null) {
          overwrite = (Boolean) entry.getValue().get(OVERWRITE);
          commitWithin = (Integer) entry.getValue().get(COMMIT_WITHIN);
        }
        if (!Objects.equals(overwrite, lastOverwrite) || !Objects.equals(commitWithin, lastCommitWithin) || docLists.isEmpty()) {
          docList = new LinkedHashMap<>();
          docLists.add(docList);
        }
        docList.put(entry.getKey(), entry.getValue());
        lastCommitWithin = commitWithin;
        lastOverwrite = overwrite;
      }
    }

    if (docIterator != null) {
      docList = new LinkedHashMap<>();
      docLists.add(docList);
      while (docIterator.hasNext()) {
        final SolrInputDocument doc = docIterator.next();
        if (doc != null) {
          docList.put(doc, null);
        }
      }
    }

    return docLists;
  }

  /**
   * @since solr 1.4
   */
  public ModifiedUpdateRequest writeXML(final Writer writer) throws IOException {
    final List<Map<SolrInputDocument, Map<String, Object>>> getDocLists = getDocLists(documents);

    for (final Map<SolrInputDocument, Map<String, Object>> docs : getDocLists) {

      if ((docs != null && docs.size() > 0)) {
        final Entry<SolrInputDocument, Map<String, Object>> firstDoc = docs.entrySet().iterator().next();
        final Map<String, Object> map = firstDoc.getValue();
        Integer cw = null;
        Boolean ow = null;
        if (map != null) {
          cw = (Integer) firstDoc.getValue().get(COMMIT_WITHIN);
          ow = (Boolean) firstDoc.getValue().get(OVERWRITE);
        }
        if (ow == null)
          ow = true;
        final int commitWithin = (cw != null && cw != -1) ? cw : this.commitWithin;
        final boolean overwrite = ow;
        if (commitWithin > -1 || overwrite != true) {
          writer.write("<add commitWithin=\"" + commitWithin + "\" " + "overwrite=\"" + overwrite + "\">");
        } else {
          writer.write("<add>");
        }

        final Set<Entry<SolrInputDocument, Map<String, Object>>> entries = docs.entrySet();
        for (final Entry<SolrInputDocument, Map<String, Object>> entry : entries) {
          ClientUtils.writeXML(entry.getKey(), writer);
        }

        writer.write("</add>");
      }
    }

    // Add the delete commands
    final boolean deleteI = deleteById != null && deleteById.size() > 0;
    final boolean deleteQ = deleteQuery != null && deleteQuery.size() > 0;
    if (deleteI || deleteQ) {
      if (commitWithin > 0) {
        writer.append("<delete commitWithin=\"").append(String.valueOf(commitWithin)).append("\">");
      } else {
        writer.append("<delete>");
      }
      if (deleteI) {
        for (final Map.Entry<String, Map<String, Object>> entry : deleteById.entrySet()) {
          writer.append("<id");
          final Map<String, Object> map = entry.getValue();
          if (map != null) {
            final Long version = (Long) map.get(VER);
            final String route = (String) map.get(_ROUTE_);
            if (version != null) {
              writer.append(" version=\"").append(String.valueOf(version)).append('"');
            }

            if (route != null) {
              writer.append(" _route_=\"").append(route).append('"');
            }
          }
          writer.append(">");

          XML.escapeCharData(entry.getKey(), writer);
          writer.append("</id>");
        }
      }
      if (deleteQ) {
        for (final String q : deleteQuery) {
          writer.append("<query>");
          XML.escapeCharData(q, writer);
          writer.append("</query>");
        }
      }
      writer.append("</delete>");
    }
    return this;
  }

  // --------------------------------------------------------------------------
  // --------------------------------------------------------------------------

  // --------------------------------------------------------------------------
  //
  // --------------------------------------------------------------------------

  public List<SolrInputDocument> getDocuments() {
    if (documents == null)
      return null;
    final List<SolrInputDocument> docs = new ArrayList<>(documents.size());
    docs.addAll(documents.keySet());
    return docs;
  }

  public Map<SolrInputDocument, Map<String, Object>> getDocumentsMap() {
    return documents;
  }

  public Iterator<SolrInputDocument> getDocIterator() {
    return docIterator;
  }

  public List<String> getDeleteById() {
    if (deleteById == null)
      return null;
    final List<String> deletes = new ArrayList<>(deleteById.keySet());
    return deletes;
  }

  public Map<String, Map<String, Object>> getDeleteByIdMap() {
    return deleteById;
  }

  public List<String> getDeleteQuery() {
    return deleteQuery;
  }

  public boolean isLastDocInBatch() {
    return isLastDocInBatch;
  }

  public void lastDocInBatch() {
    isLastDocInBatch = true;
  }

}
