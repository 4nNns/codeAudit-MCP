package com.codeaudit.server.stdio;

import com.codeaudit.server.Application;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * 基于标准输入输出的 MCP 服务器
 */
public class StdioMCPServer {
    private static final Logger logger = LoggerFactory.getLogger(StdioMCPServer.class);
    private final Application app;
    private final ObjectMapper objectMapper;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public StdioMCPServer(Application app) {
        this.app = app;
        this.objectMapper = new ObjectMapper();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new PrintWriter(System.out, true);
    }

    public void run() {
        try {
            logger.info("MCP stdio 服务器启动");
            
            // 发送初始化消息
            sendInitialize();
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    JsonNode request = objectMapper.readTree(line);
                    handleRequest(request);
                } catch (Exception e) {
                    logger.error("处理请求失败", e);
                    sendError(null, -32700, "Parse error", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("服务器运行错误", e);
            System.exit(1);
        }
    }

    private void sendInitialize() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", 1);
        
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", objectMapper.createObjectNode()
                .put("name", "codeAudit")
                .put("version", "1.2.0"));
        
        ObjectNode capabilities = objectMapper.createObjectNode();
        capabilities.set("tools", objectMapper.createObjectNode());
        result.set("capabilities", capabilities);
        
        response.set("result", result);
        
        sendJson(response);
    }

    private void handleRequest(JsonNode request) {
        if (!request.has("method")) {
            return;
        }

        String method = request.get("method").asText();
        JsonNode params = request.has("params") ? request.get("params") : null;
        JsonNode id = request.has("id") ? request.get("id") : null;

        try {
            switch (method) {
                case "tools/list":
                    handleToolsList(id);
                    break;
                case "tools/call":
                    handleToolsCall(id, params);
                    break;
                case "initialize":
                    // 已在 sendInitialize 中处理
                    break;
                default:
                    sendError(id, -32601, "Method not found", "Unknown method: " + method);
            }
        } catch (Exception e) {
            logger.error("处理请求失败: {}", method, e);
            sendError(id, -32603, "Internal error", e.getMessage());
        }
    }

    private void handleToolsList(JsonNode id) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id);
        }
        
        ObjectNode result = objectMapper.createObjectNode();
        var toolsArray = result.putArray("tools");
        
        // 注册工具定义（简化版，实际应从 app 获取）
        toolsArray.add(createToolDefinition("remote_code_audit", 
                "从远程仓库下载代码并进行自动化代码审计"));
        toolsArray.add(createToolDefinition("code_search", 
                "基于AST的代码搜索工具"));
        toolsArray.add(createToolDefinition("class_hierarchy", 
                "查找指定类的所有父类或所有子类"));
        
        response.set("result", result);
        sendJson(response);
    }

    private ObjectNode createToolDefinition(String name, String description) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", name);
        tool.put("description", description);
        
        ObjectNode inputSchema = objectMapper.createObjectNode();
        inputSchema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();
        ArrayNode required = inputSchema.putArray("required");
        
        switch (name) {
            case "remote_code_audit":
                properties.set("repository_url", createStringProperty("远程仓库URL"));
                properties.set("branch", createStringProperty("Git分支名"));
                required.add("repository_url");
                break;
            case "code_search":
                properties.set("className", createStringProperty("要搜索的类名"));
                properties.set("methodName", createStringProperty("要搜索的方法名"));
                properties.set("fieldName", createStringProperty("要搜索的字段名"));
                required.add("className");
                break;
            case "class_hierarchy":
                properties.set("className", createStringProperty("要查找的类名"));
                properties.set("type", createStringProperty("查找类型: super 或 sub"));
                required.add("className");
                required.add("type");
                break;
        }
        
        inputSchema.set("properties", properties);
        tool.set("inputSchema", inputSchema);
        
        return tool;
    }

    private ObjectNode createStringProperty(String description) {
        ObjectNode prop = objectMapper.createObjectNode();
        prop.put("type", "string");
        prop.put("description", description);
        return prop;
    }

    private void handleToolsCall(JsonNode id, JsonNode params) {
        if (params == null || !params.has("name")) {
            sendError(id, -32602, "Invalid params", "Missing tool name");
            return;
        }

        String toolName = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : 
                objectMapper.createObjectNode();

        try {
            Object result;
            switch (toolName) {
                case "remote_code_audit":
                    result = app.handleRemoteCodeAudit(arguments);
                    break;
                case "code_search":
                    result = app.handleCodeSearch(arguments);
                    break;
                case "class_hierarchy":
                    result = app.handleClassHierarchy(arguments);
                    break;
                default:
                    sendError(id, -32601, "Tool not found", "Unknown tool: " + toolName);
                    return;
            }

            // 创建成功响应
            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            if (id != null) {
                response.set("id", id);
            }
            
            ObjectNode resultObj = objectMapper.createObjectNode();
            if (result instanceof String) {
                resultObj.put("type", "text");
                resultObj.put("text", (String) result);
            } else {
                resultObj.set("data", objectMapper.valueToTree(result));
            }
            
            ObjectNode contentObj = objectMapper.createObjectNode();
            contentObj.putArray("content").add(resultObj);
            response.set("result", contentObj);
            
            sendJson(response);
        } catch (Exception e) {
            logger.error("工具调用失败: {}", toolName, e);
            sendError(id, -32603, "Tool execution failed", e.getMessage());
        }
    }

    private void sendError(JsonNode id, int code, String message, String data) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id);
        } else {
            response.putNull("id");
        }
        
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.put("data", data);
        }
        response.set("error", error);
        
        sendJson(response);
    }

    private void sendJson(ObjectNode json) {
        try {
            writer.println(objectMapper.writeValueAsString(json));
            writer.flush();
        } catch (Exception e) {
            logger.error("发送JSON响应失败", e);
        }
    }
}
