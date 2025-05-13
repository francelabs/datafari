package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.BinaryConfig;
import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import com.francelabs.datafari.transformation.binary.utils.JsonUtils;
import com.francelabs.datafari.transformation.binary.utils.PromptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class DatakeenExternalService extends ExternalService implements IExternalService {

    private static final Logger LOGGER = LogManager.getLogger(DatakeenExternalService.class.getName());

    static final String DEFAULT_URL = "https://api.datakeen.co/api/v1/";
    static final String DEFAULT_ENDPOINT = "/reco/multi-doc";

    public DatakeenExternalService(BinarySpecification spec) {
        // ALWAYS CALL SUPER AT THE BEGINING OF THE CONSTRUCTOR
        super(spec);

        // Default URL
        if (this.url == null) {
            this.url = URI.create(DEFAULT_URL + DEFAULT_ENDPOINT);
        }
    }

    public String invoke(String base64content) throws ManifoldCFException {

        String apiToken = getSecurityToken();
        if (apiToken == null || apiToken.isBlank()) {
            throw new ManifoldCFException("Unable to retrieve security token.");
        }
        HttpClient client = HttpClient.newHttpClient();

        String requestBody = "{\"paramDict\":{\"files\":[\"" + base64content  + "\"]}}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.url)
                .header("Authorization", "Bearer " + apiToken)
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            throw new ManifoldCFException("Unexpected error occured while calling Datakeen services.",e);
        }

        // Error management
        String errorCode = JsonUtils.extractResponse(response.body(), "code");
        if (errorCode != null && !errorCode.isBlank()) {
            String description = JsonUtils.extractResponse(response.body(), "description");
            int code = Integer.parseInt(errorCode);
            throw new ManifoldCFException(description, code);
        }

        return response.body();
    }

    public String getSecurityToken() throws ManifoldCFException {

        String datakeenAiApiKey = spec.getStringProperty(BinaryConfig.NODE_SERVICE_SECURITY_TOKEN);
        if (datakeenAiApiKey != null && !datakeenAiApiKey.isBlank()) return datakeenAiApiKey;

        Map<String, String> parameters = spec.getMapProperty(BinaryConfig.NODE_SERVICE_ADDITIONAL_PARAMETERS);
        String username = parameters.getOrDefault("username", "");
        String password = parameters.getOrDefault("password", "");

        if (username.isEmpty() || password.isEmpty()) {
            throw new ManifoldCFException("Additional parameters 'username' and 'password' are required for this service.");
        }
        HttpClient client = HttpClient.newHttpClient();

        String requestBody =  "{\"paramDict\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.datakeen.co/auth"))
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return JsonUtils.extractResponse(response.body(), "access_token");
    }
}