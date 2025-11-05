package com.codeaudit.server.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * MCP 响应
 */
public class MCPResponse {
    private boolean success;
    private JsonNode content;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public JsonNode getContent() {
        return content;
    }

    public void setContent(JsonNode content) {
        this.content = content;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

