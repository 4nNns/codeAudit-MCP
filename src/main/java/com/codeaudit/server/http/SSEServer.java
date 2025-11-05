package com.codeaudit.server.http;

import com.codeaudit.server.Application;
import com.codeaudit.server.mcp.MCPTool;
import com.codeaudit.server.mcp.MCPServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * SSE 服务器（简化版 MCP 服务器）
 */
public class SSEServer {
    private static final Logger logger = LoggerFactory.getLogger(SSEServer.class);
    private final Application app;
    private final MCPServer mcpServer;
    private final ObjectMapper objectMapper;
    private HttpServer server;

    public SSEServer(Application app) {
        this.app = app;
        this.mcpServer = new MCPServer("Fenrir - 基于 MCP 的自动化代码审计工具", "1.2.0");
        this.objectMapper = new ObjectMapper();
        registerTools();
    }

    private void registerTools() {
        // 注册远程代码审计工具
        ObjectNode remoteAuditSchema = objectMapper.createObjectNode();
        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode repoUrlProp = objectMapper.createObjectNode();
        repoUrlProp.put("type", "string");
        repoUrlProp.put("description", "远程仓库URL，支持格式: zip:https://example.com/repo.zip 或 git:https://github.com/user/repo.git");
        properties.set("repository_url", repoUrlProp);
        ObjectNode branchProp = objectMapper.createObjectNode();
        branchProp.put("type", "string");
        branchProp.put("description", "Git分支名 (仅用于git仓库)");
        properties.set("branch", branchProp);
        remoteAuditSchema.set("properties", properties);
        remoteAuditSchema.putArray("required").add("repository_url");
        
        MCPTool remoteAuditTool = new MCPTool(
            "remote_code_audit",
            "从远程仓库下载代码并进行自动化代码审计",
            remoteAuditSchema,
            args -> {
                try {
                    return app.handleRemoteCodeAudit(args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );
        mcpServer.registerTool(remoteAuditTool);

        // 注册代码搜索工具
        ObjectNode codeSearchSchema = objectMapper.createObjectNode();
        ObjectNode codeSearchProps = objectMapper.createObjectNode();
        ObjectNode classNameProp = objectMapper.createObjectNode();
        classNameProp.put("type", "string");
        classNameProp.put("description", "要搜索的类名");
        codeSearchProps.set("className", classNameProp);
        ObjectNode methodNameProp = objectMapper.createObjectNode();
        methodNameProp.put("type", "string");
        methodNameProp.put("description", "要搜索的方法名");
        codeSearchProps.set("methodName", methodNameProp);
        ObjectNode fieldNameProp = objectMapper.createObjectNode();
        fieldNameProp.put("type", "string");
        fieldNameProp.put("description", "要搜索的字段名");
        codeSearchProps.set("fieldName", fieldNameProp);
        codeSearchSchema.set("properties", codeSearchProps);
        codeSearchSchema.putArray("required").add("className");
        
        MCPTool codeSearchTool = new MCPTool(
            "code_search",
            "基于AST的代码搜索工具",
            codeSearchSchema,
            args -> {
                try {
                    return app.handleCodeSearch(args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );
        mcpServer.registerTool(codeSearchTool);

        // 注册类层次结构工具
        ObjectNode hierarchySchema = objectMapper.createObjectNode();
        ObjectNode hierarchyProps = objectMapper.createObjectNode();
        ObjectNode classNameHierarchyProp = objectMapper.createObjectNode();
        classNameHierarchyProp.put("type", "string");
        classNameHierarchyProp.put("description", "要查找的类名");
        hierarchyProps.set("className", classNameHierarchyProp);
        ObjectNode typeProp = objectMapper.createObjectNode();
        typeProp.put("type", "string");
        typeProp.put("description", "查找类型: super 或 sub");
        hierarchyProps.set("type", typeProp);
        hierarchySchema.set("properties", hierarchyProps);
        hierarchySchema.putArray("required").add("className").add("type");
        
        MCPTool hierarchyTool = new MCPTool(
            "class_hierarchy",
            "查找指定类的所有父类或所有子类",
            hierarchySchema,
            args -> {
                try {
                    return app.handleClassHierarchy(args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );
        mcpServer.registerTool(hierarchyTool);
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // 工具列表端点
        server.createContext("/tools", new ToolsHandler());
        
        // 工具调用端点
        server.createContext("/call", new CallHandler());
        
        // 健康检查端点
        server.createContext("/health", new HealthHandler());
        
        server.setExecutor(null);
        server.start();
        
        logger.info("HTTP服务器已启动，监听端口: {}", port);
    }

    private class ToolsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                var tools = mcpServer.getTools();
                ObjectNode response = objectMapper.createObjectNode();
                response.put("success", true);
                var toolsArray = response.putArray("tools");
                
                for (MCPTool tool : tools) {
                    ObjectNode toolNode = objectMapper.createObjectNode();
                    toolNode.put("name", tool.getName());
                    toolNode.put("description", tool.getDescription());
                    toolNode.set("inputSchema", tool.getInputSchema());
                    toolsArray.add(toolNode);
                }
                
                sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                logger.error("处理工具列表请求失败", e);
                sendResponse(exchange, 500, "Internal Server Error");
            }
        }
    }

    private class CallHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                JsonNode request = objectMapper.readTree(requestBody);
                
                String toolName = request.has("tool") ? request.get("tool").asText() : null;
                JsonNode arguments = request.has("arguments") ? request.get("arguments") : 
                        objectMapper.createObjectNode();
                
                if (toolName == null) {
                    sendResponse(exchange, 400, "tool parameter is required");
                    return;
                }
                
                CompletableFuture<com.codeaudit.server.mcp.MCPResponse> future =
                        mcpServer.callTool(toolName, arguments);
                com.codeaudit.server.mcp.MCPResponse response = future.get();
                
                ObjectNode responseJson = objectMapper.createObjectNode();
                responseJson.put("success", response.isSuccess());
                if (response.isSuccess()) {
                    responseJson.set("content", response.getContent());
                } else {
                    responseJson.put("error", response.getError());
                }
                
                sendResponse(exchange, 200, objectMapper.writeValueAsString(responseJson));
            } catch (Exception e) {
                logger.error("处理工具调用请求失败", e);
                ObjectNode errorResponse = objectMapper.createObjectNode();
                errorResponse.put("success", false);
                errorResponse.put("error", e.getMessage());
                sendResponse(exchange, 500, objectMapper.writeValueAsString(errorResponse));
            }
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("status", "ok");
            response.put("ready", app.isReady());
            sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}

