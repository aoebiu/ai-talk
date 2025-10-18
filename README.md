# AI Talk

**AI Talk** 是一个智能对话系统，支持动态配置各种模型与工具（Tools），提供文档向量化、RAG 检索增强生成（Retrieval-Augmented Generation）以及流式对话输出功能。

> ⚠️ 当前项目仍在搭建中，部分功能尚不完善，但已可进行体验。

动态配置tools,并且可以通过js发起http请求,并拿到结果
```jshelllanguage
function execute(params) {
    const userId = params.userId;
    const url = 'https://jsonplaceholder.typicode.com/posts?userId=' + userId;
    const responseStr = http.get(url);
    const responseObj = JSON.parse(responseStr);
    const posts = JSON.parse(responseObj.body);
    console.log(posts);

    const count = Array.isArray(posts) ? posts.length : 0;
    const v  = JSON.stringify({
        userId: userId,
        postCount: count,
        message: '用户 ' + userId + ' 共有 ' + count + ' 篇帖子',
        posts: posts
    });
    return v;
}
```


---

## 技术栈

- JVM 与运行环境：GraalVM 17
- 后端框架：Spring Boot 3.5.6
- AI 框架：LangChain4j 1.6
- 存储与检索：Elasticsearch、MySQL
- 脚本执行：GraalJS（需安装 JS 引擎）

安装 GraalJS：
```bash
sudo ${JAVA_HOME}/lib/installer/bin/gu install js
```

---

## 支持的模型

- 通义千问：chat、streaming_chat、embedding
- Ollama：chat、streaming_chat、embedding
- Cohere：scoring

---

## 快速上手

1. 准备第三方客户端，并配置 API 地址：
```
http://localhost:7900
```
2. 修改 ai-talk-server 的 `application.yaml` 中 MySQL 与 Elasticsearch 配置，或通过环境变量注入
3. 搭建环境（未来将支持一键 Shell 部署）
4. 定位到 `sql/schema.sql`，修改 `chat_api_key` 为自己的 API Key
5. 执行 `sql/schema.sql` 创建数据库表与初始化数据
6. 启动 ai-talk-server

---

## 快速示例

启动成功后，可使用第三方客户端调用 API 与模型进行对话或向量检索

---

## 问题反馈

如遇问题或有建议，请提交 Issue 或联系：
邮箱：LTL1510@126.com
