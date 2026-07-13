package com.francelabs.datafari.mcp.dto;

import java.util.List;

public record AgenticResponse(
        String message,
        List<Source> sources
) {
}