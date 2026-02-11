package com.francelabs.datafari.ai.stream;

import com.francelabs.datafari.ai.agentic.tools.AgenticToolException;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.UUID;

public final class StreamToolExecutor implements ToolExecutor {

    private final ToolExecutor delegate;
    private final ChatStream stream;
    private final String toolName;
    private final String label;
    private final String icon;
    private final String i18nKey;


    public StreamToolExecutor(String toolName, ToolExecutor delegate, ChatStream stream,
                              String label, String icon, String i18nKey) {
        this.toolName = toolName;
        this.delegate = delegate;
        this.stream = stream;
        this.label = label;
        this.icon = icon;
        this.i18nKey = i18nKey;
    }

    @Override
    public String execute(ToolExecutionRequest req, Object memoryId) {
        String id = UUID.randomUUID().toString();
        long t0 = System.nanoTime();

        // Sending the tool.call event
        stream.toolCall(id, toolName, label, icon, i18nKey);

        try {
            // The InvocationContext is use to provide the tool call ID to the tool method
            InvocationContext.Builder invocationContext = InvocationContext.builder();
            invocationContext.chatMemoryId(memoryId);
            invocationContext.invocationId(UUID.fromString(id));
            invocationContext.invocationParameters(InvocationParameters.from("toolCallId", id));

            ToolExecutionResult toolExecResult = delegate.executeWithContext(req, invocationContext.build());
            String result = toolExecResult.resultText();

            long ms = (System.nanoTime() - t0) / 1_000_000;
            stream.toolResult(id, ms);
            return result;
        } catch (AgenticToolException ex) {
            // If the error is properly caught, the message is returned to the agent
            long ms = (System.nanoTime() - t0) / 1_000_000;
            stream.toolError(id, ms, String.valueOf(ex.getMessage()));
            return String.valueOf(ex.getMessage());
        } catch (Throwable t) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            stream.toolError(id, ms, String.valueOf(t.getMessage()));
            return "Uncaught exception: " + t.getMessage();
        }
    }
}