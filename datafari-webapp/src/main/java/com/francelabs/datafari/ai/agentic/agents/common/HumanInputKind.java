package com.francelabs.datafari.ai.agentic.agents.common;

/**
 * Describes the business reason why the AI workflow requires a human input.
 *
 * <p>This enum is used to help the frontend understand the context of the request
 * and to allow backend code to distinguish between different human-in-the-loop use cases.</p>
 */
public enum HumanInputKind {

    /** A tool requires user approval before being executed. */
    TOOL_CONFIRMATION,

    /** The AI needs additional information from the user to continue. */
    USER_CLARIFICATION,

    /** The user is asked to validate or reject a generated response. */
    RESPONSE_VALIDATION,

    /** A tool requires an additional parameter from the user. */
    TOOL_PARAMETER
}