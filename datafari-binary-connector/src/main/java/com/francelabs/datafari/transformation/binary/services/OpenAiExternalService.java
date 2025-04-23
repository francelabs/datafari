package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.BinaryConfig;
import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.IOException;
import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class OpenAiExternalService extends ExternalService implements IExternalService {

    private static final Logger LOGGER = LogManager.getLogger(OpenAiExternalService.class.getName());

    static final String DEFAULT_URL = "https://api.openai.com/v1/";
    private String maxTokens = "1000";

    public OpenAiExternalService(BinarySpecification spec) {
        super(spec);
        if (spec.getStringProperty(BinaryConfig.NODE_SERVICE_ENDPOINT).isEmpty()) spec.setProperty(BinaryConfig.NODE_SERVICE_ENDPOINT, DEFAULT_URL);

        Map<String, String> params = spec.getMapProperty(BinaryConfig.NODE_SERVICE_ADDITIONAL_PARAMETERS);
        if (params.containsKey("maxTokens")) {
            this.maxTokens = params.get("maxTokens");
        }
    }


    /**
     * Call the external (OpenAI, OCR...) with a base64 image.
     * @param base64Image The base64 content of an image
     * @return The service response
     */
    @Override
    public String invoke(String base64Image) throws ManifoldCFException {

        String openAiApiKey = spec.getStringProperty(BinaryConfig.NODE_SERVICE_SECURITY_TOKEN);
        HttpClient client = HttpClient.newHttpClient();
        String model = this.spec.getStringProperty(BinaryConfig.NODE_SERVICE_ENDPOINT);

        String requestBody = "{" +
                "  \"model\": \"" + model + "\"," +
                "  \"messages\": [{" +
                "    \"role\": \"user\"," +
                "    \"content\": [" +
                "      {" +
                "        \"type\": \"image_url\"," +
                "        \"image_url\": {" +
                "          \"url\": \"data:image/png;base64," + base64Image  + "\"" +
                "        }" +
                "      }," +
                "      {" +
                "          \"type\": \"text\"," +
                "        \"text\": \"Describe the content of this image and mention all names entities.\"" +
                "      }" +
                "    ]" +
                "  }]," +
                "        \"max_tokens\": " + maxTokens +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response.body();
    }
}