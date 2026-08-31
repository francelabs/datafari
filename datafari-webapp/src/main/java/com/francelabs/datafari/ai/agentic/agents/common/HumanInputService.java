package com.francelabs.datafari.ai.agentic.agents.common;

import com.francelabs.datafari.ai.stream.ChatStream;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * This service can be invoked by Agents, Tools, ToolExecutor or other services
 * to send an input request to the user. The main thread is paused, until a response
 * is received or until timeout.
 */
public class HumanInputService {

    private static final long DEFAULT_TIMEOUT_SECONDS = 300L;

    private static final ConcurrentHashMap<String, CompletableFuture<String>> PENDING =
            new ConcurrentHashMap<>();

    private HumanInputService() {
    }

    public static String ask(
            ChatStream stream,
            HumanInputKind kind,   // Reason of the request
            HumanInputType type,   // Type of experted input (for UI rendering)
            String title,          // Key or label of the message title
            String message,        // Key or label of the message content
            List<String> options,  // List of options amongst which user can switch
            Map<String, ?> payload // Arguments
    ) {
        // Create a random interaction ID
        String interactionId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();

        PENDING.put(interactionId, future);

        stream.humanInputRequired(
                interactionId,
                kind.name(),
                type.name(),
                title,
                message,
                options,
                payload != null ? payload : Map.of()
        );

        try {
            return future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            stream.humanInputTimeout(interactionId);
            throw new RuntimeException("Human input timeout: " + interactionId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Human input interrupted: " + interactionId, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Human input failed: " + interactionId, e);
        } finally {
            PENDING.remove(interactionId);
        }
    }

    public static boolean answer(String interactionId, String value) {
        CompletableFuture<String> future = PENDING.get(interactionId);

        if (future == null) {
            return false;
        }

        future.complete(value);
        return true;
    }
}