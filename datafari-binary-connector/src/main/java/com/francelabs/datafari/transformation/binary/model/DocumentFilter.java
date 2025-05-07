package com.francelabs.datafari.transformation.binary.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DocumentFilter {

    private static final Logger LOGGER = LogManager.getLogger(DocumentFilter.class.getName());
    private static final Set<String> FILTERS = new HashSet<>(Arrays.asList(
            "exc_extension", "inc_extension",
            "exc_mimetype", "inc_mimetype",
            "min_size", "max_size",
            "min_creation_date", "max_creation_date",
            "metadata",
            "inc_filename_regex"
    ));

    private Set<String> includeExtensions;
    private Set<String> excludeExtensions;
    private Set<String> excludeMimeTypes;
    private Set<String> includeMimeTypes;
    private long minSize = -1;
    private long maxSize = Long.MAX_VALUE;
    private Map<String, Pattern> includeMetadata = new HashMap<>();
    private Map<String, Pattern> excludeMetadata = new HashMap<>();

    public DocumentFilter(Map<String, String> config) {
        if (config.containsKey("inc_extension")) {
            includeExtensions = Arrays.stream(config.get("inc_extension").split(","))
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        if (config.containsKey("exc_extension")) {
            excludeExtensions = Arrays.stream(config.get("exc_extension").split(","))
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        if (config.containsKey("inc_mimetype")) {
            // inc_mimetype=image/png, image/jpeg, application/pdf
            includeMimeTypes = Arrays.stream(config.get("inc_mimetype").split(","))
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        if (config.containsKey("exc_mimetype")) {
            // exc_mime=image/png, image/jpeg, application/pdf
            excludeMimeTypes = Arrays.stream(config.get("exc_mimetype").split(","))
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        if (config.containsKey("min_size")) {
            minSize = Long.parseLong(config.get("min_size"));
        }
        if (config.containsKey("max_size")) {
            maxSize = Long.parseLong(config.get("max_size"));
        }
        if (config.containsKey("inc_metadata")) {
            includeMetadata = parseMetadataPatterns(config.get("inc_metadata"));
        }
        if (config.containsKey("exc_metadata")) {
            excludeMetadata = parseMetadataPatterns(config.get("exc_metadata"));
        }
    }

    public boolean accept(RepositoryDocument document) {

        // Extension filters
        if (excludeExtensions != null && excludeExtensions.contains(getFileExtension(document.getFileName()))) {
            return false;
        }
        if (includeExtensions != null && !includeExtensions.contains(getFileExtension(document.getFileName()))) {
            return false;
        }
        // Mime type filters
        if (excludeMimeTypes != null && excludeMimeTypes.contains(document.getMimeType())) {
            return false;
        }
        if (includeMimeTypes != null && !includeMimeTypes.contains(document.getMimeType())) {
            return false;
        }

        // Size filters
        long size = document.getBinaryLength();
        if (size < minSize || size > maxSize) {
            return false;
        }

        // Metadata exclusion
        for (Map.Entry<String, Pattern> entry : excludeMetadata.entrySet()) {
            Object[] docValues = document.getField(entry.getKey());
            for (Object docValue : docValues) {
                if (docValue != null && entry.getValue().matcher(docValue.toString()).matches()) {
                    return false;
                }
            }
        }

        // Metadata inclusion
        for (Map.Entry<String, Pattern> entry : includeMetadata.entrySet()) {
            Object[] docValues = document.getField(entry.getKey());
            for (Object docValue : docValues) {
                if (docValue == null || !entry.getValue().matcher(docValue.toString()).matches()) {
                    return false;
                }
            }
        }

        return true;

    }

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return filename.substring(lastIndexOf + 1).toLowerCase().trim();
    }

    private Map<String, Pattern> parseMetadataPatterns(String metadataConfig) {
        Map<String, Pattern> map = new HashMap<>();
        if (metadataConfig != null && !metadataConfig.isEmpty()) {
            String[] pairs = metadataConfig.split(",");
            for (String pair : pairs) {
                String[] parts = pair.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String valuePattern = parts[1].trim()
                            .replace(".", "\\.")
                            .replace("*", ".*")
                            .replace("?", ".");
                    map.put(key, Pattern.compile(valuePattern));
                }
            }
        }
        return map;

    }
}
