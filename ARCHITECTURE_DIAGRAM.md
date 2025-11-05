# CodeAudit-MCP-Java 架构图

## 一、系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端层 (Client Layer)                      │
│  - MCP 客户端 (Cursor, Claude Desktop 等)                         │
│  - HTTP 客户端 (curl, Postman 等)                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP/REST
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      服务器层 (Server Layer)                       │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              SSEServer (HTTP 服务器)                      │  │
│  │  - GET /tools   : 获取工具列表                            │  │
│  │  - POST /call   : 调用工具                                │  │
│  │  - GET /health  : 健康检查                                │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │              MCPServer (MCP 协议实现)                     │  │
│  │  - 工具注册                                               │  │
│  │  - 工具调用                                               │  │
│  │  - 响应处理                                               │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │              Application (主应用)                         │  │
│  │  - 配置管理                                               │  │
│  │  - 流程协调                                               │  │
│  │  - 工具处理器                                             │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      服务层 (Service Layer)                       │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │         ASTBuilderService (AST 构建服务)                  │  │
│  │  - 构建或加载 AST                                         │  │
│  │  - 缓存管理                                               │  │
│  │  - 统计信息                                               │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │      RemoteRepositoryManager (远程仓库管理)              │  │
│  │  - Git 仓库克隆                                           │  │
│  │  - ZIP 文件下载和解压                                     │  │
│  │  - 临时文件清理                                           │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      核心层 (Core Layer)                         │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │         ParserManager (解析器管理)                       │  │
│  │  - 解析器注册                                             │  │
│  │  - 目录遍历                                               │  │
│  │  - AST 索引构建                                           │  │
│  │  - 类关系填充                                             │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │         ASTIndex (AST 索引)                               │  │
│  │  - 节点存储 (HashMap)                                     │  │
│  │  - 节点查询                                               │  │
│  │  - 节点过滤                                               │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │         QueryEngine (查询引擎)                            │  │
│  │  - 类型查询                                               │  │
│  │  - 名称查询                                               │  │
│  │  - 代码片段提取                                           │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │         SearchHandler (搜索处理器)                        │  │
│  │  - 类搜索                                                 │  │
│  │  - 方法搜索                                               │  │
│  │  - 字段搜索                                               │  │
│  │  - 类层次查询                                             │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      解析层 (Parser Layer)                        │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │         ASTParser (解析器接口)                            │  │
│  │  - parseFile() : 解析文件                                 │  │
│  │  - getLanguage() : 获取语言类型                           │  │
│  └─────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐  │
│  │         JavaASTParser (Java 解析器)                        │  │
│  │  - JavaParser 解析                                        │  │
│  │  - Visitor 遍历                                           │  │
│  │  - 节点提取                                               │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      持久化层 (Persistence Layer)                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │    ASTPersistenceManager (持久化管理器)                  │  │
│  │  - 保存 AST 索引到 JSON                                   │  │
│  │  - 加载 AST 索引从 JSON                                   │  │
│  │  - 缓存文件管理                                           │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      数据层 (Data Layer)                          │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │    UniversalASTNode (统一 AST 节点)                       │  │
│  │  - 节点基本信息                                           │  │
│  │  - 字段、方法信息                                         │  │
│  │  - 继承关系信息                                           │  │
│  │  - 元数据                                                 │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │    Config (配置对象)                                     │  │
│  │  - 代码仓库路径                                           │  │
│  │  - AST 缓存配置                                           │  │
│  │  - 远程仓库配置                                           │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 二、数据流图

### 2.1 AST 构建流程

```
源代码文件
    │
    ▼
┌──────────────────┐
│  JavaASTParser   │ 使用 JavaParser 解析
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│   AST 节点提取   │  Visitor 模式遍历
│   - 类           │
│   - 方法         │
│   - 字段         │
│   - 方法调用     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ UniversalASTNode │  统一节点模型
│   - id           │
│   - type         │
│   - name         │
│   - file         │
│   - line range   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│   ASTIndex       │  索引存储
│   HashMap<String,│
│   UniversalASTNode>│
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  类关系填充      │  填充父子类关系
│  - superClasses  │
│  - subClasses    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  持久化缓存      │  保存到 JSON
│  (可选)          │
└──────────────────┘
```

