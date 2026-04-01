package com.francelabs.datafari.ai.services.synthesis;

import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.DisabledException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Synthesis {

  private static final Logger LOGGER = LogManager.getLogger(Synthesis.class.getName());

  /**
   * Create a synthesis of multiple document, based on individual pre-generated summaries
   * @param request
   * @param documents List<Properties> - The list of documents
   * @param stream
   * @return A String synthesis
   */
  public static String synthesize(final HttpServletRequest request, List<Properties> documents, ChatStream stream) throws IOException, DatafariServerException {

    LOGGER.debug("Synthesis - Summary for document {} requested.", documents.size());

    // Get RAG configuration
    RagConfiguration config = RagConfiguration.getInstance();
    ChatModel chatModel = AiService.getChatModel(config);

    // Check if summarization AND synthesis are enabled
    if (!config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION) || !config.getBooleanProperty(RagConfiguration.ENABLE_SYNTHESIS)) {
      LOGGER.debug("AiPowered - Synthesize - Summarization is disabled");
      throw new DisabledException("The summary generation feature is disabled.");
    }

    // Convert Properties to formatted prompt
    List<String> snippets = new ArrayList<>();
    for (Properties document : documents) {
      String title = document.getProperty("title", "-");
      String url = document.getProperty("url", "-");
      String id = document.getProperty("id");
      String summary = document.getProperty("summary", "-");
      String snippet = PromptUtils.synthesisSnippet(title, summary, id, url);

      snippets.add(snippet);
    }

    if (snippets.isEmpty()) {
      LOGGER.debug("Synthesis - Synthesize - No content available for synthesis.");
      throw new IOException("No content available for synthesis.");
    }

    int chunksNb = snippets.size();

    // Setup prompt
    String initialPromptTemplate = PromptUtils.createInitialPromptForSynthesis(request);
    String iterativePromptTemplate = PromptUtils.createPromptForIterateSynthesis(request);


    // First synthesis iteration
    stream.phase("synthesize:generation");
    initialPromptTemplate = PromptUtils.stuffAsManySnippetsAsPossible(initialPromptTemplate, snippets, config);
    AiMessage responseMessage =  chatModel.chat(UserMessage.from(initialPromptTemplate)).aiMessage();
    String lastGeneratedSynthesis = responseMessage.text();

    // Refine iteratively the summary with each chunk
    while (!snippets.isEmpty()) {
      String progression = (chunksNb - snippets.size()) + "/" + chunksNb;
      stream.phase("synthesize: generation (" + progression + ")");

      // Refine the summary for each chunk
      String iterativePrompt = PromptUtils.stuffAsManySnippetsAsPossible(
          iterativePromptTemplate.replace("{synthesis}", lastGeneratedSynthesis),
          snippets, config);

      responseMessage =  chatModel.chat(UserMessage.from(iterativePrompt)).aiMessage();
      lastGeneratedSynthesis = responseMessage.text();
    }
    stream.phase("synthesize:done");

    // Return the last generated summary
    if (lastGeneratedSynthesis.length() > 1) {
      return lastGeneratedSynthesis;
    } else {
      LOGGER.debug("Synthesis - synthesize - Could not generate a synthesis for {} documents.", documents.size());
      throw new IOException("Could not generate synthesis");
    }

  }
}
