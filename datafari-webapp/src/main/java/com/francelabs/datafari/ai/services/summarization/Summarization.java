package com.francelabs.datafari.ai.services.summarization;

import com.francelabs.datafari.ai.config.RagConfiguration;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.services.synthesis.InitialSynthesizer;
import com.francelabs.datafari.ai.services.synthesis.IterativeSynthesizer;
import com.francelabs.datafari.ai.stream.ChatStream;
import com.francelabs.datafari.exception.DatafariServerException;
import com.francelabs.datafari.utils.rag.ChunkUtils;
import com.francelabs.datafari.utils.rag.PromptUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
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

    List<String> snippets = segments
        .stream()
        .map(TextSegment::text)
        .collect(Collectors.toCollection(ArrayList::new));

    // Limit the number of chunks used for the summarization
    if (segments.size() > config.getIntegerProperty(RagConfiguration.SUMMARIZATION_CHUNKS_NUMBER)) {
      snippets = snippets.subList(0, config.getIntegerProperty(RagConfiguration.SUMMARIZATION_CHUNKS_NUMBER));
    }
    int chunksNb = segments.size();

    // First synthesis iteration
    stream.phase("synthesize:generation");
    String renderedSnippets = PromptUtils.stuffAsManySnippetsAsPossible("{{snippets}}", snippets, config);
    String lang = PromptUtils.getUserLanguage(request);
    InitialSummarizer service = AiServices.builder(InitialSummarizer.class)
        .chatModel(chatModel)
        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
        .build();
    String lastGeneratedSummary = service.writeSummary(renderedSnippets, lang);

    IterativeSummarizer refiner = AiServices.builder(IterativeSummarizer.class)
        .chatModel(chatModel)
        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
        .build();

    // Refine iteratively the summary with each chunk
    while (!snippets.isEmpty()) {
      String progression = (chunksNb - snippets.size()) + "/" + chunksNb;
      stream.phase("synthesize: generation (" + progression + ")");
      renderedSnippets = PromptUtils.stuffAsManySnippetsAsPossible("{{snippets}}", snippets, config);
      lastGeneratedSummary = refiner.refineSynthesis(lastGeneratedSummary, renderedSnippets, lang);
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
