package com.codeaudit.core;

import com.codeaudit.model.UniversalASTNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一索引结构
 */
public class ASTIndex {
    private final Map<String, UniversalASTNode> index;

    public ASTIndex() {
        this.index = new HashMap<>();
    }

    /**
     * 添加节点到索引
     */
    public void addNode(UniversalASTNode node) {
        // 自动生成全限定类名
        if ("Class".equals(node.getType())) {
            if (node.getPackageName() != null && !node.getPackageName().isEmpty()) {
                node.setFullClassName(node.getPackageName() + "." + node.getName());
            } else {
                node.setFullClassName(node.getName());
            }
        }

        // 处理内部类标识
        if (node.getName() != null && node.getName().contains("$")) {
            node.setInnerClass(true);
            String[] parts = node.getName().split("\\$");
            if (parts.length > 0) {
                node.setOuterClass(parts[0]);
                node.setName(parts[parts.length - 1]);
            }
        }

        index.put(node.getId(), node);
    }

    /**
     * 获取节点
     */
    public UniversalASTNode getNode(String id) {
        return index.get(id);
    }

    /**
     * 查找节点
     */
    public List<UniversalASTNode> findNodes(java.util.function.Predicate<UniversalASTNode> filter) {
        return index.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有节点
     */
    public Map<String, UniversalASTNode> getAllNodes() {
        return new HashMap<>(index);
    }

    /**
     * 获取节点数量
     */
    public int size() {
        return index.size();
    }
}

