package com.francelabs.datafari.ai.stream;

import com.francelabs.datafari.ai.agentic.agents.common.HumanInputKind;
import com.francelabs.datafari.ai.agentic.agents.common.HumanInputType;

import java.lang.annotation.*;

/**
 * Provides Datafari-specific metadata for an agentic tool.
 *
 * <p>This annotation complements LangChain4j's {@link dev.langchain4j.agent.tool.Tool}
 * annotation with UI metadata, streaming metadata and optional human-in-the-loop behavior.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ToolMeta {
    /** Technical identifier. Default value is method name. */
    String name() default "";
    /** Unlocalized label for the UI */
    String label() default "";
    /** Description */
    String description() default "";
    /** Optional: i18n key for localized displayed in UI */
    String i18nKey() default "";
    /** Optional: icon path or name */
    String icon() default "";

//    boolean requiresConfirmation() default false;
//    String confirmationTitle() default "";
//    String confirmationMessage() default "";

    /** Whether this tool requires a human input before execution. */
    boolean requiresHumanInput() default false;
    /** Business reason for the human input request. */
    HumanInputKind humanInputKind() default HumanInputKind.TOOL_CONFIRMATION;
    /** Expected input format for the human input request. */
    HumanInputType humanInputType() default HumanInputType.CONFIRMATION;
    /** Title displayed to the user when asking for human input. */
    String humanInputTitle() default "";
    /** Message displayed to the user when asking for human input. */
    String humanInputMessage() default "";
    /** Available options for choice or confirmation inputs. */
    String[] humanInputOptions() default {"approved", "rejected"};
}