### 2.2 查询流程

```
客户端请求
    │
    ▼
┌──────────────────┐
│   SSEServer      │  HTTP 接口
│   POST /call     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│   MCPServer      │  MCP 协议
│   callTool()     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Application     │  工具处理器
│  handleCodeSearch│
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ SearchHandler    │  搜索逻辑
│ unifiedSearch()  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  QueryEngine     │  查询索引
│  - findByType()  │
│  - findByName()  │
│  - getCodeSnippet│
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│   ASTIndex       │  索引查找
│   index.get()    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  代码片段提取    │  读取文件行
│  Files.readLines │
└────────┬─────────┘
         │
         ▼
    返回结果
```

## 三、类关系图

### 3.1 核心类关系

```
Application
    │
    ├──> Config (配置)
    ├──> ASTBuilderService (AST 构建服务)
    │       │
    │       ├──> ParserManager (解析器管理)
    │       │       │
    │       │       ├──> JavaASTParser (Java 解析器)
    │       │       │       │
    │       │       │       └──> ASTParser (接口)
    │       │       │
    │       │       └──> ASTIndex (索引)
    │       │               │
    │       │               └──> UniversalASTNode (节点)
    │       │
    │       └──> ASTPersistenceManager (持久化管理)
    │
    ├──> QueryEngine (查询引擎)
    │       │
    │       └──> ASTIndex
    │
    ├──> SearchHandler (搜索处理)
    │       │
    │       └──> QueryEngine
    │
    └──> SSEServer (HTTP 服务器)
            │
            └──> MCPServer (MCP 协议)
                    │
                    └──> MCPTool (工具定义)
```

### 3.2 UniversalASTNode 结构

```
UniversalASTNode
    │
    ├──> 基本信息
    │   ├── id: String
    │   ├── language: String
    │   ├── type: String
    │   ├── name: String
    │   ├── file: String
    │   ├── packageName: String
    │   ├── startLine: int
    │   └── endLine: int
    │
    ├──> 类信息（Class 类型）
    │   ├── fullClassName: String
    │   ├── isInnerClass: boolean
    │   ├── outerClass: String
    │   ├── fields: List<FieldInfo>
    │   ├── superClasses: List<ClassRef>
    │   └── subClasses: List<ClassRef>
    │
    ├──> 方法信息（Method 类型）
    │   └── methodParams: List<String>
    │
    └──> 元数据
        ├── metadata: Map<String, String>
        └── relations: List<Relation>
```

## 四、工具调用流程

### 4.1 remote_code_audit 工具

```
1. 客户端调用
   POST /call
   {
     "tool": "remote_code_audit",
     "arguments": {
       "repository_url": "git:https://github.com/user/repo.git",
       "branch": "main"
     }
   }
   │
   ▼
2. SSEServer.CallHandler.handle()
   │
   ▼
3. MCPServer.callTool()
   │
   ▼
4. Application.handleRemoteCodeAudit()
   │
   ├──> RemoteRepositoryManager.downloadAndPrepare()
   │       │
   │       ├──> 下载 Git/ZIP 仓库
   │       │
   │       └──> 返回本地路径
   │
   ├──> Application.initializeAST()
   │       │
   │       └──> ASTBuilderService.buildOrLoadAST()
   │
   └──> 返回统计信息
```

### 4.2 code_search 工具

```
1. 客户端调用
   POST /call
   {
     "tool": "code_search",
     "arguments": {
       "className": "com.example.User",
       "methodName": "getUser"
     }
   }
   │
   ▼
2. Application.handleCodeSearch()
   │
   ▼
3. SearchHandler.unifiedSearch()
   │
   ├──> QueryEngine 查询索引
   │       │
   │       ├──> 查找匹配的类
   │       │
   │       └──> 查找匹配的方法
   │
   ├──> QueryEngine.getCodeSnippet()
   │       │
   │       └──> 读取文件行范围
   │
   └──> SearchHandler.formatSearchResults()
           │
           └──> 返回格式化的代码片段
```

