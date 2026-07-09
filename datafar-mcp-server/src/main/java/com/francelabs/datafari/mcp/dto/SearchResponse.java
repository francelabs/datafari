package com.francelabs.datafari.mcp.dto;

import java.util.List;

public record SearchResponse(
//        String message,
        List<Source> results
) {
}