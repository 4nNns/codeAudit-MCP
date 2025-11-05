package com.codeaudit.server.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 服务器
 */
public class MCPServer {
    private static final Logger logger = LoggerFactory.getLogger(MCPServer.class);
    private final ObjectMapper objectMapper;
    private final Map<String, MCPTool> tools;
    private final String name;
    private final String version;

    public MCPServer(String name, String version) {
        this.name = name;
        this.version = version;
        this.objectMapper = new ObjectMapper();
        this.tools = new ConcurrentHashMap<>();
    }

    /**
     * 注册工具
     */
    public void registerTool(MCPTool tool) {
        tools.put(tool.getName(), tool);
        logger.info("注册MCP工具: {}", tool.getName());
    }

    /**
     * 处理工具调用
     */
    public CompletableFuture<MCPResponse> callTool(String toolName, JsonNode arguments) {
        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("工具不存在: " + toolName)
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Object result = tool.getHandler().apply(arguments);
                return createSuccessResponse(result);
            } catch (Exception e) {
                logger.error("工具调用失败: {}", toolName, e);
                return createErrorResponse(e.getMessage());
            }
        });
    }

    /**
     * 获取工具列表
     */
    public List<MCPTool> getTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 获取服务器信息
     */
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("version", version);
        return info;
    }

    private MCPResponse createSuccessResponse(Object result) {
        MCPResponse response = new MCPResponse();
        response.setSuccess(true);
        
        ObjectNode content = objectMapper.createObjectNode();
        if (result instanceof String) {
            content.put("type", "text");
            content.put("text", (String) result);
        } else {
            content.set("data", objectMapper.valueToTree(result));
        }
        
        response.setContent(content);
        return response;
    }

    private MCPResponse createErrorResponse(String error) {
        MCPResponse response = new MCPResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }
}

