package com.codeaudit.parser;

import com.codeaudit.model.UniversalASTNode;
import java.util.List;

/**
 * 通用 AST 解析器接口
 */
public interface ASTParser {
    /**
     * 解析文件并返回 AST 节点列表
     */
    List<UniversalASTNode> parseFile(String filePath) throws Exception;

    /**
     * 返回支持的语言类型
     */
    String getLanguage();
}

