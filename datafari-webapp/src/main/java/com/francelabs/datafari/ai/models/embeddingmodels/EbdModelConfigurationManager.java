package com.francelabs.datafari.ai.models.embeddingmodels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.francelabs.datafari.utils.SolrConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Manages the loading, saving, and access to LLM model configurations defined in
 * the {@code models.json} file.
 * <p>
 * Provides utility methods to retrieve the active model, query specific models by name,
 * and modify the list of available configurations.
 * </p>
 */
public class EbdModelConfigurationManager {

    private static final Logger LOGGER = LogManager.getLogger(EbdModelConfigurationManager.class.getName());

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("catalina.base"), "conf", "embedding_models.json");
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private EbdModelRegistry registry;

    private static final String SOLR_URL = getSolrUrl();

    /**
     * Constructs a new configuration manager and immediately loads the model configurations
     * from the default JSON file ({@code conf/models.json}).
     *
     * @throws IOException if the file cannot be read or parsed.
     */
    public EbdModelConfigurationManager() {
        try {
            load();
        } catch (Exception e) {
            LOGGER.error("Error in EbdModelConfigurationManager", e);
            this.registry = new EbdModelRegistry();
            this.registry.setModels(new ArrayList<>());
        }
    }

    /**
     * Loads the model configurations from the {@code models.json} file.
     * If the file does not exist, initializes an empty registry.
     *
     * @throws IOException if the file exists but cannot be read or parsed.
     */
    public void load() throws IOException {
        if (Files.exists(CONFIG_PATH)) {
            this.registry = mapper.readValue(CONFIG_PATH.toFile(), EbdModelRegistry.class);
        } else {
            this.registry = new EbdModelRegistry();
            this.registry.setModels(new ArrayList<>());
        }
    }

    /**
     * Persists the current model registry to the {@code models.json} file.
     *
     * @throws IOException if the file cannot be written.
     */
    public void save() throws IOException {
        mapper.writeValue(CONFIG_PATH.toFile(), registry);
    }

    /**
     * Returns the configuration of the active model, as defined in the registry.
     *
     * @return The {@link EbdModelConfig} of the active model, or {@code null} if none is set.
     */
    public EbdModelConfig getActiveModelConfig() {
        return registry.getModels().stream()
                .filter(m -> m.getName().equals(registry.getActiveModel()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the active model by name and saves the updated registry.
     *
     * @param modelName The name of the model to mark as active.
     * @throws IOException if the updated registry cannot be saved.
     */
    public void setActiveModel(String modelName) {
        EbdModelConfig model = (modelName != null) ? getModelByName(modelName) : registry.getModels().getFirst();
        if (model == null && !registry.getModels().isEmpty()) {
            model = registry.getModels().getFirst();
        }

        try {

            if (model == null) {
                registry.setActiveModel(modelName);

                // If there is no model left, we delete the param from Solr config
                JSONObject config = getJsonFromSolr(SOLR_URL + "/VectorMain/config/overlay");
                JSONObject overlay = (JSONObject) config.get("overlay");
                JSONObject userProps = (JSONObject) overlay.get("userProps");
                if (userProps != null) {
                    String currentModel = (String) userProps.getOrDefault("texttovector.model", "");

                    if (modelName.equals(currentModel)) {
                        JSONObject unsetPayload = new JSONObject();
                        unsetPayload.put("unset-user-property", "texttovector.model");
                        postToSolr(SOLR_URL + "/VectorMain/config", unsetPayload);
                    }
                }
                return;
            }

            // Update default model in Solr
            Map<String, Object> propertiesVectorMain = new LinkedHashMap<>(); // VectorMain properties
            propertiesVectorMain.put("texttovector.model", model.getName());
            propertiesVectorMain.put("texttovector.outputfield", model.getVectorField());

            // Update each updated property in VectorMain
            for (Map.Entry<String, Object> entry : propertiesVectorMain.entrySet()) {
                JSONObject inner = new JSONObject();
                inner.put(entry.getKey(), entry.getValue());
                JSONObject payload = new JSONObject();
                payload.put("set-user-property", inner);
                postToSolr(SOLR_URL + "/VectorMain/config", payload);
            }

            registry.setActiveModel(model.getName());
            save();
        } catch (IOException | ParseException e) {
            LOGGER.error("Failed to set default embeddings model: {}", modelName, e);
        }
    }

    /**
     * Adds or updates a model configuration in the registry and persists the change.
     * If a model with the same name already exists, it will be replaced.
     *
     * @param config The new or updated model configuration.
     * @throws IOException if the registry cannot be saved.
     */
    public void addOrUpdateModel(EbdModelConfig config) throws IOException, ParseException {

        // Update model
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String newModelJson = ow.writeValueAsString(config);

            // Retrieve existing model
            JSONObject currentModelStore = getJsonFromSolr(SOLR_URL + "/VectorMain/schema/text-to-vector-model-store");
            JSONArray models = (JSONArray) currentModelStore.get("models");

            String newModelName = config.getName();

            boolean modelExists = false;
            String currentModelJson = null;
            for (Object obj : models) {
                JSONObject model = (JSONObject) obj;
                if (newModelName != null && newModelName.equals(model.get("name"))) {
                    currentModelJson = model.toJSONString();
                    modelExists = true;
                    break;
                }
            }

            // Compare JSON String to check if the model changed
            if (!newModelJson.equals(currentModelJson)) {
                // Delete existing one
                if (modelExists) {
                    deleteFromSolr(SOLR_URL + "/VectorMain/schema/text-to-vector-model-store/" + newModelName);
                }
                // Upload the new one
                putToSolr(SOLR_URL + "/VectorMain/schema/text-to-vector-model-store", newModelJson);
            }

            // Add or replace the model in the registry, then update the JSON
            registry.getModels().removeIf(m -> m.getName().equals(config.getName()));
            registry.getModels().add(config);
            updateRegistry();

        } catch (Exception e) {
            LOGGER.error("Failed to add or update embeddings model:", e);
        }
    }

    /**
     * Adds or updates a model configuration in the registry and persists the change.
     * If a model with the same name already exists, it will be replaced.
     *
     * @param configs A list of models to add or update.
     * @throws IOException if the registry cannot be saved.
     */
    public void addOrUpdateModels(List<EbdModelConfig> configs) throws IOException, ParseException {

        // Update models
        for (EbdModelConfig model : configs) {
            addOrUpdateModel(model);
        }
    }

    /**
     * Removes a model configuration by name.
     * If the removed model was the active model, the active model will be unset.
     *
     * @param modelName The name of the model to remove.
     * @throws IOException if the updated registry cannot be saved.
     */
    public void removeModel(String modelName) throws IOException {

        String deleteUrl = SOLR_URL + "/VectorMain/schema/text-to-vector-model-store/" + modelName;
        deleteFromSolr(deleteUrl);

        if (modelName.equals(registry.getActiveModel())) {
            // If the delete model is the active one, update the active model
            setActiveModel(null);
        }

        registry.getModels().removeIf(m -> m.getName().equals(modelName));
        if (modelName.equals(registry.getActiveModel())) {
            registry.setActiveModel(null);
        }
        save();
    }

    /**
     * Returns the list of all registered model configurations.
     *
     * @return A list of {@link EbdModelConfig} instances.
     */
    public List<EbdModelConfig> listModels() {
        updateRegistry();
        return registry.getModels();
    }

    /**
     * Update the JSON file, based on Solr configuration.
     */
    public void updateRegistry() {
        try {
            List<EbdModelConfig> models = registry.getModels();
            List<EbdModelConfig> updatedModels = new ArrayList<>();

            // Retrieve existing model
            JSONObject currentModelStore = getJsonFromSolr(SOLR_URL + "/VectorMain/schema/text-to-vector-model-store");
            JSONArray solrModels = (JSONArray) currentModelStore.get("models");

            Map<String, String> fieldMap = new HashMap<>();
            for (EbdModelConfig model : models) {
                fieldMap.put(model.getName(), model.getVectorField());
            }

            String activeModel = registry.getActiveModel();
            boolean missingActiveModel = true;

            for (Object obj : solrModels) {
                JSONObject model = (JSONObject) obj;
                String name = (String) model.get("name");
                if (name.equals(activeModel)) {
                    missingActiveModel = false;
                }

                EbdModelConfig solrModel = toEbd(model, mapper);
                solrModel.setVectorField(fieldMap.get(name));
                updatedModels.add(solrModel);
            }

            if (missingActiveModel) registry.setActiveModel(null);

            registry.setModels(updatedModels);
            save();

        } catch (Exception e) {
            LOGGER.error("Embedding models registry could not be updated.", e);
        }
    }

    public static EbdModelConfig toEbd(JSONObject model, ObjectMapper mapper) {
        EbdModelConfig cfg = new EbdModelConfig();
        cfg.setName((String) model.get("name"));
        cfg.setInterfaceType((String) model.get("class"));    // "class" → "interfaceType"
        cfg.setParams(mapper.convertValue(model.get("params"), new TypeReference<Map<String,Object>>() {}));
        return cfg;
    }

    /**
     * Retrieves a model configuration by its name.
     *
     * @param name The name of the model to retrieve.
     * @return The corresponding {@link EbdModelConfig}, or {@code null} if not found.
     */
    public EbdModelConfig getModelByName(String name) {
        return registry.getModels().stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }



    private void disableSSLCertificateChecking() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    // Utils HTTP (GET)
    private JSONObject getJsonFromSolr(String url) throws IOException, ParseException {
        try {
            disableSSLCertificateChecking();
        } catch (Exception e) {
            throw new IOException("Erreur lors de la désactivation de la vérification SSL", e);
        }
        URLConnection conn = new URL(url).openConnection();
        try (InputStream is = conn.getInputStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(reader);
        }
    }

    protected static String getSolrUrl() {
        SolrConfiguration solrConf = SolrConfiguration.getInstance();
        String solrserver = solrConf.getProperty(SolrConfiguration.SOLRHOST, "localhost");
        String solrport = solrConf.getProperty(SolrConfiguration.SOLRPORT, "8983");
        String protocol = solrConf.getProperty(SolrConfiguration.SOLRPROTOCOL, "http");
        return protocol + "://" + solrserver + ":" + solrport + "/solr";
    }

    private void postToSolr(String url, JSONObject payload) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (InputStream is = conn.getInputStream()) {
            IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    private void deleteFromSolr(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Accept", "application/json");
        conn.connect();

        try (InputStream is = conn.getInputStream()) {
            IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    private void putToSolr(String url, String jsonPayload) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }

        try (InputStream is = conn.getInputStream()) {
            IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
