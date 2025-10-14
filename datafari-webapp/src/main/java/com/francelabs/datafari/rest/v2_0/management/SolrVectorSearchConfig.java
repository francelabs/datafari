package com.francelabs.datafari.rest.v2_0.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.models.embeddingmodels.EbdModelConfig;
import com.francelabs.datafari.ai.models.embeddingmodels.EbdModelConfigurationManager;
import com.francelabs.datafari.atomicupdates.AtomicUpdatesJobService;
import com.francelabs.datafari.atomicupdates.JobState;
import com.francelabs.datafari.utils.SolrConfiguration;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "SolrVectorSearchConfig", urlPatterns = "/rest/v2.0/management/solr-vector-search")
public class SolrVectorSearchConfig extends HttpServlet {

    private final ObjectMapper M = new ObjectMapper();

    // Services
    private EbdModelConfigurationManager modelMgr;
    private final AtomicUpdatesJobService jobs = new AtomicUpdatesJobService();

    private final SolrClient solr = getSolrClient();

    private static final String DEFAULT_COLLECTION = "VectorMain";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json;charset=UTF-8");
        modelMgr = new EbdModelConfigurationManager();
        String fn = Optional.ofNullable(req.getParameter("fn")).orElse("status");

        try {
            switch (fn) {
                case "models":
                    write(resp, modelsPayload());
                    break;
                case "status":
                    write(resp, statusPayload(req));
                    break;
                default:
                    error(resp, 400, "Unsupported fn=" + fn);
            }
        } catch (Exception e) {
            error(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        modelMgr = new EbdModelConfigurationManager();
        String fn = Optional.ofNullable(req.getParameter("fn")).orElse("");

        try {
            switch (fn) {
                case "setActiveModel": {
                    Map body = M.readValue(req.getInputStream(), Map.class);
                    String name = Objects.toString(body.get("name"), null);
                    if (name == null || name.isBlank()) { error(resp, 400, "Missing name"); return; }
                    modelMgr.setActiveModel(name);
                    write(resp, Map.of("ok", true));
                    break;
                }
                case "startEmbeddings": {
                    Map body = M.readValue(req.getInputStream(), Map.class);
                    boolean force = Boolean.TRUE.equals(body.get("force"));
                    if (jobs.isRunning()) { error(resp, 409, "A job is already running"); return; }
                    JobState js = (force) ? jobs.startJob("VECTOR_FORCE") : jobs.startJob("VECTOR");
                    write(resp, Map.of("ok", true));
                    break;
                }
                default:
                    error(resp, 400, "Unsupported fn=" + fn);
            }
        } catch (Exception e) {
            error(resp, 500, e.getMessage());
        }
    }

    // -------- Helpers --------

    private Map<String, Object> modelsPayload() {
        EbdModelConfig active = modelMgr.getActiveModelConfig();
        String activeName = active != null ? active.getName() : null;
        String vectorField = active != null ? safeVectorField(active) : null;

        List<Map<String, Object>> models = modelMgr.listModels().stream()
                .map(m -> {
                    Map<String, Object> mm = new LinkedHashMap<>();
                    mm.put("name", m.getName());
                    // mm.put("provider", m.getProvider()); // si dispo
                    return mm;
                }).collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("activeModel", activeName);
        out.put("vectorField", vectorField);
        out.put("models", models);
        return out;
    }

    private Map<String, Object> statusPayload(HttpServletRequest req) throws Exception {
        EbdModelConfig active = modelMgr.getActiveModelConfig();
        String vectorField = active != null ? safeVectorField(active) : null;

        long total = totalDocs();
        long missingVect  = (vectorField == null) ? total : docsNotHavingVector();
        long vect  = total - missingVect;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalDocs", total);
        out.put("vectorizedDocs", vect);
        out.put("vectorField", vectorField);

        return out;
    }

    private long totalDocs() throws SolrServerException, IOException {
        if (solr == null) return 0L;
        SolrQuery q = new SolrQuery("embedded_content:*");
        q.setRows(0);
        QueryResponse rsp = solr.query(DEFAULT_COLLECTION, q);
        return rsp.getResults().getNumFound();
    }

    private long docsNotHavingVector() throws IOException, SolrServerException {
        if (solr == null) return 0L;
        SolrQuery q = new SolrQuery("embedded_content:*");
        q.setRequestHandler("/select/not-embedded");
        q.setRows(0);
        QueryResponse rsp = solr.query(DEFAULT_COLLECTION, q);
        return rsp.getResults().getNumFound();
    }

    private String safeVectorField(EbdModelConfig cfg) {
        try {
            return cfg.getVectorField();
        } catch (Exception e) {
            return null;
        }
    }

    private void write(HttpServletResponse resp, Object payload) throws IOException {
        resp.getWriter().write(M.writeValueAsString(payload));
    }

    private void error(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        write(resp, Map.of("code", code, "message", msg));
    }

    // -----------------
    // Solr Config
    // -----------------


    protected static String getSolrUrl() {
        SolrConfiguration solrConf = SolrConfiguration.getInstance();
        String solrserver = solrConf.getProperty(SolrConfiguration.SOLRHOST, "localhost");
        String solrport = solrConf.getProperty(SolrConfiguration.SOLRPORT, "8983");
        String protocol = solrConf.getProperty(SolrConfiguration.SOLRPROTOCOL, "http");
        return protocol + "://" + solrserver + ":" + solrport + "/solr";
    }
    protected SolrClient getSolrClient(){
        String solrBaseUrls = getSolrUrl();
        final List<String> cloudServers = new ArrayList<>();
        cloudServers.add(solrBaseUrls);
        return new CloudSolrClient.Builder(cloudServers).build();
    }
}
