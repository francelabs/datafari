package com.francelabs.datafari.ai.stream;

import com.francelabs.datafari.ai.dto.ApiError;
import dev.langchain4j.service.TokenStream;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

public final class AgentStreamer {

    @FunctionalInterface
    public interface EventEmitter {
        void event(String type,  Map<String, ?> payload);
    }

    private final Duration timeout;

    public AgentStreamer() {
        this.timeout = Duration.ofMinutes(5);
    }

    public AgentStreamer(Duration timeout) {
        this.timeout = timeout == null ? Duration.ofMinutes(5) : timeout;
    }


    /**
     * Streams tokens from LangChain4j and forwards them as structured events.
     * @param ts TokenStream from LangChain4j
     * @param emit something like stream::event (ChatStream)
     * @return the full, concatenated answer
     */
    public String stream(TokenStream ts, EventEmitter emit) {
        StringBuilder full = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);

        ts.onPartialResponse(token -> {
                    if (token != null && !token.isEmpty()) {
                        full.append(token);
                        // token -> message.delta
                        emit.event("message.delta", Map.of("text", token));
                    }
                })
                .onPartialThinking(thinking -> {
                    // If model emits "thinking"
                    if (thinking != null && thinking.text() != null) {
                        emit.event("thinking", Map.of("text", thinking.text()));
                    }
                })
                .onError(err -> {
                    emit.event("error", Map.of(
                            "code", 500,
                            "label", ApiError.RAG_TECHNICAL_ERROR.getKey(),
                            "message", ApiError.RAG_TECHNICAL_ERROR.getValue(),
                            "reason", err.getLocalizedMessage()
                    ));
                    done.countDown();
                })
                .onCompleteResponse(resp -> {
                    // End of stream
                    emit.event("stream.completed", Map.of(
                            "finishReason", String.valueOf(resp.finishReason())
                    ));
                    done.countDown();
                })
                .start();

        // Block until the end, or until timeout
        try {
            done.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        return full.toString();
    }
}