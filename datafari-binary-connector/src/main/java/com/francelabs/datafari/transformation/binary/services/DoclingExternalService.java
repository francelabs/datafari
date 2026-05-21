package com.francelabs.datafari.transformation.binary.services;

import com.francelabs.datafari.transformation.binary.BinaryConfig;
import com.francelabs.datafari.transformation.binary.model.BinarySpecification;
import com.francelabs.datafari.transformation.binary.utils.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class DoclingExternalService extends ExternalService implements IExternalService {

    private static final Logger LOGGER = LogManager.getLogger(DoclingExternalService.class.getName());

    static final String DEFAULT_URL = "http://127.0.0.1:5001";
    static final String DEFAULT_ENDPOINT = "/v1/convert/source";

    public DoclingExternalService(BinarySpecification spec) {
        // ALWAYS CALL SUPER AT THE BEGINING OF THE CONSTRUCTOR
        super(spec);

        // Default URL
        if (this.url == null) {
            this.url = URI.create(DEFAULT_URL + DEFAULT_ENDPOINT);
        }
    }

    public String invoke(String base64content) throws ManifoldCFException {

        String apiToken = spec.getStringProperty(BinaryConfig.NODE_SERVICE_SECURITY_TOKEN);

        if (apiToken == null || apiToken.isBlank()) {
            throw new ManifoldCFException("Unable to retrieve security token.");
        }
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        String filename = UUID.randomUUID().toString();

        // Remove prefix
        base64content = base64content.replaceFirst("^data:.*;base64,", "");

        String requestBody = "{\"options\": {\"from_formats\": [\"docx\", \"pptx\", \"html\", \"image\", \"pdf\", \"asciidoc\", \"md\", \"xlsx\"],\"to_formats\": [\"md\", \"json\"],\"image_export_mode\": \"placeholder\",\"do_ocr\": true,\"force_ocr\": false,\"ocr_lang\": [\"en\", \"fr\", \"es\", \"de\"]},\"sources\": [{\"base64_string\": \"${B64_DATA}\",\"filename\": \"${FILENAME}\",\"kind\": \"file\"}]}"
            .replace("${B64_DATA}", base64content)
            .replace("${FILENAME}", filename); // TODO : retrieve actual filename
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.url)
                .version(HttpClient.Version.HTTP_1_1)
                .header("X-Api-Key", apiToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            throw new ManifoldCFException("Unexpected error occured while calling Docling services.",e);
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

}