package com.francelabs.datafari.ai.services.summarization;

import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.rag.ChunkUtils;
import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.DisabledException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Summarization {

  private static final Logger LOGGER = LogManager.getLogger(Summarization.class.getName());

  public static String summarize(final HttpServletRequest request, Document doc, ChatStream stream) throws IOException, DatafariServerException {

    LOGGER.debug("Summarization - Summary for document {} requested.", doc.metadata().getString("id"));

    // Get RAG configuration
    RagConfiguration config = RagConfiguration.getInstance();
    ChatModel chatModel = AiService.getChatModel(config);

    // Check if summarization is enabled
    if (!config.getBooleanProperty(RagConfiguration.ENABLE_SUMMARIZATION)) {
      LOGGER.debug("AiPowered - Summarize - Summarization is disabled");
      throw new DisabledException("The summary generation feature is disabled.");
    }

    // Chunk document
    stream.phase("summarize:chunking");
    List<TextSegment> segments = ChunkUtils.chunkContent(doc, config);

    if (segments.isEmpty()) {
      LOGGER.debug("Summarization - Summarize - No text segments found for document {}.", doc.metadata().getString("id"));
      throw new IOException("No content available for summarization.");
    }

    List<String> chunks = segments
        .stream()
        .map(TextSegment::text)
        .collect(Collectors.toCollection(ArrayList::new));

    // Limit the number of chunks used for the summarization
    if (segments.size() > config.getIntegerProperty(RagConfiguration.SUMMARIZATION_CHUNKS_NUMBER)) {
      chunks = chunks.subList(0, config.getIntegerProperty(RagConfiguration.SUMMARIZATION_CHUNKS_NUMBER));
    }
    int chunksNb = segments.size();

    // Setup prompt
    String initialPromptTemplate = PromptUtils.createInitialPromptForSummarization(request);
    String iterativePromptTemplate = PromptUtils.createPromptForIterateSummaries(request);


    // Summarize first chunk
    stream.phase("summarize:summarization");
    initialPromptTemplate = PromptUtils.stuffAsManySnippetsAsPossible(initialPromptTemplate, chunks, config);
    AiMessage responseMessage =  chatModel.chat(UserMessage.from(initialPromptTemplate)).aiMessage();
    String lastGeneratedSummary = responseMessage.text();

    // Refine iteratively the summary with each chunk
    while (!chunks.isEmpty()) {
      String progression = (chunksNb - chunks.size()) + "/" + chunksNb;
      stream.phase("summarize:" + progression);

      // Refine the summary for each chunk
      String iterativePrompt = PromptUtils.stuffAsManySnippetsAsPossible(
          iterativePromptTemplate.replace("{summary}", lastGeneratedSummary),
          chunks, config);

      responseMessage =  chatModel.chat(UserMessage.from(iterativePrompt)).aiMessage();
      lastGeneratedSummary = responseMessage.text();
    }
    stream.phase("summarize:done");

    // Return the last generated summary
    if (lastGeneratedSummary.length() > 1) {
      return lastGeneratedSummary;
    } else {
      LOGGER.debug("Summarization - Summarize - Could not generate a summary for document {}. ", doc.metadata().getString("id"));
      throw new IOException("Could not generate any summary for this document");
    }

  }
}
