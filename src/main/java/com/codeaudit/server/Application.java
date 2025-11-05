package com.codeaudit.server;

import com.codeaudit.config.Config;
import com.codeaudit.config.ConfigLoader;
import com.codeaudit.core.ASTIndex;
import com.codeaudit.core.QueryEngine;
import com.codeaudit.remote.RemoteRepositoryManager;
import com.codeaudit.search.SearchHandler;
import com.codeaudit.server.http.SSEServer;
import com.codeaudit.server.stdio.StdioMCPServer;
import com.codeaudit.service.ASTBuilderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 主应用程序
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    private Config config;
    private ASTBuilderService astService;
    private ASTIndex index;
    private QueryEngine query;
    private boolean ready = false;
    private final ReentrantReadWriteLock readyLock = new ReentrantReadWriteLock();
    
    public static void main(String[] args) {
        Application app = new Application();
        
        // 检查是否使用 stdio 模式
        boolean stdioMode = false;
        String repoPath = null;
        String remoteRepo = null;
        String branch = "main";
        
        for (int i = 0; i < args.length; i++) {
            if ("--stdio".equals(args[i]) || "-stdio".equals(args[i])) {
                stdioMode = true;
            } else if ("-i".equals(args[i]) && i + 1 < args.length) {
                repoPath = args[i + 1];
                i++;
            } else if ("-remote".equals(args[i]) && i + 1 < args.length) {
                remoteRepo = args[i + 1];
                i++;
            } else if ("-branch".equals(args[i]) && i + 1 < args.length) {
                branch = args[i + 1];
                i++;
            }
        }
        
        if (stdioMode) {
            app.runStdioMode(repoPath, remoteRepo, branch);
        } else {
            app.run(repoPath, remoteRepo, branch);
        }
    }
    
    public void run(String repoPath, String remoteRepo, String branch) {
        try {
            // 确保默认目录存在
            ensureDefaultResourcesAndCache();
            
            // 加载配置
            config = ConfigLoader.loadDefaultConfig();
            
            // 处理远程仓库
            if (remoteRepo != null && !remoteRepo.isEmpty()) {
                String repoPathResult = handleRemoteRepository(remoteRepo, branch);
                config.getCodeAudit().setRepositoryPath(repoPathResult);
            }
            
            // 如果命令行提供了 -i，则以命令行参数为准
            if (repoPath != null && !repoPath.isEmpty()) {
                config.getCodeAudit().setRepositoryPath(repoPath);
            }
            
            // 如果已经指定了代码仓库路径，立即初始化AST
            if (config.getCodeAudit().getRepositoryPath() != null && 
                !config.getCodeAudit().getRepositoryPath().isEmpty()) {
                File repoFile = new File(config.getCodeAudit().getRepositoryPath());
                if (!repoFile.exists()) {
                    System.err.println("错误：指定的路径不存在：" + repoFile.getAbsolutePath());
                    System.exit(1);
                }
                config.getCodeAudit().setRepositoryPath(repoFile.getAbsolutePath());
                initializeAST();
            } else {
                System.out.println("未指定本地代码仓库路径，等待远程连接...");
                System.out.println("请使用 MCP 客户端调用 remote_code_audit 工具来提供远程仓库地址");
            }
            
            // 启动 MCP SSE 服务器
            SSEServer sseServer = new SSEServer(this);
            int port = 8338;
            sseServer.start(port);
            
            logger.info("SSE server listening on http://0.0.0.0:{}", port);
            System.out.println("\n服务器已启动，按 Ctrl+C 退出...");
            
            if (!isReady()) {
                System.out.println("等待远程代码仓库连接...");
                System.out.println("请使用 MCP 客户端调用 remote_code_audit 工具来提供远程仓库地址");
            }
            
            // 保持程序运行
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("启动失败", e);
            System.exit(1);
        }
    }
    
    public void runStdioMode(String repoPath, String remoteRepo, String branch) {
        try {
            // 确保默认目录存在
            ensureDefaultResourcesAndCache();
            
            // 加载配置
            config = ConfigLoader.loadDefaultConfig();
            
            // 处理远程仓库
            if (remoteRepo != null && !remoteRepo.isEmpty()) {
                String repoPathResult = handleRemoteRepository(remoteRepo, branch);
                config.getCodeAudit().setRepositoryPath(repoPathResult);
            }
            
            // 如果命令行提供了 -i，则以命令行参数为准
            if (repoPath != null && !repoPath.isEmpty()) {
                config.getCodeAudit().setRepositoryPath(repoPath);
            }
            
            // 如果已经指定了代码仓库路径，立即初始化AST
            if (config.getCodeAudit().getRepositoryPath() != null && 
                !config.getCodeAudit().getRepositoryPath().isEmpty()) {
                File repoFile = new File(config.getCodeAudit().getRepositoryPath());
                if (!repoFile.exists()) {
                    System.err.println("错误：指定的路径不存在：" + repoFile.getAbsolutePath());
                    System.exit(1);
                }
                config.getCodeAudit().setRepositoryPath(repoFile.getAbsolutePath());
                initializeAST();
            }
            
            // 启动 stdio MCP 服务器
            StdioMCPServer stdioServer = new StdioMCPServer(this);
            stdioServer.run();
        } catch (Exception e) {
            logger.error("启动失败", e);
            System.exit(1);
        }
    }
    
    private void ensureDefaultResourcesAndCache() throws Exception {
        // 确保 resources/config.yaml 存在
        Path resourcesDir = Paths.get("resources");
        Path configPath = resourcesDir.resolve("config.yaml");
        
        if (!Files.exists(configPath)) {
            Files.createDirectories(resourcesDir);
            String defaultYaml = """
                # 代码审计配置
                code_audit:
                  # 代码仓库路径
                  repository_path: ""
                  
                  # AST缓存配置
                  ast_cache:
                    # 是否启用AST缓存
                    enabled: true
                    # AST缓存目录
                    cache_dir: "./cache"
                    # 是否在启动时重新构建AST（当enabled为true时，此选项生效）
                    rebuild_on_startup: false

                # 远程仓库配置
                remote_repository:
                  enabled: false
                  type: ""  # zip, git, local
                  url: ""
                  branch: "main"
                  target_path: "/tmp/codeAudit_remote"
                  auto_clean: true
                """;
            Files.writeString(configPath, defaultYaml);
            System.out.println("resources/config.yaml 不存在，已创建默认配置。");
        }
        
        // 确保 ./cache 目录存在
        Path cacheDir = Paths.get("./cache");
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
            System.out.println("./cache 目录不存在，已创建。");
        }
    }
    
    private String handleRemoteRepository(String remoteSpec, String branch) throws Exception {
        String[] parts = remoteSpec.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("远程仓库格式错误，应为 type:url");
        }
        
        String repoType = parts[0];
        String repoURL = parts[1];
        
        // 设置临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(remoteSpec.getBytes());
        String hashStr = bytesToHex(hash);
        Path tempPath = Paths.get(tempDir, "fenrir_remote", hashStr);
        
        // 更新配置
        config.getRemoteRepository().setEnabled(true);
        config.getRemoteRepository().setType(repoType);
        config.getRemoteRepository().setUrl(repoURL);
        config.getRemoteRepository().setBranch(branch);
        config.getRemoteRepository().setTargetPath(tempPath.toString());
        config.getRemoteRepository().setAutoClean(true);
        
        // 下载和处理仓库
        RemoteRepositoryManager repoManager = new RemoteRepositoryManager(config);
        String repoPath = repoManager.downloadAndPrepare();
        
        System.out.printf("远程仓库已下载到: %s\n", repoPath);
        return repoPath;
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    public void initializeAST() throws Exception {
        readyLock.writeLock().lock();
        try {
            // 创建AST构建服务
            astService = new ASTBuilderService(config);
            
            // 构建或加载AST索引
            index = astService.buildOrLoadAST();
            
            // 创建查询引擎
            query = new QueryEngine(index);
            
            // 打印统计信息
            astService.printStatistics(index);
            
            ready = true;
        } finally {
            readyLock.writeLock().unlock();
        }
    }
    
    public void waitUntilReady() throws InterruptedException {
        while (!isReady()) {
            Thread.sleep(100);
        }
    }
    
    public boolean isReady() {
        readyLock.readLock().lock();
        try {
            return ready;
        } finally {
            readyLock.readLock().unlock();
        }
    }
    
    public QueryEngine getQuery() {
        return query;
    }
    
    public Config getConfig() {
        return config;
    }
    
    // MCP 工具处理方法
    public Object handleRemoteCodeAudit(JsonNode arguments) throws Exception {
        String repoURL = arguments.has("repository_url") ? 
                arguments.get("repository_url").asText() : null;
        String branch = arguments.has("branch") ? 
                arguments.get("branch").asText() : "main";
        
        if (repoURL == null || repoURL.isEmpty()) {
            throw new IllegalArgumentException("repository_url 参数是必需的");
        }
        
        // 处理远程仓库
        String repoPath = handleRemoteRepository(repoURL, branch);
        
        // 更新配置
        config.getCodeAudit().setRepositoryPath(repoPath);
        
        // 初始化AST
        initializeAST();
        
        // 获取统计信息
        Map<String, Object> stats = new HashMap<>();
        var nodes = query.getAllNodes();
        stats.put("total_nodes", nodes.size());
        
        // 按类型统计
        Map<String, Long> typeCount = new HashMap<>();
        for (var node : nodes) {
            typeCount.put(node.getType(), typeCount.getOrDefault(node.getType(), 0L) + 1);
        }
        stats.put("by_type", typeCount);
        
        // 按语言统计
        Map<String, Long> languageCount = new HashMap<>();
        for (var node : nodes) {
            languageCount.put(node.getLanguage(), 
                    languageCount.getOrDefault(node.getLanguage(), 0L) + 1);
        }
        stats.put("by_language", languageCount);
        
        ObjectMapper mapper = new ObjectMapper();
        String statsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats);
        
        return String.format("远程代码审计完成!\n仓库路径: %s\n审计统计:\n%s", repoPath, statsJson);
    }
    
    public Object handleCodeSearch(JsonNode arguments) throws Exception {
        waitUntilReady();
        
        String className = arguments.has("className") ? 
                arguments.get("className").asText() : "";
        String methodName = arguments.has("methodName") ? 
                arguments.get("methodName").asText() : "";
        String fieldName = arguments.has("fieldName") ? 
                arguments.get("fieldName").asText() : "";
        
        // 调用统一搜索函数
        var results = SearchHandler.unifiedSearch(query, className, methodName, fieldName);
        
        // 格式化结果
        return SearchHandler.formatSearchResults(results);
    }
    
    public Object handleClassHierarchy(JsonNode arguments) throws Exception {
        waitUntilReady();
        
        String className = arguments.has("className") ? 
                arguments.get("className").asText() : "";
        String type = arguments.has("type") ? 
                arguments.get("type").asText() : "";
        
        StringBuilder resultStr = new StringBuilder();
        
        if ("super".equals(type)) {
            var allSupers = SearchHandler.getAllSuperClasses(query, className);
            resultStr.append(String.format("类 %s 的所有父类：\n", className));
            boolean found = false;
            for (var superList : allSupers) {
                for (var sup : superList) {
                    resultStr.append(String.format("  %s.%s\n", 
                            sup.getPackageName(), sup.getName()));
                    found = true;
                }
            }
            if (!found) {
                resultStr.append("  (无父类)\n");
            }
        } else if ("sub".equals(type)) {
            var allSubs = SearchHandler.getAllSubClasses(query, className);
            resultStr.append(String.format("类 %s 的所有子类：\n", className));
            boolean found = false;
            for (var subList : allSubs) {
                for (var sub : subList) {
                    resultStr.append(String.format("  %s.%s\n", 
                            sub.getPackageName(), sub.getName()));
                    found = true;
                }
            }
            if (!found) {
                resultStr.append("  (无子类)\n");
            }
        } else {
            resultStr.append("type 参数只能为 super 或 sub");
        }
        
        return resultStr.toString();
    }
}
