# AI Talk

**AI Talk** 是一个智能对话系统，支持动态配置各种模型与工具（Tools），提供文档向量化、RAG 检索增强生成（Retrieval-Augmented Generation）以及流式对话输出功能。

> ⚠️ 当前项目仍在搭建中，部分功能尚不完善，但已可进行体验。
>
>
> 
### 问题反馈

如遇问题或有建议，请提交 Issue 或 联系邮箱：LTL1510@126.com

## 技术栈

- JVM 与运行环境：GraalVM 17（需安装 GraalJS）
- 后端框架：Spring Boot 3.5.6
- AI 框架：LangChain4j 1.6
- 存储与检索：Elasticsearch、MySQL

安装 GraalJS：
```bash
sudo ${JAVA_HOME}/lib/installer/bin/gu install js
```

---

### 目前支持以下模型

- 通义千问：chat、streaming_chat、embedding
- Ollama：chat、streaming_chat、embedding
- Cohere：scoring
- OpenAI：moderate

---


# 简单快速上手

1. 准备第三方客户端，并配置 API 地址：
```
http://localhost:7900
```
2. 修改 ai-talk-server 的 `application.yaml` 中 MySQL 与 Elasticsearch 配置，或通过环境变量注入
3. 搭建环境（TODO 未来将支持一键 Shell 部署，并且会准备一套前端页面和对应的接口）
4. 定位到 `sql/schema.sql`，修改 `chat_api_key` 为自己的 API Key
5. 执行 `sql/schema.sql` 创建数据库表与初始化数据
6. 启动 ai-talk-server


# Tools 配置与动态扩展方案

在传统项目中，工具（Tool）通常通过 **硬编码** 的方式定义在业务代码中，例如：

```java
@Service
public class ToolService {

    @Tool("获取帖子")
    public String changshaWeather(@P(value = "userId", required = true) Long userId) {
        JSONObject responseJson = new HttpClients()
            .get("https://jsonplaceholder.typicode.com/posts?userId=" + userId);

        UserPost userPost = responseJson.toBean(UserPost.class);
        // 省略其他业务逻辑
        return userPost;
    }
}
```

这种方式存在明显问题：

- 工具逻辑被固定在代码中，缺乏灵活性
- 无法根据业务变化动态调整或新增 Tool
- 每次修改都需要重新编译、部署

---

### 动态配置 Tools

为了让工具具备更强的扩展性，只需定义 `ToolDescription`，即可让 LLM 根据描述自动选择、调用对应工具，而无需改动 Java 代码。

- 每个工具只需提供 **名称 + 描述 + 参数定义 + 执行脚本**
- 模型可根据上下文自主选择合适的 Tool
- 支持快速扩展与热加载

这样，传统企业的业务逻辑可以从「代码驱动」转变为「配置驱动 + 智能调用」，  
极大提升灵活性与维护效率。

```java
@Data
public class ToolDescription {

    /** 工具名称 */
    private String name;

    /** 工具功能描述 */
    private String description;

    /** 参数描述（键为参数名，值为说明） */
    private Map<String, String> property;

    /** 必填参数 */
    private List<String> required;

    /** 执行逻辑（JavaScript 脚本） */
    private String execute;
}
```

---

### 动态执行示例

下面的示例展示了一个通过内置 `http` 对象发起请求、解析结果并返回统计信息的脚本。

TODO 未来会支持自然语言转JavaScripts

```js
function execute(params) {
    const userId = params.userId;
    const url = 'https://jsonplaceholder.typicode.com/posts?userId=' + userId;

    const responseStr = http.get(url);
    const responseObj = JSON.parse(responseStr);
    const posts = JSON.parse(responseObj.body);

    console.log(posts);

    const count = Array.isArray(posts) ? posts.length : 0;

    return JSON.stringify({
        userId: userId,
        postCount: count,
        message: '用户 ' + userId + ' 共有 ' + count + ' 篇帖子'
    });
}
```

---

# RAG

AI Talk 提供完整的 RAG 能力，支持文档上传、智能分割、向量化存储与检索增强对话。

## 核心流程

```
文件上传 → 保存到本地 → 智能解析 → 按类型分割 → 生成向量 → 存入 Elasticsearch → 检索增强对话
```

## 快速使用

### 上传文档并向量化

使用文档上传接口将文件进行向量化处理：


**指定文档类型上传**：
```bash
curl --location 'http://localhost:7900/api/documents/upload?type=paper' \
--form 'file=@"/path/to/your/document.pdf"'
```

**支持的文档类型**：

| 类型 | 说明 | 分块大小 | 重叠大小 | 适用场景 |
|------|------|----------|----------|----------|
| `short_text` | 短文本 | 150 字符 | 20 字符 | 新闻、博客、短文档 |
| `paper` | 论文 | 400 字符 | 40 字符 | 学术论文、研究报告 |
| `contract` | 合同 | 300 字符 | 0 字符 | 法律文档、合同协议 |
| `novel` | 小说 | 750 字符 | 50 字符 | 小说、长篇叙事文档 |
| `default` | 默认 | 300 字符 | 50 字符 | 通用文档 |

**支持的文件格式**：
- **PDF 文档**（`.pdf`）
- **Office 文档**（`.doc`, `.docx`, `.ppt`, `.pptx`, `.xls`, `.xlsx`）
- **文本文档**（`.md`, `.txt`）


### 配置 RAG

在对话配置中启用 RAG 功能，系统将自动：
- 根据用户问题检索相关文档片段
- 将检索结果作为上下文注入对话
- 生成基于文档内容的准确回答

### 开始对话

启动成功后，使用第三方客户端调用 API，系统将自动结合文档内容进行智能对话。