### 4.3 class_hierarchy 工具

```
1. 客户端调用
   POST /call
   {
     "tool": "class_hierarchy",
     "arguments": {
       "className": "com.example.User",
       "type": "super"
     }
   }
   │
   ▼
2. Application.handleClassHierarchy()
   │
   ▼
3. SearchHandler.getAllSuperClasses()
   │
   ├──> 查找目标类
   │
   ├──> 递归收集所有父类
   │       │
   │       └──> collectAllSuperClasses()
   │
   └──> 返回父类链列表
```

## 五、状态转换图

### 5.1 应用启动状态

```
[未初始化]
    │
    ▼
[加载配置]
    │
    ▼
[处理远程仓库] (可选)
    │
    ▼
[初始化 AST]
    │
    ├──> [检查缓存]
    │       │
    │       ├──> [缓存存在] ──> [加载缓存] ──> [就绪]
    │       │
    │       └──> [缓存不存在] ──> [构建 AST] ──> [保存缓存] ──> [就绪]
    │
    └──> [就绪]
            │
            ▼
        [启动 HTTP 服务器]
            │
            ▼
        [等待请求]
```

### 5.2 AST 索引状态

```
[空索引]
    │
    ▼
[构建中]
    │
    ├──> [遍历文件]
    │       │
    │       ├──> [解析文件]
    │       │       │
    │       │       └──> [添加节点]
    │       │
    │       └──> [继续遍历]
    │
    └──> [填充关系]
            │
            ▼
        [索引完成]
            │
            ▼
        [保存缓存] (可选)
            │
            ▼
        [就绪]
```

## 六、关键算法

### 6.1 类层次关系填充算法

```
Algorithm: fillSubClasses()
1. 构建全限定名到节点ID的映射表
   FOR each node in index:
       IF node.type == "Class" AND node.fullClassName != null:
           classMap[node.fullClassName] = node.id

2. 遍历所有类节点
   FOR each node in index:
       IF node.type != "Class":
           CONTINUE
       
       FOR each superClass in node.superClasses:
           superClassName = buildFullName(superClass)
           parentId = classMap[superClassName]
           
           IF parentId != null:
               parentNode = index.getNode(parentId)
               IF parentNode != null AND not alreadyAdded:
                   parentNode.subClasses.add(node.classRef)
```

### 6.2 代码搜索算法

```
Algorithm: searchClassMethod(className, methodName)
1. 查找匹配的类
   targetClasses = []
   FOR each node in query.getAllNodes():
       IF node.type == "Class" AND isMatchingClass(node, className):
           targetClasses.add(node)

2. 在类范围内查找方法
   results = []
   FOR each classNode in targetClasses:
       FOR each node in query.getAllNodes():
           IF node.type == "Method" AND
              node.file == classNode.file AND
              node.startLine >= classNode.startLine AND
              node.endLine <= classNode.endLine:
               IF isMatchingMethod(node, methodName):
                   snippet = query.getCodeSnippet(node, contextLines)
                   results.add(snippet)
   
   RETURN results
```

### 6.3 递归父类收集算法

```
Algorithm: collectAllSuperClasses(className, visited)
1. 查找当前类节点
   currentNode = null
   FOR each node in query.getAllNodes():
       IF node.type == "Class" AND node.fullClassName == className:
           currentNode = node
           BREAK

2. 递归收集父类
   allSuperClasses = []
   FOR each superClass in currentNode.superClasses:
       superClassName = buildFullName(superClass)
       IF superClassName NOT IN visited:
           visited.add(superClassName)
           allSuperClasses.add(superClass)
           // 递归获取父类的父类
           allSuperClasses.addAll(
               collectAllSuperClasses(superClassName, visited)
           )
   
   RETURN allSuperClasses
```
