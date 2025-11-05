# CodeAudit-MCP代码审计工具 - Java 版本

CodeAudit-MCP 是一个基于 MCP 协议与 AST 技术的代码审计工具，这是其 Java 实现版本。

## 功能特性

1. **AST 抽象语法树** - 使用 JavaParser 解析 Java 代码构建 AST
2. **代码结构分析** - 基于 AST 实现类、方法、字段的精确查找
3. **MCP 协议支持** - 通过 HTTP 接口提供 MCP 工具
4. **高效的 AST 缓存机制** - 支持 AST 索引的持久化缓存
5. **远程仓库支持** - 支持从 ZIP URL 或 Git 仓库下载代码

## 系统要求

- JDK 17 或更高版本
- Maven 3.6+

## 安装说明

### 1. 克隆或下载项目

```bash
cd codeAudit-MCP
```

### 2. 编译项目

```bash
mvn clean package
```

编译完成后，可执行 JAR 文件位于 `target/codeAudit-mcp-1.2.0.jar`

## 使用方法

### 1. 运行服务器

```bash
java -jar target/codeAudit-mcp-1.2.0.jar -i "/path/to/code/repository"
```

或者使用远程仓库：

```bash
java -jar target/codeAudit-mcp-1.2.0.jar -remote "git:https://github.com/user/repo.git" -branch "main"
```

### 2. 使用 MCP 客户端

服务器启动后，可以通过 HTTP 接口调用工具：

- **获取工具列表**: `GET http://localhost:8338/tools`
- **调用工具**: `POST http://localhost:8338/call`

### 3. 可用工具

#### remote_code_audit
从远程仓库下载代码并进行自动化代码审计。

```json
{
  "tool": "remote_code_audit",
  "arguments": {
    "repository_url": "zip:https://example.com/repo.zip",
    "branch": "main"
  }
}
```

#### code_search
基于 AST 的代码搜索工具。

```json
{
  "tool": "code_search",
  "arguments": {
    "className": "com.example.MyClass",
    "methodName": "myMethod",
    "fieldName": ""
  }
}
```

#### class_hierarchy
查找指定类的所有父类或所有子类。

```json
{
  "tool": "class_hierarchy",
  "arguments": {
    "className": "com.example.MyClass",
    "type": "super"
  }
}
```

## 配置文件

默认配置文件位于 `resources/config.yaml`：

```yaml
code_audit:
  repository_path: ""
  ast_cache:
    enabled: true
    cache_dir: "./cache"
    rebuild_on_startup: false

remote_repository:
  enabled: false
  type: ""
  url: ""
  branch: "main"
  target_path: "/tmp/codeAudit_remote"
  auto_clean: true
```

## 项目结构

```
codeAudit-mcp/
├── src/main/java/com/codeaudit/
│   ├── config/          # 配置管理
│   ├── core/            # 核心功能（ASTIndex, QueryEngine, ParserManager）
│   ├── model/           # 数据模型
│   ├── parser/          # AST 解析器
│   ├── persistence/     # AST 缓存持久化
│   ├── remote/          # 远程仓库管理
│   ├── search/          # 搜索功能
│   ├── server/           # 服务器（MCP HTTP）
│   ├── service/         # 服务层
│   └── utils/           # 工具类
├── resources/            # 配置文件
└── pom.xml              # Maven 配置
```

## 技术栈

- **JavaParser** - Java 代码解析
- **Jackson** - JSON 处理
- **SnakeYAML** - YAML 配置解析
- **Eclipse JGit** - Git 仓库克隆
- **Java HTTP Server** - MCP 服务器

## 注意事项

- 目前仅支持对 Java 代码构建 AST 索引（包括反编译代码）
- 实际审计效果依赖于大模型的能力
- 大项目首次构建 AST 索引可能需要较长时间

## 许可证

本项目采用 Apache 2.0 许可证

