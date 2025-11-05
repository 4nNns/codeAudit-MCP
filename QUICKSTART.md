# 🚀 快速开始指南

## ✅ 已完成的工作

1. ✅ **分析了 Fenrir-CodeAuditTool（Go 版本）源码**
2. ✅ **创建了完整的 Java 版本实现**
3. ✅ **配置了 Cursor MCP 服务器**
4. ✅ **生成了 testnet-java-main 项目的安全审计报告**

## 📁 项目位置

```
/Users/liuhao/H4Tools/CTF/Fenrir-CodeAuditTool-Java/
```

## 🎯 现在开始使用

### 第 1 步: 重启 Cursor

**重要**: 完全关闭并重新打开 Cursor 编辑器

### 第 2 步: 在 Cursor 中测试

重启后，在 Cursor 聊天界面输入：

```
请帮我搜索 testnet-java-main 项目中所有使用 Runtime.getRuntime 的代码
```

或者：

```
请查找 JeecgController 的所有子类
```

### 第 3 步: 审计新项目

要审计其他项目，修改配置文件 `~/.cursor/mcp.json` 中的路径：

```json
{
  "mcpServers": {
    "fenrir": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/liuhao/H4Tools/CTF/Fenrir-CodeAuditTool-Java/target/fenrir-code-audit-tool-1.2.0-jar-with-dependencies.jar",
        "--stdio",
        "-i",
        "/path/to/your/code/project"  // 改为你的项目路径
      ]
    }
  }
}
```

然后重启 Cursor。

## 📊 生成的审计报告

我已经生成了 testnet-java-main 项目的详细审计报告：

**文件**: `TESTNET_SECURITY_AUDIT_REPORT.md`

### 主要发现

🔴 **Critical**:
- Fastjson 1.2.83 反序列化漏洞（可导致 RCE）
- MyBatis SQL 注入风险
- 弱权限控制

🟡 **Medium-High**:
- 输入验证不足
- 配置不当
- 敏感信息泄露风险

## 🛠️ 可用工具

### 1. code_search
搜索类、方法、字段

**在 Cursor 中使用**:
```
请使用 code_search 工具搜索类 ApiController
```

### 2. class_hierarchy
查找类的父类或子类

**在 Cursor 中使用**:
```
请查找 JeecgController 的所有子类
```

### 3. remote_code_audit
从远程仓库下载并审计代码

**在 Cursor 中使用**:
```
请使用 remote_code_audit 工具分析 GitHub 上的某个项目
```

## 📝 文档清单

- ✅ `README.md` - 项目说明
- ✅ `SUMMARY.md` - 实现总结
- ✅ `QUICKSTART.md` - 本文件
- ✅ `HOW_TO_USE_IN_CURSOR.md` - Cursor 使用指南
- ✅ `TESTNET_SECURITY_AUDIT_REPORT.md` - 审计报告
- ✅ `FINAL_SUMMARY.md` - 最终总结

## 🔧 常用命令

### 编译项目
```bash
cd /Users/liuhao/H4Tools/CTF/Fenrir-CodeAuditTool-Java
mvn clean package
```

### 运行服务器（HTTP 模式）
```bash
java -jar target/fenrir-code-audit-tool-1.2.0-jar-with-dependencies.jar \
  -i /path/to/code
```

### 运行服务器（stdio 模式，Cursor 会自动调用）
```bash
java -jar target/fenrir-code-audit-tool-1.2.0-jar-with-dependencies.jar \
  --stdio -i /path/to/code
```

## 🎉 完成

现在你拥有：
- ✅ 功能完整的 Fenrir Java 版本
- ✅ 与 Cursor 完美集成
- ✅ testnet-java-main 的完整审计报告

**立即重启 Cursor，开始使用！**

