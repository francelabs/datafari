package com.francelabs.datafari.rest.v2_0.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francelabs.datafari.ai.models.ChatModelFactory;
import com.francelabs.datafari.ai.models.ChatModelConfig;
import com.francelabs.datafari.ai.models.ChatModelConfigurationManager;
import com.francelabs.datafari.ai.models.ChatModelRegistry;
import dev.langchain4j.model.chat.ChatModel;
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
                ChatModelConfigurationManager manager = new ChatModelConfigurationManager();
                ChatModelRegistry registry = new ChatModelRegistry();
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
            ChatModelRegistry incomingRegistry = mapper.readValue(req.getInputStream(), ChatModelRegistry.class);

            // Basic validation
            if (incomingRegistry.getModels() == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing 'models' array in request body.");
                return;
            }

            // Load current models configuration
            ChatModelConfigurationManager manager = new ChatModelConfigurationManager();

            // Updated models list
            List<ChatModelConfig> updatedModels = new ArrayList<>();

            for (ChatModelConfig config : incomingRegistry.getModels()) {
                if (config.getName() == null || config.getInterfaceType() == null) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Model must have 'name' and 'interfaceType'");
                    return;
                }
                updatedModels.add(config);
            }

            // If JSON request body is valid
            // 1. Delete all existing models
            for (ChatModelConfig existing : new ArrayList<>(manager.listModels())) {
                manager.removeModel(existing.getName());
            }

            // 2. Add new models
            for (ChatModelConfig config : updatedModels) {
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
            ChatModelConfigurationManager configManager = new ChatModelConfigurationManager();
            ChatModelFactory factory = new ChatModelFactory(configManager);
            ChatModel model = factory.createChatModel(modelName);
            String result = model.chat(prompt);
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