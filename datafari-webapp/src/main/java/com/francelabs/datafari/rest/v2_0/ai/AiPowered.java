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


                // Optional : Send the JSON final response (Datafari API format)
//                ApiResponse response = new ApiResponse();
//                response.status = ERROR;
//                response.content.error = new ApiError("400",
//                        ApiError.RAG_BAD_REQUEST.getKey(),
//                        ApiError.RAG_BAD_REQUEST.getValue(),
//                        String.join("; ", errors));
//                emitter.send(Map.of("status", response.status, "content", response.content));

                stream.completed(ERROR);
                emitter.close();
                return;
            }

            // Common action handling
            ApiContent response = handle(params, request, stream);

            ApiResponse finalApi = new ApiResponse();
            finalApi.status = (response.error == null) ? OK : ERROR;
            finalApi.content = response;

            // Optional : Send the JSON final response (Datafari API format)
            // emitter.send(Map.of("status", finalApi.status, "content", finalApi.content));

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

//              Uncomment to stream a final NDJSON with the Datafari API formatted response.
//                ApiResponse response = new ApiResponse();
//                response.status = ERROR;
//                response.content.error = new ApiError("500",
//                        ApiError.RAG_TECHNICAL_ERROR.getKey(),
//                        ApiError.RAG_TECHNICAL_ERROR.getValue(),
//                        e.getMessage());
//                emitter.send(Map.of("status", response.status, "content", response.content));

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

}
