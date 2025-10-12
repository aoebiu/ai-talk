-- auto-generated definition
CREATE TABLE chat_messages
(
    id         bigint AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    session_id varchar(255)                        NOT NULL COMMENT '会话ID',
    role       varchar(50)                         NOT NULL COMMENT '角色: USER, ASSISTANT, SYSTEM',
    content    text                                NOT NULL COMMENT '消息内容',
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间'
)
    COMMENT '聊天消息表' CHARSET = utf8mb4;

CREATE TABLE chat_api_key
(
    id             bigint AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    key_type       varchar(50)                         NOT NULL COMMENT 'API Key类型: chat, streaming_chat, embedding',
    api_key        varchar(500)                        NOT NULL COMMENT 'API Key',
    model_name     varchar(255)                        NOT NULL COMMENT '模型名称',
    model_provider varchar(255)                        NULL,
    created_at     timestamp DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updated_at     timestamp DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
)
    COMMENT 'API Key配置表' CHARSET = utf8mb4;


CREATE TABLE chat_option
(
    id                      bigint AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    name                    varchar(255)                         NOT NULL COMMENT '配置名称',
    max_messages            int        DEFAULT 10                NULL COMMENT '最大消息窗口数',
    enabled                 tinyint(1) DEFAULT 1                 NULL COMMENT '是否启用',
    transform               varchar(100)                         NULL COMMENT 'Query Transformer 类型',
    content_aggregator      varchar(100)                         NULL COMMENT 'Content Aggregator 类型',
    content_injector_prompt text                                 NULL COMMENT 'Content Injector 提示词模板',
    max_results             int        DEFAULT 5                 NULL COMMENT '检索最大结果数',
    min_score               double     DEFAULT 0.7               NULL COMMENT '检索最小相似度分数',
    enable_text_search      tinyint(1) DEFAULT 1                 NULL COMMENT '是否启用文本检索',
    enable_vector_search    tinyint(1) DEFAULT 1                 NULL COMMENT '是否启用向量检索',
    remark                  text                                 NULL COMMENT '备注',
    created_at              timestamp  DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updated_at              timestamp  DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
)
    COMMENT '聊天配置表' CHARSET = utf8mb4;

CREATE TABLE chat_option_api_key_rel
(
    id              bigint AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    chat_option_id  bigint                              NOT NULL COMMENT '聊天配置ID',
    chat_api_key_id bigint                              NOT NULL COMMENT 'API Key配置ID',
    created_at      timestamp DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updated_at      timestamp DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
)
    COMMENT '聊天配置与API Key关联表' CHARSET = utf8mb4;




