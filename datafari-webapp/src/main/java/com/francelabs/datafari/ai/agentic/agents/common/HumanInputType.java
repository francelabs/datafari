package com.francelabs.datafari.ai.agentic.agents.common;

/**
 * Describes the expected input format for a human-in-the-loop interaction.
 *
 * <p>This enum is intended to drive the frontend rendering logic.</p>
 */
public enum HumanInputType {

    /** Simple approval/rejection interaction. */
    CONFIRMATION,

    /** User must choose one value from a predefined list. */
    CHOICE,

    /** User can provide a free text value. */
    TEXT,

    /** User can provide a sensitive value, such as a password or token. */
    SECRET
}