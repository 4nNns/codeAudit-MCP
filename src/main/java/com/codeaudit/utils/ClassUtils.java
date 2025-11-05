package com.codeaudit.utils;

import com.codeaudit.model.UniversalASTNode;

/**
 * 类工具类
 */
public class ClassUtils {
    /**
     * 解析全限定类名
     */
    public static ClassNameParts parseFullClassName(String fullName) {
        boolean isInner = false;
        if (fullName.contains("$")) {
            fullName = fullName.replace("$", ".");
            isInner = true;
        }

        String[] parts = fullName.split("\\.");
        if (parts.length == 0) {
            return new ClassNameParts("", "", false);
        }

        String className = parts[parts.length - 1];
        String pkgPath = "";
        if (parts.length > 1) {
            pkgPath = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1));
        }

        return new ClassNameParts(pkgPath, className, isInner);
    }

    /**
     * 检查节点是否匹配全限定类名
     */
    public static boolean isMatchingClass(UniversalASTNode node, String fullClassName) {
        ClassNameParts target = parseFullClassName(fullClassName);

        if (!"Class".equals(node.getType())) {
            return false;
        }

        String nodePkg;
        String nodeFullName;

        if (node.getFullClassName() != null && !node.getFullClassName().isEmpty()) {
            nodeFullName = node.getFullClassName();
            ClassNameParts parts = parseFullClassName(nodeFullName);
            nodePkg = parts.getPkgPath();
        } else {
            nodePkg = node.getPackageName();
            if (nodePkg != null && !nodePkg.isEmpty()) {
                nodeFullName = nodePkg + "." + node.getName();
            } else {
                nodeFullName = node.getName();
            }
        }

        // 检查包路径匹配
        if (target.getPkgPath() != null && !target.getPkgPath().isEmpty()) {
            if (!target.getPkgPath().equals(nodePkg)) {
                return false;
            }
        }

        // 检查类名匹配（支持内部类）
        if (target.isInner()) {
            return nodeFullName.equals(fullClassName.replace("$", "."));
        }

        return node.getName().equals(target.getClassName());
    }

    public static class ClassNameParts {
        private final String pkgPath;
        private final String className;
        private final boolean isInner;

        public ClassNameParts(String pkgPath, String className, boolean isInner) {
            this.pkgPath = pkgPath;
            this.className = className;
            this.isInner = isInner;
        }

        public String getPkgPath() {
            return pkgPath;
        }

        public String getClassName() {
            return className;
        }

        public boolean isInner() {
            return isInner;
        }
    }
}

