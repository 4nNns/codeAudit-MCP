package com.codeaudit.core;

import com.codeaudit.model.ClassRef;
import com.codeaudit.model.UniversalASTNode;
import com.codeaudit.parser.ASTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理多种语言的解析器
 */
public class ParserManager {
    private static final Logger logger = LoggerFactory.getLogger(ParserManager.class);
    private final Map<String, ASTParser> parsers;
    private final ASTIndex index;

    public ParserManager() {
        this.parsers = new HashMap<>();
        this.index = new ASTIndex();
    }

    /**
     * 注册解析器
     */
    public void registerParser(ASTParser parser) {
        parsers.put(parser.getLanguage(), parser);
        logger.info("注册解析器: {}", parser.getLanguage());
    }

    /**
     * 从目录构建索引
     */
    public void buildIndexFromDir(String root) throws IOException {
        logger.info("开始构建索引，根目录: {}", root);
        Path rootPath = new File(root).toPath();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isDirectory()) {
                    return FileVisitResult.CONTINUE;
                }

                String filePath = file.toString();
                String ext = getFileExtension(filePath);
                String language = getLanguageFromExtension(ext);

                if (language == null) {
                    return FileVisitResult.CONTINUE;
                }

                ASTParser parser = parsers.get(language);
                if (parser == null) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    List<UniversalASTNode> nodes = parser.parseFile(filePath);
                    synchronized (index) {
                        for (UniversalASTNode node : nodes) {
                            index.addNode(node);
                        }
                    }
                    logger.debug("解析文件成功: {} (节点数: {})", filePath, nodes.size());
                } catch (Exception e) {
                    logger.warn("解析文件失败: {}", filePath, e);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        // 填充子类关系
        fillSubClasses();
        logger.info("索引构建完成，总节点数: {}", index.size());
    }

    /**
     * 获取索引
     */
    public ASTIndex getIndex() {
        return index;
    }

    /**
     * 填充子类关系
     */
    private void fillSubClasses() {
        Map<String, String> classMap = new HashMap<>();
        
        // 构建全限定名 -> 节点ID 映射
        for (Map.Entry<String, UniversalASTNode> entry : index.getAllNodes().entrySet()) {
            UniversalASTNode node = entry.getValue();
            if ("Class".equals(node.getType()) && node.getFullClassName() != null) {
                classMap.put(node.getFullClassName(), entry.getKey());
            }
        }

        // 遍历所有类节点，为其父类填充子类信息
        for (Map.Entry<String, UniversalASTNode> entry : index.getAllNodes().entrySet()) {
            UniversalASTNode node = entry.getValue();
            if (!"Class".equals(node.getType())) {
                continue;
            }

            for (ClassRef superClassRef : node.getSuperClasses()) {
                String superClassName = (superClassRef.getPackageName() != null ? 
                        superClassRef.getPackageName() + "." : "") + superClassRef.getName();
                
                String parentId = classMap.get(superClassName);
                if (parentId != null) {
                    UniversalASTNode parentNode = index.getNode(parentId);
                    if (parentNode != null) {
                        // 避免重复添加
                        boolean found = false;
                        for (ClassRef subClass : parentNode.getSubClasses()) {
                            if (subClass.getName().equals(node.getName()) && 
                                java.util.Objects.equals(subClass.getPackageName(), node.getPackageName())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            parentNode.getSubClasses().add(new ClassRef(node.getPackageName(), node.getName()));
                        }
                    }
                }
            }
        }
    }

    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot >= 0) {
            return filePath.substring(lastDot);
        }
        return "";
    }

    private String getLanguageFromExtension(String ext) {
        switch (ext.toLowerCase()) {
            case ".java":
                return "java";
            case ".go":
                return "go";
            case ".py":
                return "python";
            default:
                return null;
        }
    }
}

