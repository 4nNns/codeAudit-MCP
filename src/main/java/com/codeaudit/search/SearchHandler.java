package com.codeaudit.search;

import com.codeaudit.core.QueryEngine;
import com.codeaudit.model.UniversalASTNode;
import com.codeaudit.utils.ClassUtils;
import com.codeaudit.utils.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 搜索处理器
 */
public class SearchHandler {
    private static final Logger logger = LoggerFactory.getLogger(SearchHandler.class);

    /**
     * 统一搜索入口
     */
    public static List<String> unifiedSearch(QueryEngine query, String className, 
                                             String methodName, String fieldName) {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("className is required");
        }
        if (methodName != null && !methodName.isEmpty() && 
            fieldName != null && !fieldName.isEmpty()) {
            throw new IllegalArgumentException("cannot specify both methodName and fieldName at the same time");
        }

        try {
            if (methodName != null && !methodName.isEmpty()) {
                return smartSearchClassMethod(query, className, methodName);
            } else if (fieldName != null && !fieldName.isEmpty()) {
                return enhancedSearchClassField(query, className, fieldName);
            } else {
                return searchClassOnly(query, className);
            }
        } catch (IOException e) {
            logger.error("搜索失败", e);
            throw new RuntimeException("搜索失败", e);
        }
    }

    /**
     * 查询类节点并返回源代码片段
     */
    public static List<String> searchClassOnly(QueryEngine query, String className) throws IOException {
        List<String> results = new ArrayList<>();
        for (UniversalASTNode node : query.getAllNodes()) {
            if ("Class".equals(node.getType()) && ClassUtils.isMatchingClass(node, className)) {
                String snippet = query.getCodeSnippet(node, 100);
                results.add(snippet);
            }
        }
        return results;
    }

    /**
     * 查询类中的方法并返回源代码片段
     */
    public static List<String> searchClassMethod(QueryEngine query, String className, 
                                                 String methodName) throws IOException {
        List<UniversalASTNode> targetClasses = new ArrayList<>();
        for (UniversalASTNode node : query.getAllNodes()) {
            if ("Class".equals(node.getType()) && ClassUtils.isMatchingClass(node, className)) {
                targetClasses.add(node);
                logger.debug("Found target class: {}.{}", node.getPackageName(), node.getName());
            }
        }

        List<String> results = new ArrayList<>();
        for (UniversalASTNode classNode : targetClasses) {
            for (UniversalASTNode node : query.getAllNodes()) {
                // 检查方法是否在目标类范围内
                if ("Method".equals(node.getType()) &&
                    node.getFile().equals(classNode.getFile()) &&
                    node.getStartLine() >= classNode.getStartLine() &&
                    node.getEndLine() <= classNode.getEndLine()) {

                    if (MethodUtils.isMatchingMethod(node, methodName)) {
                        logger.debug("Found matching method: {}.{}", node.getPackageName(), node.getName());
                        String snippet = query.getCodeSnippet(node, 1);
                        results.add(snippet);
                    }
                }
            }
        }
        return results;
    }

    /**
     * 智能搜索方法（根据输入自动选择简单或增强搜索）
     */
    public static List<String> smartSearchClassMethod(QueryEngine query, String className, 
                                                     String methodName) throws IOException {
        // 如果方法名包含括号，使用增强匹配（支持方法签名）
        if (methodName.contains("(")) {
            return searchClassMethod(query, className, methodName);
        }
        // 否则使用简单搜索（仅方法名）
        return searchClassMethod(query, className, methodName);
    }

    /**
     * 查询类中的字段并返回源代码片段
     */
    public static List<String> enhancedSearchClassField(QueryEngine query, String className, 
                                                       String fieldName) throws IOException {
        List<String> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (UniversalASTNode node : query.getAllNodes()) {
            if ("Class".equals(node.getType()) && ClassUtils.isMatchingClass(node, className)) {
                logger.debug("Found matching class: {}.{}", node.getPackageName(), node.getName());

                for (var field : node.getFields()) {
                    if (field.getName().equals(fieldName)) {
                        logger.debug("Found matching field: {} (Type: {}, Lines: {}-{})",
                                field.getName(), field.getType(), field.getStartLine(), field.getEndLine());

                        // 创建临时节点用于获取代码片段
                        UniversalASTNode fieldNode = new UniversalASTNode();
                        fieldNode.setId(String.format("%s:%s:%d", node.getFile(), field.getName(), field.getStartLine()));
                        fieldNode.setLanguage(node.getLanguage());
                        fieldNode.setType("Field");
                        fieldNode.setName(field.getName());
                        fieldNode.setFile(node.getFile());
                        fieldNode.setPackageName(node.getPackageName());
                        fieldNode.setStartLine(field.getStartLine());
                        fieldNode.setEndLine(field.getEndLine());

                        String snippet = query.getCodeSnippet(fieldNode, 1);
                        if (!seen.contains(snippet)) {
                            String result = String.format("Field: %s (Type: %s)\n%s",
                                    field.getName(), field.getType(), snippet);
                            results.add(result);
                            seen.add(snippet);
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * 格式化搜索结果
     */
    public static String formatSearchResults(List<String> results) {
        if (results.isEmpty()) {
            return "未找到匹配结果";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            builder.append(String.format("==== 结果 %d ====\n%s\n\n", i + 1, results.get(i)));
        }
        return builder.toString();
    }

    /**
     * 查找所有父类
     */
    public static List<List<com.codeaudit.model.ClassRef>> getAllSuperClasses(QueryEngine query, String className) {
        List<List<com.codeaudit.model.ClassRef>> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (UniversalASTNode node : query.getAllNodes()) {
            if ("Class".equals(node.getType()) && 
                (className.equals(node.getFullClassName()) || className.equals(node.getName()))) {
                List<com.codeaudit.model.ClassRef> allSuperClasses =
                    collectAllSuperClasses(query, node.getFullClassName(), visited);
                results.add(allSuperClasses);
            }
        }
        return results;
    }

    /**
     * 递归收集所有父类（包括间接父类）
     */
    private static List<com.codeaudit.model.ClassRef> collectAllSuperClasses(
            QueryEngine query, String className, Set<String> visited) {
        List<com.codeaudit.model.ClassRef> allSuperClasses = new ArrayList<>();

        // 查找当前类
        UniversalASTNode currentNode = null;
        for (UniversalASTNode node : query.getAllNodes()) {
            if ("Class".equals(node.getType()) && className.equals(node.getFullClassName())) {
                currentNode = node;
                break;
            }
        }

        if (currentNode == null) {
            return allSuperClasses;
        }

        // 添加直接父类
        for (com.codeaudit.model.ClassRef superClass : currentNode.getSuperClasses()) {
            String superClassName = (superClass.getPackageName() != null ? 
                    superClass.getPackageName() + "." : "") + superClass.getName();
            if (!visited.contains(superClassName)) {
                visited.add(superClassName);
                allSuperClasses.add(superClass);
                // 递归获取父类的父类
                allSuperClasses.addAll(collectAllSuperClasses(query, superClassName, visited));
            }
        }

        return allSuperClasses;
    }

    /**
     * 查找所有子类
     */
    public static List<List<com.codeaudit.model.ClassRef>> getAllSubClasses(QueryEngine query, String className) {
        List<List<com.codeaudit.model.ClassRef>> results = new ArrayList<>();
        for (UniversalASTNode node : query.getAllNodes()) {
            if ("Class".equals(node.getType()) && 
                (className.equals(node.getFullClassName()) || className.equals(node.getName()))) {
                results.add(node.getSubClasses());
            }
        }
        return results;
    }
}

