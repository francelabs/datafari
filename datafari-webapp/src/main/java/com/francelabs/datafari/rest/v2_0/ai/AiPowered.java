package com.francelabs.datafari.rest.v2_0.ai;

import com.francelabs.datafari.ai.agentic.tools.SourcesAccumulator;
import com.francelabs.datafari.ai.dto.AiRequest;
import com.francelabs.datafari.ai.dto.ApiContent;
import com.francelabs.datafari.ai.dto.ApiError;
import com.francelabs.datafari.ai.dto.ApiResponse;
import com.francelabs.datafari.ai.services.AgenticService;
import com.francelabs.datafari.ai.services.AiService;
import com.francelabs.datafari.ai.services.RagService;
import com.francelabs.datafari.ai.services.SummarizationService;
import com.francelabs.datafari.ai.stream.*;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/v2.0/ai")
public class AiPowered {

    private static final Logger LOGGER = LogManager.getLogger(AiPowered.class.getName());
    private static final String ERROR = "ERROR";
    private static final String OK = "OK";

    // ========= CLASSIC MODE (JSON) =========
    @PostMapping(
            value = "",
            consumes = "application/json",
            produces = "application/json;charset=UTF-8"
    )
    public ResponseEntity<ApiResponse> classic(@RequestBody AiRequest body, HttpServletRequest request) {
        ApiResponse response = new ApiResponse();
        NoopChatStream nostream = new NoopChatStream();

        try {
            List<String> errors = body.validate();
            if (!errors.isEmpty()) {
                response.status = ERROR;
                response.content.error = new ApiError("400",
                        ApiError.RAG_BAD_REQUEST.getKey(),
                        ApiError.RAG_BAD_REQUEST.getValue(),
                        String.join("; ", errors));
                return ResponseEntity.badRequest().body(response);
            }

            ApiContent content = handle(body, request, nostream); // Common action handling

            response.status = (content.error == null) ? OK : ERROR;
            response.content = content;

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.status = ERROR;
            response.content.error = new ApiError("500",
                    "ragTechnicalError",
                    "Sorry, I met a technical issue. Please try again later, and if the problem remains, contact an administrator.",
                    e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========= MODE STREAM (NDJSON) =========
    @PostMapping(
            value = "/stream",
            consumes = "application/json",
            produces = "application/x-ndjson;charset=UTF-8"
    )
    public void stream(
            @RequestBody AiRequest params,
            HttpServletRequest request,
            HttpServletResponse resp
    ) {
        try {
            // Headers NDJSON
            resp.setStatus(200);
            resp.setHeader("Content-Type", "application/x-ndjson; charset=utf-8");
            resp.setHeader("Cache-Control", "no-cache, no-transform");
            resp.setHeader("Connection", "keep-alive");
            resp.setHeader("X-Accel-Buffering", "no");

            AsyncContext async = request.startAsync();
            async.setTimeout(0);
            NdjsonEmitter emitter = new NdjsonEmitter(async);
            ChatStream stream = new NdjsonChatStream(emitter);
            stream.start();

            // Validation
            List<String> errors = params.validate();
            if (!errors.isEmpty()) {
                stream.error("400",
                        ApiError.RAG_BAD_REQUEST.getKey(),
                        ApiError.RAG_BAD_REQUEST.getValue(),
                        String.join("; ", errors));

                stream.completed(ERROR);
                emitter.close();
                return;
            }

            // Common action handling
            ApiContent response = handle(params, request, stream);

            ApiResponse finalApi = new ApiResponse();
            finalApi.status = (response.error == null) ? OK : ERROR;
            finalApi.content = response;

            stream.completed(finalApi.status);
            emitter.close();


        } catch (Exception e) {
            try {
                AsyncContext async = request.getAsyncContext();
                NdjsonEmitter emitter = new NdjsonEmitter(async);
                ChatStream stream = new NdjsonChatStream(emitter);
                stream.error("500",
                        ApiError.RAG_TECHNICAL_ERROR.getKey(),
                        ApiError.RAG_TECHNICAL_ERROR.getValue(),
                        e.getMessage());

                stream.completed(ERROR);
                emitter.close();
            } catch (Exception ex) {
                LOGGER.error("Unexpected error in AiPowered API. Stream emitter could not be closed properly.", ex);
            }
        }
    }

    // ========= MODE STREAM (NDJSON) =========
    // ============== TEST MODE ===============
    @PostMapping(
            value = "/test",
            consumes = "application/json",
            produces = "application/x-ndjson;charset=UTF-8"
    )
    public void test(
            @RequestBody AiRequest params,
            HttpServletRequest request,
            HttpServletResponse resp
    ) {
        try {
            // Headers NDJSON
            resp.setStatus(200);
            resp.setHeader("Content-Type", "application/x-ndjson; charset=utf-8");
            resp.setHeader("Cache-Control", "no-cache, no-transform");
            resp.setHeader("Connection", "keep-alive");
            resp.setHeader("X-Accel-Buffering", "no");

            AsyncContext async = request.startAsync();
            async.setTimeout(0);
            NdjsonEmitter emitter = new NdjsonEmitter(async);
            ChatStream stream = new NdjsonChatStream(emitter);
            stream.start();

            // Validation
            List<String> errors = params.validate();
            if (!errors.isEmpty()) {
                stream.error("400",
                        ApiError.RAG_BAD_REQUEST.getKey(),
                        ApiError.RAG_BAD_REQUEST.getValue(),
                        String.join("; ", errors));

                stream.completed(ERROR);
                emitter.close();
                return;
            }

            // Common action handling
            ApiContent response = testStream(params, request, stream);

            ApiResponse finalApi = new ApiResponse();
            finalApi.status = (response.error == null) ? OK : ERROR;
            finalApi.content = response;

            stream.completed(finalApi.status);
            emitter.close();


        } catch (Exception e) {
            try {
                AsyncContext async = request.getAsyncContext();
                NdjsonEmitter emitter = new NdjsonEmitter(async);
                ChatStream stream = new NdjsonChatStream(emitter);
                stream.error("500",
                        ApiError.RAG_TECHNICAL_ERROR.getKey(),
                        ApiError.RAG_TECHNICAL_ERROR.getValue(),
                        e.getMessage());

                stream.completed(ERROR);
                emitter.close();
            } catch (Exception ex) {
                LOGGER.error("Unexpected error in AiPowered API. Stream emitter could not be closed properly.", ex);
            }
        }
    }

    private ApiContent handle(AiRequest params, HttpServletRequest request, ChatStream stream) {

        // If no action is provided, using "rag" by default
        AiRequest.Action action = params.action == null ? AiRequest.Action.rag : params.action;

        stream.phase("service.started");

        SourcesAccumulator sourcesAcc = new SourcesAccumulator(stream);

        ApiContent result = new ApiContent();
        try {
            result = switch (action.name()) {
                case "rag" -> RagService.rag(request, params, stream, sourcesAcc);
                case "agentic" -> AgenticService.agentic(params, request, stream, sourcesAcc);
                case "summarize" -> SummarizationService.summarize(params, request, stream, sourcesAcc);
                default -> result;
            };

            // JSONize and stream the final sources
            result.sources = sourcesAcc.toJsonArray();

            if (result.message != null && !result.message.isBlank())
                stream.finalMessage(result.message);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in AIService.", e);
            return AiService.error(stream, "500",
                    ApiError.RAG_TECHNICAL_ERROR.getKey(),
                    ApiError.RAG_TECHNICAL_ERROR.getValue(),
                    e.getMessage());
        }

        stream.phase("service:done");

        return result;
    }

    private ApiContent testStream(AiRequest params, HttpServletRequest request, ChatStream stream) {
        final long TEST_DELAY_MS = 250L;

        AiRequest.Action action = params.action == null ? AiRequest.Action.rag : params.action;

        stream.phase("service.started");
        SourcesAccumulator sourcesAcc = new SourcesAccumulator(stream);
        ApiContent result = new ApiContent();

        try {
            result = new ApiContent();

            // 1) Starting phase
            stream.phase("service.started");

            // 2) Showing action
            emit(stream, () -> stream.phase("action:" + action.name().toLowerCase()), TEST_DELAY_MS);

            // 3) Simuluation of 3 tools (2 successful, one failure)
            emit(stream, () -> stream.phase("tool.calling"), TEST_DELAY_MS);
            emit(stream, () -> stream.event("tool.call", Map.of(
                    "id", "jobSuccess1",
                    "name", "SuccessfulJob",
                    "icon", "search",
                    "label", "This label should not be displayed. Use i18n translation instead.",
                    "i18nKey", "testLabelForToolCalling"
            )), TEST_DELAY_MS);
            emit(stream, () -> stream.event("tool.call", Map.of(
                    "id", "jobSuccess2",
                    "name", "AnotherSuccessfulJob",
                    "icon", "brain",
                    "label", "Label to display. No i18n key available."
            )), TEST_DELAY_MS);
            emit(stream, () -> stream.event("tool.call", Map.of(
                    "id", "job3Failure",
                    "name", "BrokenJob",
                    "icon", "document",
                    "label", "This label should not be displayed. Use i18n translation instead.",
                    "i18nKey", "testLabelForToolCalling"
            )), TEST_DELAY_MS);
            emit(stream, () -> stream.event("tool.result", Map.of(
                    "id", "jobSuccess1",
                    "durationMs", "320"
            )), TEST_DELAY_MS);
            emit(stream, () -> stream.event("tool.result", Map.of(
                    "id", "jobSuccess2",
                    "durationMs", "441"
            )), TEST_DELAY_MS);
            emit(stream, () -> stream.event("tool.error", Map.of(
                    "id", "job3Failure",
                    "durationMs", "530",
                    "error", "This job has failed. This message should not be displayed to the user."
            )), TEST_DELAY_MS);

            // 4) Simule des sources au fil de l’eau
            emit(stream, () -> stream.phase("sources.retrieval"), TEST_DELAY_MS);
            Document source1 = Document.from(
                    "This is an example of source (Google). This text should not be visible anywhere.",
                    Metadata.from(Map.of(
                            "url", "https://fr.wikipedia.org/wiki/Nyan_Cat",
                            "id", "https://fr.wikipedia.org/wiki/Nyan_Cat",
                            "title", "Rare space cat (source title to be displayed)"
                    )));
            Document source2 = Document.from(
                    "This is a source known as Wikipedia.",
                    Metadata.from(Map.of(
                            "url", "https://fr.wikipedia.org/wiki/Manidae",
                            "id", "https://fr.wikipedia.org/wiki/Manidae",
                            "title", "Manidae (source title to be displayed)"
                    )));
            emit(stream, () -> sourcesAcc.add(source1), TEST_DELAY_MS);
            emit(stream, () -> sourcesAcc.add(source2), TEST_DELAY_MS);

            // 5) Progress / phases fines
            emit(stream, () -> stream.phase("validation.done"), TEST_DELAY_MS);
            if ("summarize".equals(action.name())) {
                return AiService.error(stream, "402", "testLabelForError",
                        "This test endpoint returns an error if 'summarize' action is called",
                        "Technical error that should not be displayed to the user");
            }


            // 6) Tokens stream (ex. 25 tokens espacés)
            emitTokens(stream, List.of(
                    "This ", "text ", "must ", "be ", "rendered ", "progressively, ",
                    "token ", "by ", "token. ", "It ", "will ",
                    "eventually ", "be ", "replaced ", "by ", "the ", "final ",
                    "message.", "\n"
            ), TEST_DELAY_MS);

            // 7) Final message and sources
            result.message = "✅ This text is the final message. It must override the existing token-by-token text. \nIt must be displayed as the chatbot response.";
            result.sources = sourcesAcc.toJsonArray();


            // 8) Ending
            emit(stream, () -> stream.phase("service.done"), TEST_DELAY_MS);


            // JSONize and stream the final sources
            result.sources = sourcesAcc.toJsonArray();

            if (result.message != null && !result.message.isBlank())
                stream.finalMessage(result.message);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in (test) AIService.", e);
            return AiService.error(stream, "500",
                    ApiError.RAG_TECHNICAL_ERROR.getKey(),
                    ApiError.RAG_TECHNICAL_ERROR.getValue(),
                    e.getMessage());
        }

        stream.phase("service:done");

        return result;
    }

    /** Envoie un event, flush, puis dort delayMs. */
    private void emit(ChatStream stream, Runnable event, long delayMs) {
        event.run();
        if (delayMs > 0) {
            try { Thread.sleep(delayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
    }

    /** Stream a list of tokens with a fixed delay between each. */
    private void emitTokens(ChatStream stream, List<String> tokens, long delayMs) {
        for (String t : tokens) {
            emit(stream, () -> stream.token(t), delayMs);
        }
    }

}
