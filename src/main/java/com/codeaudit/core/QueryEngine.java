package com.codeaudit.core;

import com.codeaudit.model.UniversalASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 提供强大的查询功能
 */
public class QueryEngine {
    private static final Logger logger = LoggerFactory.getLogger(QueryEngine.class);
    private final ASTIndex index;

    public QueryEngine(ASTIndex index) {
        this.index = index;
    }

    /**
     * 获取索引中的所有节点
     */
    public List<UniversalASTNode> getAllNodes() {
        return new ArrayList<>(index.getAllNodes().values());
    }

    /**
     * 按类型查找节点
     */
    public List<UniversalASTNode> findByType(String nodeType) {
        return index.findNodes(node -> nodeType.equals(node.getType()));
    }

    /**
     * 按名称查找节点
     */
    public List<UniversalASTNode> findByName(String name) {
        return index.findNodes(node -> name.equals(node.getName()));
    }

    /**
     * 按包名查找节点
     */
    public List<UniversalASTNode> findByPackage(String pkg) {
        return index.findNodes(node -> pkg.equals(node.getPackageName()));
    }

    /**
     * 按语言查找节点
     */
    public List<UniversalASTNode> findByLanguage(String language) {
        return index.findNodes(node -> language.equals(node.getLanguage()));
    }

    /**
     * 获取代码片段
     */
    public String getCodeSnippet(UniversalASTNode node, int contextLines) throws IOException {
        logger.debug("获取代码片段: {} (Type: {}, Lines: {}-{})",
                node.getName(), node.getType(), node.getStartLine(), node.getEndLine());

        Path file = Path.of(node.getFile());
        if (!Files.exists(file)) {
            throw new IOException("文件不存在: " + node.getFile());
        }

        List<String> allLines = Files.readAllLines(file);
        int startLine = Math.max(0, node.getStartLine() - 1 - contextLines); // 转换为0-based
        int endLine = Math.min(allLines.size() - 1, node.getEndLine() - 1 + contextLines);

        List<String> lines = new ArrayList<>();
        for (int i = startLine; i <= endLine && i < allLines.size(); i++) {
            lines.add(allLines.get(i));
        }

        if (lines.isEmpty()) {
            throw new IOException(String.format("未找到行范围 %d-%d", node.getStartLine(), node.getEndLine()));
        }

        logger.debug("读取了 {} 行代码", lines.size());
        return String.join("\n", lines);
    }
}

