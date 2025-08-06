package com.francelabs.datafari.rest.v2_0.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.ChatLanguageModelFactory;
import com.francelabs.datafari.ai.LLMModelConfig;
import com.francelabs.datafari.ai.LLMModelConfigurationManager;
import com.francelabs.datafari.ai.LLMModelRegistry;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/rest/v2.0/management/chat-language-models")
public class ChatLanguageModelsConfig extends HttpServlet {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOGGER = LogManager.getLogger(ChatLanguageModelsConfig.class.getName());

    @Override
    /**
     * Returns the content of models.json
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setJsonHeaders(resp);
        try {
            if (req.getParameter("test") != null && "true".equals(req.getParameter("test"))) {
                handleTestModel(req, resp);
            } else {
                LLMModelConfigurationManager manager = new LLMModelConfigurationManager();
                LLMModelRegistry registry = new LLMModelRegistry();
                registry.setActiveModel(manager.getActiveModelConfig() != null ? manager.getActiveModelConfig().getName() : null);
                registry.setModels(manager.listModels());

                resp.setStatus(HttpServletResponse.SC_OK);
                mapper.writeValue(resp.getOutputStream(), registry);
            }
        } catch (IOException e) {
            LOGGER.error(e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to read models.json: " + e.getMessage());
        }

    }

    /**
     * Replaces models.json with request body
     * @param req  an {@link HttpServletRequest} object that contains the request the client has made of the servlet
     * @param resp an {@link HttpServletResponse} object that contains the response the servlet sends to the client
     *
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setJsonHeaders(resp);

        try {
            // Read the JSON body of the request
            LLMModelRegistry incomingRegistry = mapper.readValue(req.getInputStream(), LLMModelRegistry.class);

            // Basic validation
            if (incomingRegistry.getModels() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing 'models' array in request body.");
                return;
            }

            // Load current models configuration
            LLMModelConfigurationManager manager = new LLMModelConfigurationManager();

            // Updated models list
            List<LLMModelConfig> updatedModels = new ArrayList<>();

            for (LLMModelConfig config : incomingRegistry.getModels()) {
                if (config.getName() == null || config.getInterfaceType() == null) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Model must have 'name' and 'interfaceType'");
                    return;
                }
                updatedModels.add(config);
            }

            // If JSON request body is valid
            // 1. Delete all existing models
            for (LLMModelConfig existing : new ArrayList<>(manager.listModels())) {
                manager.removeModel(existing.getName());
            }

            // 2. Add new models
            for (LLMModelConfig config : updatedModels) {
                manager.addOrUpdateModel(config);
            }

            // 3. Apply active model
            if (incomingRegistry.getActiveModel() != null) {
                manager.setActiveModel(incomingRegistry.getActiveModel());
            }

            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (IOException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON or format: " + e.getMessage());
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update models.json: " + e.getMessage());
        }
    }

    private void handleTestModel(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String modelName = req.getParameter("model");
        LOGGER.info("Testing Chat Language Model: {}", modelName);
        String prompt = "Hello !";

        if (modelName == null) {
            LOGGER.error("Missing 'model' parameter.");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing 'model' parameter.");
            return;
        }

        try {
            LLMModelConfigurationManager configManager = new LLMModelConfigurationManager();
            ChatLanguageModelFactory factory = new ChatLanguageModelFactory(configManager);
            ChatLanguageModel model = factory.createChatModel(modelName);
            String result = model.generate(prompt);
            resp.setContentType("text/plain");
            resp.getWriter().write(result);
        } catch (Exception e) {
            LOGGER.error("Error while testing model", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error while testing model: " + e.getMessage());
        }
    }

    private void setJsonHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        // Optional CORS for frontend usage (adapt as needed)
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        PrintWriter out = resp.getWriter();
        out.write("{\"error\": \"" + message.replace("\"", "'") + "\"}");
        out.flush();
    }
}