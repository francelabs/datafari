package com.francelabs.datafari.ai.stream;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

import java.lang.reflect.Method;
import java.util.*;

public final class ToolMaps {

    private final Map<String, ToolDef> byName = new LinkedHashMap<>();
//
//    public ToolMaps register(Object toolInstance) {
//        Class<?> clazz = toolInstance.getClass();
//        for (Method m : clazz.getMethods()) {
//            if (!isToolMethod(m)) continue; // ton critère (public, non-static ? signature ?)
//
//            ToolMeta meta = m.getAnnotation(ToolMeta.class);
//
//            String name = (meta != null && !meta.name().isBlank())
//                    ? meta.name()
//                    : m.getName();
//
//            // Validation d’unicité
//            if (byName.containsKey(name)) {
//                throw new IllegalStateException("Duplicate tool name: " + name + " for method " + m);
//            }
//
//            String label = (meta != null && !meta.label().isBlank()) ? meta.label() : name;
//            String desc  = (meta != null) ? meta.description() : "";
//            String i18n  = (meta != null) ? meta.i18nKey() : "";
//            String icon  = (meta != null) ? meta.icon() : "";
//
//            byName.put(name, new ToolDef(name, label, desc, i18n, icon, toolInstance, m));
//        }
//        return this;
//    }

    public Optional<ToolDef> get(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public Collection<ToolDef> all() {
        return byName.values();
    }

    private boolean isToolMethod(Method m) {
        // Adapte selon tes règles : pas de bridge/synthetic, public, etc.
        return !m.isSynthetic() && !m.isBridge();
    }

    public static Map<ToolSpecification, ToolExecutor> build(Object toolsInstance, ChatStream stream) {
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
            String name = spec.name();
            Method original = byName.get(name);
            if (original == null) continue;

            // 🔍Extract label from ToolMeta  if any. Otherwise, the label is "Processing..."
            ToolMeta meta = original.getAnnotation(ToolMeta.class);
            String label = (meta != null && !meta.label().isBlank())
                    ? meta.label()
                    : "Processing...";
            String icon = meta != null && !meta.icon().isBlank()
                    ? meta.icon()
                    : null;
            String i18nKey = meta != null && !meta.i18nKey().isBlank()
                    ? meta.i18nKey()
                    : null;

            DefaultToolExecutor delegate = new DefaultToolExecutor(toolsInstance, original, original);
            ToolExecutor wrapped = new StreamToolExecutor(name, delegate, stream, label, icon, i18nKey);
            map.put(spec, wrapped);
        }
        return map;
    }

    public record ToolDef(
            String name,          // Technical identifier
            String label,         // UI display
            String description,
            String i18nKey,
            String icon,
            Object target,        // instance owning the method
            Method method         // methode to invoke
    ) {}
}