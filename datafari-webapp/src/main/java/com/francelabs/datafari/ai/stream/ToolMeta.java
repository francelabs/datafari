package com.francelabs.datafari.ai.stream;

import java.lang.annotation.*;

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
}