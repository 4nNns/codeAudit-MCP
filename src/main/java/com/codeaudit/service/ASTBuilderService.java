package com.codeaudit.service;

import com.codeaudit.config.Config;
import com.codeaudit.core.ASTIndex;
import com.codeaudit.core.ParserManager;
import com.codeaudit.core.QueryEngine;
import com.codeaudit.model.UniversalASTNode;
import com.codeaudit.parser.JavaASTParser;
import com.codeaudit.persistence.ASTPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * AST构建服务
 */
public class ASTBuilderService {
    private static final Logger logger = LoggerFactory.getLogger(ASTBuilderService.class);
    private final Config config;
    private final ParserManager manager;
    private final ASTPersistenceManager persistence;

    public ASTBuilderService(Config config) {
        this.config = config;
        this.manager = new ParserManager();
        
        // 注册解析器
        manager.registerParser(new JavaASTParser());
        // 可以添加更多语言的解析器
        
        this.persistence = new ASTPersistenceManager(config);
    }

    /**
     * 构建或加载AST索引
     */
    public ASTIndex buildOrLoadAST() throws IOException {
        // 检查是否启用缓存
        if (!config.getCodeAudit().getAstCache().isEnabled()) {
            logger.info("AST缓存已禁用，正在重新构建AST...");
            return buildAST();
        }

        // 检查是否需要重新构建
        if (config.getCodeAudit().getAstCache().isRebuildOnStartup()) {
            logger.info("配置要求重新构建AST，正在构建...");
            return buildAST();
        }

        // 尝试从缓存加载
        if (persistence.cacheExists()) {
            logger.info("发现AST缓存文件，正在加载...");
            try {
                ASTIndex index = persistence.loadASTIndex();
                int nodeCount = index.getAllNodes().size();
                logger.info("成功从缓存加载AST，节点数: {}", nodeCount);
                return index;
            } catch (Exception e) {
                logger.warn("加载AST缓存失败: {}，正在重新构建...", e.getMessage());
                return buildAST();
            }
        }

        // 缓存不存在，构建新的AST
        logger.info("未发现AST缓存文件，正在构建AST...");
        return buildAST();
    }

    /**
     * 构建AST索引
     */
    private ASTIndex buildAST() throws IOException {
        String repoPath = config.getCodeAudit().getRepositoryPath();

        // 获取绝对路径
        File repoFile = new File(repoPath);
        String absPath = repoFile.getAbsolutePath();

        // 检查路径是否存在
        if (!repoFile.exists()) {
            throw new IOException("指定的路径不存在: " + absPath);
        }

        logger.info("正在分析代码仓库: {}", absPath);

        // 构建索引
        manager.buildIndexFromDir(absPath);

        ASTIndex index = manager.getIndex();

        // 如果启用缓存，保存到文件
        if (config.getCodeAudit().getAstCache().isEnabled()) {
            logger.info("正在保存AST索引到缓存文件...");
            try {
                persistence.saveASTIndex(index);
                logger.info("AST索引已保存到缓存文件");
            } catch (Exception e) {
                logger.error("保存AST缓存失败: {}", e.getMessage(), e);
                // 不抛出异常，因为索引已经构建成功
            }
        }

        return index;
    }

    /**
     * 获取查询引擎
     */
    public QueryEngine getQueryEngine() throws IOException {
        ASTIndex index = buildOrLoadAST();
        return new QueryEngine(index);
    }

    /**
     * 打印统计信息
     */
    public void printStatistics(ASTIndex index) {
        QueryEngine query = new QueryEngine(index);
        var nodes = query.getAllNodes();

        System.out.println("\n代码分析完成！");
        System.out.println("总节点数：" + nodes.size());

        // 按类型统计
        var typeCount = nodes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        UniversalASTNode::getType,
                        java.util.stream.Collectors.counting()
                ));

        System.out.println("\n节点类型统计：");
        typeCount.forEach((type, count) -> 
            System.out.printf("- %s: %d\n", type, count)
        );
    }

    /**
     * 清除缓存
     */
    public void clearCache() throws IOException {
        persistence.clearCache();
    }

    /**
     * 列出缓存文件
     */
    public java.util.List<String> listCacheFiles() throws IOException {
        return persistence.listCacheFiles();
    }
}
