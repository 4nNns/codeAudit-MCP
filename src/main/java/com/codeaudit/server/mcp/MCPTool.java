package com.codeaudit.server.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Function;

/**
 * MCP 工具定义
 */
public class MCPTool {
    private final String name;
    private final String description;
    private final JsonNode inputSchema;
    private final Function<JsonNode, Object> handler;

    public MCPTool(String name, String description, JsonNode inputSchema, 
                   Function<JsonNode, Object> handler) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonNode getInputSchema() {
        return inputSchema;
    }

    public Function<JsonNode, Object> getHandler() {
        return handler;
    }
}

