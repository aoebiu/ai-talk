CREATE DATABASE IF NOT EXISTS `dialoger_ai`;
USE `dialoger_ai`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chat_api_key
-- ----------------------------
DROP TABLE IF EXISTS `chat_api_key`;
CREATE TABLE `chat_api_key`
(
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `key_type`       varchar(50)  NOT NULL COMMENT 'API Key类型: chat, streaming_chat, embedding',
    `api_key`        varchar(500) NOT NULL COMMENT 'API Key',
    `model_name`     varchar(255) NOT NULL COMMENT '模型名称',
    `model_provider` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `created_at`     timestamp    NULL                                             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     timestamp    NULL                                             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='API Key配置表';

-- ----------------------------
-- Records of chat_api_key
-- ----------------------------
BEGIN;
INSERT INTO `chat_api_key` (`id`, `key_type`, `api_key`, `model_name`, `model_provider`)
VALUES (1, 'chat', '', 'qwen-turbo', 'Qwen');
INSERT INTO `chat_api_key` (`id`, `key_type`, `api_key`, `model_name`, `model_provider`)
VALUES (2, 'streaming_chat', '', 'qwen-turbo', 'Qwen');
INSERT INTO `chat_api_key` (`id`, `key_type`, `api_key`, `model_name`, `model_provider`)
VALUES (3, 'embedding', '', 'text-embedding-v2', 'Qwen');
INSERT INTO `chat_api_key` (`id`, `key_type`, `api_key`, `model_name`, `model_provider`)
VALUES (5, 'scoring', '', 'rerank-multilingual-v3.0', 'cohere');
COMMIT;

-- ----------------------------
-- Table structure for chat_messages
-- ----------------------------
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages`
(
    `id`         bigint(20)                                                     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` varchar(255)                                                   NOT NULL COMMENT '会话ID',
    `role`       varchar(50)                                                    NOT NULL COMMENT '角色: USER, ASSISTANT, SYSTEM',
    `content`    varchar(2550) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
    `extras`     json                                                           NULL COMMENT '扩展字段 JSON（thinking、toolCalls、tool 结果 id 等）',
    `created_at` timestamp                                                      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='聊天消息表';

-- ----------------------------
-- Records of chat_messages
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for chat_option
-- ----------------------------
DROP TABLE IF EXISTS `chat_option`;
CREATE TABLE `chat_option`
(
    `id`                      bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                    varchar(255) NOT NULL COMMENT '配置名称',
    `rag`                     tinyint(4)        DEFAULT '0',
    `tools`                   tinyint(1)        DEFAULT '0',
    `max_messages`            int(11)           DEFAULT '10' COMMENT '最大消息窗口数',
    `enabled`                 tinyint(1)        DEFAULT '1' COMMENT '是否启用',
    `transform`               varchar(100)      DEFAULT NULL COMMENT 'Query Transformer 类型',
    `content_injector_prompt` text COMMENT 'Content Injector 提示词模板',
    `content_aggregator`      tinyint(1)        DEFAULT '0',
    `max_results`             int(11)           DEFAULT '5' COMMENT '检索最大结果数',
    `min_score`               double            DEFAULT '0.7' COMMENT '检索最小相似度分数',
    `enable_text_search`      tinyint(1)        DEFAULT '1' COMMENT '是否启用文本检索',
    `enable_vector_search`    tinyint(1)        DEFAULT '1' COMMENT '是否启用向量检索',
    `in_DB`                   tinyint(1)        DEFAULT '1' COMMENT '是否入库',
    `remark`                  text COMMENT '备注',
    `created_at`              timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`              timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='聊天配置表';

-- ----------------------------
-- Records of chat_option
-- ----------------------------
BEGIN;
INSERT INTO `chat_option` (`id`, `name`, `rag`, `tools`, `max_messages`, `enabled`, `transform`,
                           `content_injector_prompt`, `content_aggregator`, `max_results`, `min_score`,
                           `enable_text_search`, `enable_vector_search`, `in_DB`, `remark`)
VALUES (1, '默认配置', 1, 0, 10, 1, 'DEFAULT', '请根据以下上下文信息回答用户问题：\n{context}\n\n用户问题：{question}', 1,
        5, 0.7, 1, 1, 0, '基于阿里云DashScope的默认聊天配置');
INSERT INTO `chat_option` (`id`, `name`, `rag`, `tools`, `max_messages`, `enabled`, `transform`,
                           `content_injector_prompt`, `content_aggregator`, `max_results`, `min_score`,
                           `enable_text_search`, `enable_vector_search`, `in_DB`, `remark`)
VALUES (2, '通用配置', 1, 0, 10, 1, 'DEFAULT', '请根据以下上下文信息回答用户问题：\n{context}\n\n用户问题：{question}', 1,
        5, 0.7, 1, 1, 1, '基于阿里云DashScope的默认聊天配置');
COMMIT;

-- ----------------------------
-- Table structure for chat_option_api_key_rel
-- ----------------------------
DROP TABLE IF EXISTS `chat_option_api_key_rel`;
CREATE TABLE `chat_option_api_key_rel`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `chat_option_id`  bigint(20) NOT NULL COMMENT '聊天配置ID',
    `chat_api_key_id` bigint(20) NOT NULL COMMENT 'API Key配置ID',
    `created_at`      timestamp  NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      timestamp  NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_option_key` (`chat_option_id`, `chat_api_key_id`),
    KEY `idx_chat_option_id` (`chat_option_id`),
    KEY `idx_chat_api_key_id` (`chat_api_key_id`),
    CONSTRAINT `fk_rel_api_key` FOREIGN KEY (`chat_api_key_id`) REFERENCES `chat_api_key` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rel_option` FOREIGN KEY (`chat_option_id`) REFERENCES `chat_option` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='聊天配置与API Key关联表';

-- ----------------------------
-- Records of chat_option_api_key_rel
-- ----------------------------
BEGIN;
INSERT INTO `chat_option_api_key_rel` (`id`, `chat_option_id`, `chat_api_key_id`)
VALUES (1, 1, 1);
INSERT INTO `chat_option_api_key_rel` (`id`, `chat_option_id`, `chat_api_key_id`)
VALUES (2, 1, 2);
INSERT INTO `chat_option_api_key_rel` (`id`, `chat_option_id`, `chat_api_key_id`)
VALUES (3, 1, 3);
INSERT INTO `chat_option_api_key_rel` (`id`, `chat_option_id`, `chat_api_key_id`)
VALUES (4, 1, 5);
COMMIT;

-- ----------------------------
-- Table structure for chat_tool_description
-- ----------------------------
DROP TABLE IF EXISTS `chat_tool_description`;
CREATE TABLE `chat_tool_description`
(
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(255) NOT NULL COMMENT '工具名称',
    `description` text COMMENT '工具描述',
    `property`    text COMMENT '属性列表(JSON格式存储)',
    `required`    text COMMENT '必需字段列表(JSON格式存储)',
    `execute`     text COMMENT '执行脚本或命令',
    `created_at`  timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='工具描述信息表';

-- ----------------------------
-- Records of chat_tool_description
-- ----------------------------
BEGIN;
INSERT INTO `chat_tool_description` (`id`, `name`, `description`, `property`, `required`, `execute`)
VALUES (1, 'weatherQuery', '查询指定城市的天气信息,支持国内外主要城市', '{\"city\": \"城市\",\"unit\": \"温度\"}\n',
        '[\"city\"]',
        'function execute(params) {\n    var city = params.city;\n    var unit = params.unit || \"celsius\";\n    // 模拟天气数据(实际应用中应该调用真实的天气API)\n    var weatherData = {\n        \"北京\": { temp: 15, weather: \"晴\", humidity: 45 },\n        \"上海\": { temp: 20, weather: \"多云\", humidity: 60 },\n        \"深圳\": { temp: 25, weather: \"雨\", humidity: 80 },\n        \"New York\": { temp: 18, weather: \"Sunny\", humidity: 50 },\n        \"London\": { temp: 12, weather: \"Cloudy\", humidity: 70 }\n    };\n    var data = weatherData[city];\n    if (!data) {\n        return \"抱歉,暂时无法查询 \" + city + \" 的天气信息\";\n    }\n    var temperature = unit === \"fahrenheit\" ?\n        (data.temp * 9/5 + 32).toFixed(1) + \"°F\" :\n        data.temp + \"°C\";\n    return \"📍 \" + city + \" 的天气信息:\\n\" +\n           \"🌡️ 温度: \" + temperature + \"\\n\" +\n           \"☁️ 天气: \" + data.weather + \"\\n\" +\n           \"💧 湿度: \" + data.humidity + \"%\";\n}');
INSERT INTO `chat_tool_description` (`id`, `name`, `description`, `property`, `required`, `execute`)
VALUES (2, 'queryDatabase', '通过属性查询数据库用户', '{\"queryType\": \"字段名\",\"queryValue\": \"字段值\"}\n',
        '[\"queryType\",\"queryValue\"]',
        ' function execute(params) {\n    var queryType = params.queryType;\n    var queryValue = params.queryValue;\n    // 模拟数据库数据\n    var users = [\n        { id: \"1001\", username: \"zhangsan\", email: \"zhangsan@example.com\", role: \"Admin\" },\n        { id: \"1002\", username: \"lisi\", email: \"lisi@example.com\", role: \"User\" },\n        { id: \"1003\", username: \"wangwu\", email: \"wangwu@example.com\", role: \"User\" },\n        { id: \"1004\", username: \"zhaoliu\", email: \"zhaoliu@example.com\", role: \"Manager\" }\n    ];\n    // 执行查询\n    var result = null;\n    for (var i = 0; i < users.length; i++) {\n        var user = users[i];\n        if (user[queryType] === queryValue) {\n            result = user;\n            break;\n        }\n    }\n    if (!result) {\n        return \"❌ 未找到符合条件的用户: \" + queryType + \" = \" + queryValue;\n    }\n    return \"👤 用户信息:\\n\" +\n           \"🆔 ID: \" + result.id + \"\\n\" +\n           \"👤 用户名: \" + result.username + \"\\n\" +\n           \"📧 邮箱: \" + result.email + \"\\n\" +\n           \"🔑 角色: \" + result.role;\n}');
COMMIT;

-- ----------------------------
-- Table structure for members
-- ----------------------------
DROP TABLE IF EXISTS `chat_member`;
CREATE TABLE `chat_member`
(
    `id`         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`   varchar(50)  NOT NULL COMMENT '用户名',
    `password`   varchar(255) NOT NULL COMMENT '密码(MD5加密)',
    `nickname`   varchar(100)      DEFAULT NULL COMMENT '昵称',
    `email`      varchar(100)      DEFAULT NULL COMMENT '邮箱',
    `phone`      varchar(20)       DEFAULT NULL COMMENT '手机号',
    `avatar`     varchar(500)      DEFAULT NULL COMMENT '头像URL',
    `status`     int(11)           DEFAULT '1' COMMENT '状态: 1-正常, 0-禁用',
    `created_at` timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`    int(11)           DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='会员表';

-- ----------------------------
-- Records of members
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for api_keys
-- ----------------------------
DROP TABLE IF EXISTS `chat_project_api_key`;
CREATE TABLE `chat_project_api_key`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `api_key`      varchar(255) NOT NULL COMMENT 'API Key，以 sk- 开头',
    `member_id`    bigint(20)   NOT NULL COMMENT '所属用户ID',
    `name`         varchar(100)      DEFAULT NULL COMMENT 'API Key 名称/描述',
    `status`       int(11)           DEFAULT '1' COMMENT '状态: 1-启用, 0-禁用',
    `expires_at`   timestamp    NULL DEFAULT NULL COMMENT '过期时间（可选）',
    `last_used_at` timestamp    NULL DEFAULT NULL COMMENT '最后使用时间',
    `created_at`   timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      int(11)           DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='OpenAI API Key 表';

-- ----------------------------
-- Records of api_keys
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for biz_config
-- ----------------------------
DROP TABLE IF EXISTS `biz_config`;
CREATE TABLE `biz_config`
(
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `member_id`       bigint(20)   NOT NULL COMMENT '所属用户',
    `config_key`      varchar(191) NOT NULL COMMENT '配置键，如 amap.web_service_key',
    `config_value`    text         NOT NULL COMMENT '配置值，encrypt_storage=1 时为密文',
    `encrypt_storage` tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否加密存储：1 是 0 否',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `created_at`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_config_member_key` (`member_id`, `config_key`),
    KEY `idx_biz_config_member_id` (`member_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='业务配置（第三方 Key 等）';

-- ----------------------------
-- Direct model invoke audit (directInvoke / directInvokeRaw)
-- ----------------------------
DROP TABLE IF EXISTS `direct_model_invoke_log`;
CREATE TABLE `direct_model_invoke_log`
(
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `invoke_source`   varchar(512) NOT NULL COMMENT '调用来源',
    `template_name`   varchar(255)          DEFAULT NULL COMMENT '模板名（模板调用时有值）',
    `prompt_text`     mediumtext   NOT NULL COMMENT '完整提示词',
    `model_name`      varchar(255)          DEFAULT NULL COMMENT '解析到的模型名',
    `model_provider`  varchar(255)          DEFAULT NULL COMMENT '模型提供方',
    `response_text`   mediumtext            DEFAULT NULL COMMENT '模型返回文本',
    `success`         tinyint(1)   NOT NULL COMMENT '是否成功',
    `error_message`   text                  DEFAULT NULL COMMENT '失败信息',
    `duration_ms`     bigint(20)   NOT NULL COMMENT '耗时毫秒',
    `created_at`      timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_invoke_source` (`invoke_source`(191)),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_success` (`success`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='直接模型调用审计日志';

-- ----------------------------
-- Table structure for async_task
-- ----------------------------
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task` (
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `task_id`       varchar(64)  NOT NULL COMMENT '任务唯一标识(UUID)',
    `member_id`     bigint(20)   NOT NULL COMMENT '所属用户',
    `task_type`     varchar(50)  NOT NULL COMMENT '任务类型: GENERATE_SCRIPT, RAG_VECTORIZE...',
    `status`        varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
    `current_step`  int(11)               DEFAULT 0 COMMENT '当前步骤序号(从1开始)',
    `total_steps`   int(11)               DEFAULT 0 COMMENT '总步骤数',
    `steps`         json                  DEFAULT NULL COMMENT '步骤详情JSON数组',
    `result`        mediumtext            DEFAULT NULL COMMENT '最终结果JSON',
    `error_message` text                  DEFAULT NULL COMMENT '错误信息',
    `created_at`    timestamp    NULL     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    timestamp    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_task_type_status` (`task_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用异步任务进度表';

-- ----------------------------
-- Table structure for knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `member_id`    bigint(20)   NOT NULL COMMENT '所属用户ID',
    `name`         varchar(100) NOT NULL COMMENT '知识库名称',
    `description`  varchar(500)          DEFAULT NULL COMMENT '知识库描述',
    `visibility`   varchar(20)  NOT NULL DEFAULT 'private' COMMENT '可见范围: private/public',
    `index_name`   varchar(255) NOT NULL COMMENT 'ES 索引名，格式: {memberId}_kb_{id}',
    `build_task_id` varchar(64)          DEFAULT NULL COMMENT '知识库构建异步任务ID',
    `deleted`      int(11)               DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    `created_at`   timestamp    NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   timestamp    NULL     DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_index_name` (`index_name`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_member_deleted` (`member_id`, `deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='文档知识库';

-- ----------------------------
-- Table structure for document_info
-- ----------------------------
DROP TABLE IF EXISTS `document_info`;
CREATE TABLE `document_info`
(
    `id`                  bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `member_id`           bigint(20)   NOT NULL COMMENT '所属用户ID',
    `kb_id`               bigint(20)   NOT NULL COMMENT '所属知识库ID',
    `task_id`             varchar(64)           DEFAULT NULL COMMENT '关联 async_task.task_id',
    `original_name`       varchar(255) NOT NULL COMMENT '用户上传的原始文件名',
    `stored_name`         varchar(255) NOT NULL COMMENT '磁盘存储文件名（memberId_uuid_ext，防重名）',
    `index_name`          varchar(255) NOT NULL COMMENT 'ES 索引名（与 knowledge_base.index_name 一致，一库一索引）',
    `file_type`           varchar(20)  NOT NULL COMMENT '文件扩展名: .pdf/.docx/.pptx/.md/.txt',
    `doc_type`            varchar(50)  NOT NULL COMMENT '文档语义类型: short_text/paper/contract/novel',
    `file_size`           bigint(20)            DEFAULT NULL COMMENT '文件大小（字节）',
    `cleaning_config`     text                  DEFAULT NULL COMMENT '清洗规则配置 JSON',
    `status`              varchar(20)  NOT NULL DEFAULT 'PENDING'
                                      COMMENT '处理状态: PENDING/PARSING/CLEANING/CHUNKING/EMBEDDING/DONE/FAILED',
    `original_char_count` int(11)               DEFAULT NULL COMMENT '解析后原始字符数（PARSING 完成后写入）',
    `cleaned_char_count`  int(11)               DEFAULT NULL COMMENT '清洗后字符数（CLEANING 完成后写入）',
    `total_chunks`        int(11)               DEFAULT NULL COMMENT '分块总数（CHUNKING 完成后写入）',
    `processed_chunks`    int(11)               DEFAULT 0   COMMENT '已向量化分块数（EMBEDDING 阶段滚动更新）',
    `error_message`       text                  DEFAULT NULL COMMENT '最近一次失败的错误原因',
    `deleted`             int(11)               DEFAULT 0   COMMENT '逻辑删除: 0-未删除 1-已删除',
    `created_at`          timestamp    NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_at`          timestamp    NULL     DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_kb_original` (`kb_id`, `original_name`, `deleted`),
    KEY `idx_member_status` (`member_id`, `status`, `deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='文档基础信息表';

-- ----------------------------
-- Table structure for chat_message_rag_source
-- ----------------------------
DROP TABLE IF EXISTS `chat_message_rag_source`;
CREATE TABLE `chat_message_rag_source`
(
    `id`         bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `message_id` bigint(20)   DEFAULT NULL COMMENT '关联 chat_messages.id，由 updateMessages 阶段写入',
    `session_id` varchar(255) NOT NULL COMMENT '会话ID，用于 inject → updateMessages 两阶段关联',
    `kb_name`    varchar(255) DEFAULT NULL COMMENT '知识库名称',
    `index_name` varchar(255) DEFAULT NULL COMMENT 'ES索引名',
    `content`    text         NOT NULL COMMENT '命中的文本片段',
    `created_at` timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='RAG检索命中片段表';

SET FOREIGN_KEY_CHECKS = 1;
