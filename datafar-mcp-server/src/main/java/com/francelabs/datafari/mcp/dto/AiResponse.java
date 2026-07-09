package com.francelabs.datafari.mcp.dto;

import java.util.List;

public record AiResponse(
        String message,
        List<Source> sources,
        List<Source> results
) {
}