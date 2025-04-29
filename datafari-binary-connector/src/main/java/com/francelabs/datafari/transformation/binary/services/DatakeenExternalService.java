package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.BinaryConfig;
import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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

        String openAiApiKey = spec.getStringProperty(BinaryConfig.NODE_SERVICE_SECURITY_TOKEN);
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new ManifoldCFException("Invalid or empty security token.");
        }
        HttpClient client = HttpClient.newHttpClient();

        String requestBody = "{\"paramDict\":{\"files\":[\"" + base64content  + "\"]}}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.url)
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // Todo : handle specific errors
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response.body();
    }
}