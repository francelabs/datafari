package com.francelabs.datafari.mcp.dto;

public record Source(
        String id,
        String title,
        String excerpt,
        String url,
        String source,
        String extension
) {
}