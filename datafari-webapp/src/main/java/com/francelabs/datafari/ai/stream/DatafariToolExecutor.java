package com.francelabs.datafari.ai.stream;

import com.francelabs.datafari.ai.agentic.agents.common.HumanInputKind;
import com.francelabs.datafari.ai.agentic.agents.common.HumanInputType;
import com.francelabs.datafari.ai.agentic.agents.common.HumanInputService;
import com.francelabs.datafari.ai.agentic.tools.AgenticToolException;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link ToolExecutor} decorator used by Datafari to intercept every tool execution
 * performed by a LangChain4j agent.
 *
 * <p>This executor enriches the standard LangChain4j tool execution lifecycle with
 * Datafari-specific features such as:</p>
 * <ul>
 *   <li>Streaming tool execution events to the frontend ({@code tool.call},
 *       {@code tool.end} and {@code tool.error});</li>
 *   <li>Generation of a unique tool invocation identifier shared between the UI
 *       and the executed tool;</li>
 *   <li>Support for Human-in-the-Loop (HITL) interactions before executing
 *       sensitive tools (confirmation, parameter selection, user clarification,
 *       ...);</li>
 *   <li>Translation of {@link AgenticToolException}s into messages that can be
 *       returned to the LLM while notifying the frontend.</li>
 * </ul>
 *
 * <p>The actual business logic is delegated to the wrapped
 * {@link ToolExecutor}. This class only manages the execution workflow and the
 * interaction with the user interface.</p>
 */
public final class DatafariToolExecutor implements ToolExecutor {

    private final ToolExecutor delegate;
    private final ChatStream stream;
    private final String toolName;
    private final String label;
    private final String icon;
    private final String i18nKey;

    private final boolean requiresHumanInput;
    private final HumanInputKind humanInputKind;
    private final HumanInputType humanInputType;
    private final String humanInputTitle;
    private final String humanInputMessage;
    private final List<String> humanInputOptions;


    public DatafariToolExecutor(String toolName, ToolExecutor delegate, ChatStream stream,
                                String label, String icon, String i18nKey,
                                boolean requiresHumanInput,
                                HumanInputKind humanInputKind,
                                HumanInputType humanInputType,
                                String humanInputTitle,
                                String humanInputMessage,
                                List<String> humanInputOptions) {
        this.toolName = toolName;
        this.delegate = delegate;
        this.stream = stream;
        this.label = label;
        this.icon = icon;
        this.i18nKey = i18nKey;
        this.requiresHumanInput = requiresHumanInput;
        this.humanInputKind = humanInputKind;
        this.humanInputType = humanInputType;
        this.humanInputTitle = humanInputTitle;
        this.humanInputMessage = humanInputMessage;
        this.humanInputOptions = humanInputOptions != null ? humanInputOptions : List.of();
    }

    @Override
    public String execute(ToolExecutionRequest req, Object memoryId) {
        String id = UUID.randomUUID().toString();
        long t0 = System.nanoTime();

        // Sending the tool.call event
        stream.toolCall(id, toolName, label, icon, i18nKey);

        // Check confirmation requirement and ask permission to the user
        String humanInputValue = null;

        if (requiresHumanInput) {
            humanInputValue = HumanInputService.ask(
                    stream,
                    humanInputKind,
                    humanInputType,
                    humanInputTitle,
                    humanInputMessage,
                    humanInputOptions,
                    Map.of(
                            "toolCallId", id,
                            "toolName", toolName,
                            "label", label,
                            "arguments", req.arguments()
                    )
            );

            if (humanInputType == HumanInputType.CONFIRMATION
                    && !"approved".equalsIgnoreCase(humanInputValue)) {
                long ms = (System.nanoTime() - t0) / 1_000_000;
                stream.toolError(id, ms, "Tool execution rejected by user");
                return "The user rejected the execution of this tool.";
            }
        }

        try {
            // The InvocationContext is used to provide the tool call ID to the tool method
            InvocationContext.Builder invocationContext = InvocationContext.builder();
            invocationContext.chatMemoryId(memoryId);
            invocationContext.invocationId(UUID.fromString(id));

            InvocationParameters invocationParameters = InvocationParameters.from("toolCallId", id);
            if (humanInputValue != null) {
                // Include human input value in the invocation context
                invocationParameters.put("humanInputValue", humanInputValue);
            }
            invocationContext.invocationParameters(invocationParameters);


            ToolExecutionResult toolExecResult = delegate.executeWithContext(req, invocationContext.build());
            String result = toolExecResult.resultText();

            long ms = (System.nanoTime() - t0) / 1_000_000;
            stream.toolEnd(id, ms);
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