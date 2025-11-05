package com.codeaudit.parser;

import com.codeaudit.model.ClassRef;
import com.codeaudit.model.FieldInfo;
import com.codeaudit.model.UniversalASTNode;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Java AST 解析器实现
 */
public class JavaASTParser implements ASTParser {
    private static final Logger logger = LoggerFactory.getLogger(JavaASTParser.class);

    public JavaASTParser() {
        // 配置 JavaParser
        ParserConfiguration config = new ParserConfiguration();
        TypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
        config.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        StaticJavaParser.setConfiguration(config);
    }

    @Override
    public String getLanguage() {
        return "java";
    }

    @Override
    public List<UniversalASTNode> parseFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        String code = Files.readString(file.toPath());
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(code);
        } catch (Exception e) {
            logger.warn("解析失败: {}", filePath, e);
            return Collections.emptyList();
        }
        List<UniversalASTNode> nodes = new ArrayList<>();

        // 提取包名
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("default.package");

        // 收集 import 语句
        Map<String, String> importMap = new HashMap<>();
        List<String> importStar = new ArrayList<>();
        for (ImportDeclaration imp : cu.getImports()) {
            String importPath = imp.getNameAsString();
            if (imp.isAsterisk()) {
                importStar.add(importPath);
            } else {
                String[] parts = importPath.split("\\.");
                String simpleName = parts[parts.length - 1];
                importMap.put(simpleName, importPath);
            }
        }

        // 访问 AST 节点
        cu.accept(new JavaASTVisitor(filePath, packageName, importMap, importStar, nodes), null);

        return nodes;
    }

    /**
     * Java AST 访问器
     */
    private static class JavaASTVisitor extends VoidVisitorAdapter<Void> {
        private final String filePath;
        private final String packageName;
        private final Map<String, String> importMap;
        private final List<String> importStar;
        private final List<UniversalASTNode> nodes;

        public JavaASTVisitor(String filePath, String packageName,
                             Map<String, String> importMap, List<String> importStar,
                             List<UniversalASTNode> nodes) {
            this.filePath = filePath;
            this.packageName = packageName;
            this.importMap = importMap;
            this.importStar = importStar;
            this.nodes = nodes;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            super.visit(n, arg);

            String className = n.getNameAsString();
            String id = String.format("%s:%s:%d", filePath, className, n.getBegin().get().line);

            UniversalASTNode classNode = new UniversalASTNode();
            classNode.setId(id);
            classNode.setLanguage("java");
            classNode.setType(n.isInterface() ? "Interface" : "Class");
            classNode.setName(className);
            classNode.setFile(filePath);
            classNode.setPackageName(packageName);
            classNode.setStartLine(n.getBegin().map(pos -> pos.line).orElse(0));
            classNode.setEndLine(n.getEnd().map(pos -> pos.line).orElse(0));

            // 收集字段
            List<FieldInfo> fields = new ArrayList<>();
            for (FieldDeclaration field : n.getFields()) {
                Type fieldType = field.getCommonType();
                for (VariableDeclarator var : field.getVariables()) {
                    FieldInfo fieldInfo = new FieldInfo();
                    fieldInfo.setName(var.getNameAsString());
                    fieldInfo.setType(fieldType.asString());
                    fieldInfo.setStartLine(var.getBegin().map(pos -> pos.line).orElse(0));
                    fieldInfo.setEndLine(var.getEnd().map(pos -> pos.line).orElse(0));
                    
                    List<String> modifiers = new ArrayList<>();
                    for (Modifier mod : field.getModifiers()) {
                        modifiers.add(mod.getKeyword().asString());
                    }
                    fieldInfo.setModifiers(modifiers);
                    
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("fullType", fieldType.asString());
                    fieldInfo.setMetadata(metadata);
                    
                    fields.add(fieldInfo);
                }
            }
            classNode.setFields(fields);

            // 获取父类和接口
            List<ClassRef> superClasses = new ArrayList<>();
            
            // 处理 extends
            n.getExtendedTypes().forEach(ext -> {
                String superType = ext.getNameAsString();
                String fullName = resolveFullName(superType);
                ClassRef classRef = parseClassRef(fullName);
                superClasses.add(classRef);
            });

            // 处理 implements
            n.getImplementedTypes().forEach(impl -> {
                String interfaceType = impl.getNameAsString();
                String fullName = resolveFullName(interfaceType);
                ClassRef classRef = parseClassRef(fullName);
                superClasses.add(classRef);
            });

            classNode.setSuperClasses(superClasses);
            
            // 保存父类信息到 metadata（兼容性）
            if (!superClasses.isEmpty()) {
                String superClassesStr = superClasses.stream()
                        .map(sc -> (sc.getPackageName() != null ? sc.getPackageName() + "." : "") + sc.getName())
                        .collect(java.util.stream.Collectors.joining(","));
                classNode.getMetadata().put("superClasses", superClassesStr);
            }

            logger.debug("Found class: {}.{} (Fields: {})", packageName, className, fields.size());
            nodes.add(classNode);
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            super.visit(n, arg);

            String methodName = n.getNameAsString();
            String id = String.format("%s:%s:%d", filePath, methodName, n.getBegin().get().line);

            // 获取方法参数
            List<String> methodParams = new ArrayList<>();
            for (Parameter param : n.getParameters()) {
                methodParams.add(param.getType().asString());
            }

            // 获取返回类型
            String returnType = "void";
            if (n.getType() != null) {
                returnType = n.getType().asString();
            }

            UniversalASTNode methodNode = new UniversalASTNode();
            methodNode.setId(id);
            methodNode.setLanguage("java");
            methodNode.setType("Method");
            methodNode.setName(methodName);
            methodNode.setFile(filePath);
            methodNode.setPackageName(packageName);
            methodNode.setStartLine(n.getBegin().map(pos -> pos.line).orElse(0));
            methodNode.setEndLine(n.getEnd().map(pos -> pos.line).orElse(0));
            methodNode.setMethodParams(methodParams);
            methodNode.getMetadata().put("returnType", returnType);

            logger.debug("Found method: {}.{} (Return: {}, Params: {})", 
                    packageName, methodName, returnType, methodParams);
            nodes.add(methodNode);
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            super.visit(n, arg);

            String methodName = n.getNameAsString();
            String id = String.format("%s:%s:%d", filePath, methodName, n.getBegin().get().line);

            List<String> methodParams = new ArrayList<>();
            for (Parameter param : n.getParameters()) {
                methodParams.add(param.getType().asString());
            }

            UniversalASTNode methodNode = new UniversalASTNode();
            methodNode.setId(id);
            methodNode.setLanguage("java");
            methodNode.setType("Method");
            methodNode.setName(methodName);
            methodNode.setFile(filePath);
            methodNode.setPackageName(packageName);
            methodNode.setStartLine(n.getBegin().map(pos -> pos.line).orElse(0));
            methodNode.setEndLine(n.getEnd().map(pos -> pos.line).orElse(0));
            methodNode.setMethodParams(methodParams);
            methodNode.getMetadata().put("returnType", "<constructor>");

            nodes.add(methodNode);
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);

            String methodName = n.getNameAsString();
            String id = String.format("%s:%s:%d", filePath, methodName, 
                    n.getBegin().map(pos -> pos.line).orElse(0));

            UniversalASTNode callNode = new UniversalASTNode();
            callNode.setId(id);
            callNode.setLanguage("java");
            callNode.setType("MethodCall");
            callNode.setName(methodName);
            callNode.setFile(filePath);
            callNode.setPackageName(packageName);
            callNode.setStartLine(n.getBegin().map(pos -> pos.line).orElse(0));
            callNode.setEndLine(n.getEnd().map(pos -> pos.line).orElse(0));

            nodes.add(callNode);
        }

        /**
         * 解析类名引用
         */
        private ClassRef parseClassRef(String fullName) {
            int lastDot = fullName.lastIndexOf('.');
            if (lastDot >= 0) {
                return new ClassRef(fullName.substring(0, lastDot), fullName.substring(lastDot + 1));
            }
            return new ClassRef("", fullName);
        }

        /**
         * 解析类型名称为全限定名
         */
        private String resolveFullName(String typeName) {
            // 如果已经是全限定名（包含点）
            if (typeName.contains(".") && !typeName.startsWith("java.")) {
                return typeName;
            }

            // 检查 import map
            if (importMap.containsKey(typeName)) {
                return importMap.get(typeName);
            }

            // 检查 import star
            for (String pkg : importStar) {
                String candidate = pkg + "." + typeName;
                // 这里可以进一步验证类是否存在
                return candidate;
            }

            // 默认使用当前包
            if (packageName != null && !packageName.isEmpty()) {
                return packageName + "." + typeName;
            }

            return typeName;
        }
    }
}
