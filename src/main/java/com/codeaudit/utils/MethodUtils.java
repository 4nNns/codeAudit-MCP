package com.codeaudit.utils;

import com.codeaudit.model.UniversalASTNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 方法工具类
 */
public class MethodUtils {
    /**
     * 解析方法签名为方法名和参数列表
     */
    public static MethodSignature parseMethodSignature(String methodSignature) {
        if (!methodSignature.contains("(")) {
            return new MethodSignature(methodSignature, new ArrayList<>());
        }

        String[] parts = methodSignature.split("\\(", 2);
        String methodName = parts[0];

        if (parts.length < 2) {
            return new MethodSignature(methodName, new ArrayList<>());
        }

        String paramsStr = parts[1];
        if (paramsStr.endsWith(")")) {
            paramsStr = paramsStr.substring(0, paramsStr.length() - 1);
        }

        if (paramsStr.trim().isEmpty()) {
            return new MethodSignature(methodName, new ArrayList<>());
        }

        // 分割参数
        List<String> params = new ArrayList<>();
        StringBuilder currentParam = new StringBuilder();
        int bracketCount = 0;

        for (char c : paramsStr.toCharArray()) {
            switch (c) {
                case '<':
                    bracketCount++;
                    currentParam.append(c);
                    break;
                case '>':
                    bracketCount--;
                    currentParam.append(c);
                    break;
                case ',':
                    if (bracketCount == 0) {
                        String paramType = extractParamType(currentParam.toString().trim());
                        if (!paramType.isEmpty()) {
                            params.add(paramType);
                        }
                        currentParam.setLength(0);
                    } else {
                        currentParam.append(c);
                    }
                    break;
                default:
                    currentParam.append(c);
            }
        }

        // 处理最后一个参数
        if (currentParam.length() > 0) {
            String paramType = extractParamType(currentParam.toString().trim());
            if (!paramType.isEmpty()) {
                params.add(paramType);
            }
        }

        return new MethodSignature(methodName, params);
    }

    /**
     * 从参数字符串中提取参数类型（去掉参数名）
     */
    private static String extractParamType(String paramStr) {
        String[] parts = paramStr.trim().split("\\s+");
        if (parts.length > 1) {
            // 假设最后一个部分是参数名，前面的部分是类型
            return String.join(" ", Arrays.copyOf(parts, parts.length - 1));
        }
        return paramStr;
    }

    /**
     * 检查方法是否匹配指定的方法签名
     */
    public static boolean isMatchingMethod(UniversalASTNode node, String methodSignature) {
        MethodSignature target = parseMethodSignature(methodSignature);

        // 检查方法名是否匹配
        if (!target.getMethodName().equals(node.getName())) {
            return false;
        }

        // 如果没有参数要求，直接返回true
        if (target.getParams().isEmpty()) {
            return true;
        }

        // 检查参数数量是否匹配
        if (node.getMethodParams().size() != target.getParams().size()) {
            return false;
        }

        // 检查每个参数是否匹配
        for (int i = 0; i < target.getParams().size(); i++) {
            String nodeParam = node.getMethodParams().get(i).trim();
            String targetParam = target.getParams().get(i).trim();

            // 简化的类型比较（可以进一步优化）
            if (!nodeParam.equalsIgnoreCase(targetParam)) {
                // 处理简化的类名匹配
                if (!shortClassName(nodeParam).equalsIgnoreCase(shortClassName(targetParam))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static String shortClassName(String fullName) {
        int idx = fullName.lastIndexOf('.');
        if (idx >= 0) {
            return fullName.substring(idx + 1);
        }
        return fullName;
    }

    public static class MethodSignature {
        private final String methodName;
        private final List<String> params;

        public MethodSignature(String methodName, List<String> params) {
            this.methodName = methodName;
            this.params = params;
        }

        public String getMethodName() {
            return methodName;
        }

        public List<String> getParams() {
            return params;
        }
    }
}

