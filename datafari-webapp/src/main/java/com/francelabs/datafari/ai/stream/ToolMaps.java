package com.francelabs.datafari.ai.stream;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

import java.lang.reflect.Method;
import java.util.*;

public final class ToolMaps {

    public static Map<ToolSpecification, ToolExecutor> build(Object toolsInstance, SseBridge sse) {
        Map<ToolSpecification, ToolExecutor> map = new LinkedHashMap<>();

        // Retrieve specifications from anotated object
        List<ToolSpecification> specs = ToolSpecifications.toolSpecificationsFrom(toolsInstance);

        // For each spec, retrieve the associated @Tool method
        Class<?> clazz = toolsInstance.getClass();
        Map<String, Method> byName = new HashMap<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Tool.class)) {
                byName.put(m.getName(), m);
            }
        }

        for (ToolSpecification spec : specs) {
            String name = spec.name(); // by default : method name
            Method original = byName.get(name);
            if (original == null) continue; // garde-fou

            DefaultToolExecutor delegate = new DefaultToolExecutor(
                    toolsInstance,
                    original,
                    original
            );

            ToolExecutor wrapped = new SseToolExecutor(spec.name(), delegate, sse);
            map.put(spec, wrapped);
        }
        return map;
    }
}