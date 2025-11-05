package com.codeaudit.persistence;

import com.codeaudit.config.Config;
import com.codeaudit.core.ASTIndex;
import com.codeaudit.model.UniversalASTNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AST持久化管理器
 */
public class ASTPersistenceManager {
    private static final Logger logger = LoggerFactory.getLogger(ASTPersistenceManager.class);
    private final Config config;
    private final ObjectMapper objectMapper;

    public ASTPersistenceManager(Config config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 保存AST索引到文件
     */
    public void saveASTIndex(ASTIndex index) throws IOException {
        String cacheFilePath = config.getCacheFilePath();

        // 确保缓存目录存在
        File cacheFile = new File(cacheFilePath);
        File cacheDir = cacheFile.getParentFile();
        if (cacheDir != null && !cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        // 将索引转换为可序列化的格式
        Map<String, UniversalASTNode> data = index.getAllNodes();

        // 添加元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("repository_path", config.getCodeAudit().getRepositoryPath());
        metadata.put("build_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        metadata.put("node_count", data.size());
        metadata.put("cache_version", "1.0");

        // 创建完整的缓存数据结构
        Map<String, Object> cacheData = new HashMap<>();
        cacheData.put("metadata", metadata);
        cacheData.put("nodes", data);

        // 序列化为JSON
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, cacheData);

        logger.info("AST索引已保存到: {}", cacheFilePath);
    }

    /**
     * 从文件加载AST索引
     */
    public ASTIndex loadASTIndex() throws IOException {
        String cacheFilePath = getLatestCacheFile();
        if (cacheFilePath == null || cacheFilePath.isEmpty()) {
            throw new IOException("未找到匹配的AST缓存文件");
        }

        File cacheFile = new File(cacheFilePath);
        if (!cacheFile.exists()) {
            throw new IOException("AST缓存文件不存在: " + cacheFilePath);
        }

        // 读取文件
        Map<String, Object> cacheData = objectMapper.readValue(cacheFile, Map.class);

        // 提取元数据
        if (cacheData.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) cacheData.get("metadata");
            if (metadata.containsKey("build_time")) {
                logger.info("加载缓存文件: {} (构建时间: {})", cacheFilePath, metadata.get("build_time"));
            }
        }

        // 提取节点数据
        if (!cacheData.containsKey("nodes")) {
            throw new IOException("缓存文件中缺少节点数据");
        }

        Map<String, Map<String, Object>> nodesData = (Map<String, Map<String, Object>>) cacheData.get("nodes");

        // 创建新的索引并填充数据
        ASTIndex index = new ASTIndex();
        TypeFactory typeFactory = TypeFactory.defaultInstance();
        MapType mapType = typeFactory.constructMapType(Map.class, String.class, UniversalASTNode.class);
        
        for (Map.Entry<String, Map<String, Object>> entry : nodesData.entrySet()) {
            UniversalASTNode node = objectMapper.convertValue(entry.getValue(), UniversalASTNode.class);
            index.addNode(node);
        }

        return index;
    }

    /**
     * 检查缓存文件是否存在
     */
    public boolean cacheExists() {
        try {
            String cacheFilePath = getLatestCacheFile();
            return cacheFilePath != null && !cacheFilePath.isEmpty() && 
                   new File(cacheFilePath).exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清除缓存文件
     */
    public void clearCache() throws IOException {
        String cacheDir = config.getCodeAudit().getAstCache().getCacheDir();
        if (cacheDir == null || cacheDir.isEmpty()) {
            cacheDir = "./cache";
        }

        String repoName = getRepositoryName();
        repoName = repoName.replace(" ", "_");
        String pattern = repoName + "_ast_index.json";

        Path cachePath = Paths.get(cacheDir);
        if (!Files.exists(cachePath)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(cachePath)) {
            List<Path> matches = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(pattern))
                    .collect(Collectors.toList());

            for (Path file : matches) {
                Files.delete(file);
                logger.info("已删除缓存文件: {}", file);
            }
        }
    }

    /**
     * 列出所有缓存文件
     */
    public List<String> listCacheFiles() throws IOException {
        String cacheDir = config.getCodeAudit().getAstCache().getCacheDir();
        if (cacheDir == null || cacheDir.isEmpty()) {
            cacheDir = "./cache";
        }

        String repoName = getRepositoryName();
        repoName = repoName.replace(" ", "_");
        String pattern = repoName + "_ast_index.json";

        Path cachePath = Paths.get(cacheDir);
        if (!Files.exists(cachePath)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(cachePath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(pattern))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取最新的缓存文件路径
     */
    private String getLatestCacheFile() {
        String cacheDir = config.getCodeAudit().getAstCache().getCacheDir();
        if (cacheDir == null || cacheDir.isEmpty()) {
            cacheDir = "./cache";
        }

        String repoName = getRepositoryName();
        repoName = repoName.replace(" ", "_");
        String pattern = repoName + "_ast_index.json";

        Path cachePath = Paths.get(cacheDir);
        if (!Files.exists(cachePath)) {
            return null;
        }

        try (Stream<Path> paths = Files.walk(cachePath)) {
            List<Path> matches = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(pattern))
                    .collect(Collectors.toList());

            if (matches.isEmpty()) {
                return null;
            }

            return matches.get(0).toString();
        } catch (IOException e) {
            logger.error("查找缓存文件失败", e);
            return null;
        }
    }

    private String getRepositoryName() {
        String path = config.getCodeAudit().getRepositoryPath();
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        File file = new File(path);
        return file.getName();
    }
}

