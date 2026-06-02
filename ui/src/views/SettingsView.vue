<template>
  <div class="body-content">
        <!-- 文档知识库 -->
        <template v-if="currentSection === 'documents'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">文档知识库</h1>
              <p class="page-desc">上传文档并构建知识库，支持 PDF、Word、TXT、Markdown 格式，供对话检索引用。</p>
            </div>
            <div class="section-header-actions">
              <button
                type="button"
                class="primary-action-btn"
                @click="router.push({ name: 'kbCreate' })"
              >
                新建知识库
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingKbList"
                  @click="loadKnowledgeBaseList"
                >
                  {{ loadingKbList ? '刷新中…' : '刷新' }}
                </button>
              </div>
              <div class="data-table-panel" :class="{ 'is-loading': loadingKbList }">
                <div v-if="loadingKbList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                  <div class="data-table-loading-backdrop" />
                  <div class="data-table-loading-content">
                    <span class="data-table-spinner" />
                    <span class="data-table-loading-text">加载知识库列表…</span>
                  </div>
                </div>
                <div v-else-if="knowledgeBaseList.length > 0" class="table-scroll-x">
                  <div class="upload-list kb-list">
                    <div class="list-header">
                      <span>知识库名称</span>
                      <span>描述</span>
                      <span>可见范围</span>
                      <span>文档数</span>
                      <span>创建时间</span>
                      <span>操作</span>
                    </div>
                    <div
                      v-for="kb in knowledgeBaseList"
                      :key="kb.id"
                      class="upload-item kb-item"
                    >
                      <button
                        type="button"
                        class="upload-name kb-name-link"
                        :title="kb.name"
                        @click="goToKbDetail(kb)"
                      >
                        {{ kb.name }}
                      </button>
                      <span class="kb-desc" :title="kb.description || ''">{{ kb.description || '-' }}</span>
                      <span>{{ kb.visibility === 'public' ? '公开' : '私有' }}</span>
                      <span>{{ kb.documentCount }}</span>
                      <span class="doc-created-at">{{ formatDate(kb.createdAt) }}</span>
                      <div class="upload-actions">
                        <button
                          type="button"
                          class="edit-btn"
                          title="管理知识库"
                          @click="goToKbDetail(kb)"
                        >
                          管理知识库
                        </button>
                        <button
                          type="button"
                          class="delete-btn"
                          title="删除知识库"
                          @click="confirmDeleteKnowledgeBase(kb)"
                        >
                          删除
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-else class="table-empty">暂无知识库，点击「新建知识库」开始</p>
              </div>
            </div>
          </div>
        </template>

        <!-- API Key 管理 -->
        <template v-else-if="currentSection === 'apikey'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">API Key 管理</h1>
              <p class="page-desc">创建和管理用于调用 OpenAI 兼容接口的 API Key。创建后请妥善保存，完整 Key 仅显示一次。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="showCreateKeyModal = true">
                + 创建 API Key
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingApiKeyList"
                  @click="loadApiKeyList"
                >
                  {{ loadingApiKeyList ? '刷新中…' : '刷新' }}
                </button>
              </div>
            <div class="data-table-panel" :class="{ 'is-loading': loadingApiKeyList }">
              <div v-if="loadingApiKeyList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                <div class="data-table-loading-backdrop" />
                <div class="data-table-loading-content">
                  <span class="data-table-spinner" />
                  <span class="data-table-loading-text">加载 API Key…</span>
                </div>
              </div>
              <div v-else-if="apiKeyList.length > 0" class="table-scroll-x">
                <div class="account-list apikey-list">
                <div class="list-header">
                  <span>名称</span>
                  <span>Key</span>
                  <span>创建时间</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="key in apiKeyList"
                  :key="key.id"
                  class="account-item apikey-item"
                >
                  <div class="account-info apikey-info">
                    <span class="account-username">{{ key.name || '未命名' }}</span>
                    <span class="apikey-masked">{{ key.apiKey }}</span>
                    <span class="apikey-meta">
                      {{ formatDate(key.createdAt) }}
                      <template v-if="key.expiresAt"> · 过期 {{ formatDate(key.expiresAt) }}</template>
                    </span>
                  </div>
                  <div class="upload-actions">
                    <button
                      type="button"
                      class="delete-btn"
                      @click="confirmDeleteKey(key)"
                    >
                      删除
                    </button>
                  </div>
                </div>
                </div>
              </div>
              <p v-else class="table-empty">暂无 API Key，点击上方按钮创建</p>
            </div>
          </div>
        </div>

          <!-- 创建 API Key 弹窗 -->
          <div v-if="showCreateKeyModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">创建 API Key</h2>
                <button type="button" class="modal-close" @click="closeCreateKeyModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">名称（可选）</label>
                  <input
                    v-model="createKeyForm.name"
                    type="text"
                    class="form-input"
                    placeholder="例如：生产环境"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">有效天数（可选，不填则永不过期）</label>
                  <input
                    v-model.number="createKeyForm.expiresInDays"
                    type="number"
                    class="form-input"
                    placeholder="例如：30"
                    min="1"
                  />
                </div>
                <p v-if="createKeyError" class="form-error">{{ createKeyError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeCreateKeyModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="creatingKey" @click="submitCreateKey">
                  {{ creatingKey ? '创建中...' : '创建' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 新建 Key 展示弹窗（仅创建成功后显示一次） -->
          <div v-if="newKeyResult" class="modal-overlay">
            <div class="modal-content modal-key-result">
              <div class="modal-header">
                <h2 class="modal-title">API Key 已创建，请妥善保存</h2>
                <button type="button" class="modal-close" @click="closeNewKeyModal">×</button>
              </div>
              <div class="modal-body">
                <p class="form-label">完整 Key 仅显示一次，请复制保存：</p>
                <div class="key-display">
                  <code class="key-value">{{ newKeyResult.apiKey }}</code>
                  <button type="button" class="copy-key-btn" @click="copyNewKey">复制</button>
                </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-confirm" @click="closeNewKeyModal">已保存，关闭</button>
              </div>
            </div>
          </div>
        </template>

        <!-- 外部服务配置（高德 Key 等） -->
        <template v-else-if="currentSection === 'bizConfig'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">外部服务配置</h1>
              <p class="page-desc">每条记录对应一个「业务配置键」；其下可添加多组子键/子值，保存时合并为 JSON 字符串写入数据库。可选加密存储；列表不展示配置内容，编辑时加载各子键名（子值需重新填写）。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="openBizConfigModal(null)">
                + 添加配置
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingBizConfigList"
                  @click="loadBizConfigList"
                >
                  {{ loadingBizConfigList ? '刷新中…' : '刷新' }}
                </button>
              </div>
              <div class="data-table-panel" :class="{ 'is-loading': loadingBizConfigList }">
                <div v-if="loadingBizConfigList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                  <div class="data-table-loading-backdrop" />
                  <div class="data-table-loading-content">
                    <span class="data-table-spinner" />
                    <span class="data-table-loading-text">加载外部服务配置…</span>
                  </div>
                </div>
                <div v-else-if="bizConfigList.length > 0" class="table-scroll-x">
                  <div class="account-list biz-config-list">
                  <div class="list-header">
                    <span>配置键</span>
                    <span>加密存储</span>
                    <span>备注</span>
                    <span>更新时间</span>
                    <span>操作</span>
                  </div>
                  <div
                    v-for="row in bizConfigList"
                    :key="row.id"
                    class="account-item biz-config-item"
                  >
                    <div class="account-info biz-config-info">
                      <span class="biz-config-key" :title="row.configKey">{{ row.configKey }}</span>
                      <span class="biz-config-flag">{{ row.encryptStorage ? '是' : '否' }}</span>
                      <span class="biz-config-remark">{{ row.remark || '—' }}</span>
                      <span class="apikey-meta">{{ formatDate(row.updatedAt) }}</span>
                    </div>
                    <div class="upload-actions">
                      <button type="button" class="edit-btn" @click="openBizConfigModal(row)">编辑</button>
                      <button
                        type="button"
                        class="delete-btn"
                        @click="confirmDeleteBizConfig(row)"
                      >
                        删除
                      </button>
                    </div>
                  </div>
                  </div>
                </div>
                <p v-else class="table-empty">暂无外部服务配置，点击上方「添加配置」</p>
              </div>
            </div>
          </div>

          <div v-if="showBizConfigModal" class="modal-overlay">
            <div class="modal-content modal-wide">
              <div class="modal-header">
                <h2 class="modal-title">{{ bizConfigEditing ? '编辑外部服务配置' : '添加外部服务配置' }}</h2>
                <button type="button" class="modal-close" @click="closeBizConfigModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">业务配置键 <span class="required">*</span></label>
                  <input
                    v-model="bizConfigForm.configKey"
                    type="text"
                    class="form-input"
                    :disabled="!!bizConfigEditing"
                    placeholder="例如：amap.credentials（库中一条记录的主键）"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">配置项（子键 / 子值）<span class="required">*</span></label>
                  <p class="form-hint">至少保留一行；空行会被忽略。保存时将多行合并为 JSON 对象写入「配置值」字段。</p>
                  <div class="property-list biz-config-pair-list">
                    <div
                      v-for="(pair, index) in bizConfigPairs"
                      :key="index"
                      class="property-row biz-config-pair-row"
                    >
                      <input
                        v-model="pair.key"
                        type="text"
                        class="form-input property-input"
                        placeholder="子键，如 webServiceKey"
                      />
                      <input
                        v-model="pair.value"
                        type="text"
                        class="form-input property-input"
                        placeholder="子值"
                      />
                      <button
                        type="button"
                        class="property-remove-btn"
                        :disabled="bizConfigPairs.length <= 1"
                        @click="removeBizConfigPair(index)"
                      >
                        删除
                      </button>
                    </div>
                    <button type="button" class="property-add-btn" @click="addBizConfigPair">+ 添加配置项</button>
                  </div>
                  <p v-if="bizConfigEditing && loadingBizConfigDetail" class="form-hint">
                    正在加载配置值…
                  </p>
                </div>
                <div class="form-group">
                  <label class="form-check">
                    <input v-model="bizConfigForm.encryptStorage" type="checkbox" />
                    <span>落库前加密存储</span>
                  </label>
                </div>
                <div class="form-group">
                  <label class="form-label">备注</label>
                  <input
                    v-model="bizConfigForm.remark"
                    type="text"
                    class="form-input"
                    placeholder="可选，如：高德 Web 服务"
                  />
                </div>
                <p v-if="bizConfigFormError" class="form-error">{{ bizConfigFormError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeBizConfigModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="submittingBizConfig" @click="submitBizConfig">
                  {{ submittingBizConfig ? '保存中…' : '保存' }}
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- 账号管理 -->
        <template v-else-if="currentSection === 'accounts'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">账号管理</h1>
              <p class="page-desc">管理系统用户，可以添加或删除账号。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="showAddAccountModal = true">
                + 添加账号
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingMemberList"
                  @click="loadMemberList"
                >
                  {{ loadingMemberList ? '刷新中…' : '刷新' }}
                </button>
              </div>
            <div class="data-table-panel" :class="{ 'is-loading': loadingMemberList }">
              <div v-if="loadingMemberList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                <div class="data-table-loading-backdrop" />
                <div class="data-table-loading-content">
                  <span class="data-table-spinner" />
                  <span class="data-table-loading-text">加载账号列表…</span>
                </div>
              </div>
              <div v-else-if="accountList.length > 0" class="table-scroll-x">
                <div class="account-list accounts-list">
                <div class="list-header">
                  <span>用户名</span>
                  <span>昵称</span>
                  <span>手机号</span>
                  <span>状态</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="account in accountList"
                  :key="account.id"
                  class="account-item"
                >
                  <div class="account-info">
                    <span class="account-username">{{ account.username }}</span>
                    <span class="account-nickname">{{ account.nickname || '-' }}</span>
                    <span class="account-phone">{{ account.phone || '-' }}</span>
                    <span class="account-status" :class="statusClass(account.status)">
                      {{ statusTextForAccount(account.status) }}
                    </span>
                  </div>
                  <button
                    v-if="account.id !== auth.user?.id"
                    type="button"
                    class="delete-btn"
                    @click="confirmDelete(account)"
                  >
                    删除
                  </button>
                  <span v-else class="current-user-tag">当前账号</span>
                </div>
                </div>
              </div>
              <p v-else class="table-empty">暂无账号</p>
            </div>
          </div>
        </div>

          <!-- 添加账号弹窗 -->
          <div v-if="showAddAccountModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">添加账号</h2>
                <button type="button" class="modal-close" @click="closeAddAccountModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">用户名 <span class="required">*</span></label>
                  <input
                    v-model="addAccountForm.username"
                    type="text"
                    class="form-input"
                    placeholder="请输入用户名"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">密码 <span class="required">*</span></label>
                  <input
                    v-model="addAccountForm.password"
                    type="password"
                    class="form-input"
                    placeholder="请输入密码"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">昵称</label>
                  <input
                    v-model="addAccountForm.nickname"
                    type="text"
                    class="form-input"
                    placeholder="请输入昵称（可选）"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">手机号</label>
                  <input
                    v-model="addAccountForm.phone"
                    type="tel"
                    class="form-input"
                    placeholder="请输入手机号（可选）"
                  />
                </div>
                <p v-if="addAccountError" class="form-error">{{ addAccountError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeAddAccountModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="addingAccount" @click="submitAddAccount">
                  {{ addingAccount ? '添加中...' : '确定' }}
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- 模型配置 -->
        <template v-else-if="currentSection === 'modelConfig'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">模型配置</h1>
              <p class="page-desc">配置第三方模型的 API Key，支持 OpenAI、Anthropic、DeepSeek 等模型提供商。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="showCreateModelModal = true">
                + 添加模型
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingModelKeyList"
                  @click="loadModelKeyList"
                >
                  {{ loadingModelKeyList ? '刷新中…' : '刷新' }}
                </button>
              </div>
            <div class="data-table-panel" :class="{ 'is-loading': loadingModelKeyList }">
              <div v-if="loadingModelKeyList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                <div class="data-table-loading-backdrop" />
                <div class="data-table-loading-content">
                  <span class="data-table-spinner" />
                  <span class="data-table-loading-text">加载模型配置…</span>
                </div>
              </div>
              <div v-else-if="modelKeyList.length > 0" class="table-scroll-x">
                <div class="account-list model-list">
                <div class="list-header">
                  <span>模型名称</span>
                  <span>提供商</span>
                  <span>类型</span>
                  <span>Key</span>
                  <span>创建时间</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="item in modelKeyList"
                  :key="item.id"
                  class="account-item model-item"
                >
                  <div class="account-info model-info">
                    <span class="model-name">{{ item.modelName }}</span>
                    <span class="model-provider-tag" :class="providerClass(item.modelProvider)">{{ item.modelProvider }}</span>
                    <span class="model-type-tag" :class="keyTypeClass(item.keyType)">{{ keyTypeLabel(item.keyType) }}</span>
                    <span class="apikey-masked">{{ item.maskedApiKey }}</span>
                    <span class="apikey-meta">{{ formatDate(item.createdAt) }}</span>
                  </div>
                  <div class="upload-actions">
                    <button
                      type="button"
                      class="delete-btn"
                      @click="confirmDeleteModelKey(item)"
                    >
                      删除
                    </button>
                  </div>
                </div>
                </div>
              </div>
              <p v-else class="table-empty">暂无模型配置，点击上方按钮添加</p>
            </div>
          </div>
        </div>

          <!-- 创建模型 API Key 弹窗 -->
          <div v-if="showCreateModelModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">添加模型</h2>
                <button type="button" class="modal-close" @click="closeCreateModelModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">模型名称 <span class="required">*</span></label>
                  <input
                    v-model="createModelForm.modelName"
                    type="text"
                    class="form-input"
                    placeholder="例如：gpt-4o、claude-sonnet-4-20250514"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">模型提供商 <span class="required">*</span></label>
                  <select v-model="createModelForm.modelProvider" class="form-input">
                    <option value="" disabled>请选择提供商</option>
                    <option v-for="p in providerOptions" :key="p" :value="p">{{ p }}</option>
                  </select>
                </div>
                <div v-if="createModelForm.modelProvider === '自定义'" class="form-group">
                  <label class="form-label">自定义提供商名称 <span class="required">*</span></label>
                  <input
                    v-model="createModelForm.customProvider"
                    type="text"
                    class="form-input"
                    placeholder="请输入提供商名称"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">类型</label>
                  <select v-model="createModelForm.keyType" class="form-input">
                    <option value="chat">对话（chat）</option>
                    <option value="streaming_chat">流式对话（streaming_chat）</option>
                    <option value="embedding">嵌入模型（embedding）</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">API Key <span class="required">*</span></label>
                  <input
                    v-model="createModelForm.apiKey"
                    type="password"
                    class="form-input"
                    placeholder="请输入 API Key"
                  />
                </div>
                <p v-if="createModelError" class="form-error">{{ createModelError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeCreateModelModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="creatingModelKey" @click="submitCreateModelKey">
                  {{ creatingModelKey ? '添加中...' : '确定' }}
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- Function Call 管理 -->
        <template v-else-if="currentSection === 'functionCall'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">Function Call</h1>
              <p class="page-desc">管理 AI 对话中可调用的工具/函数，定义工具的参数、描述和执行脚本。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="router.push({ name: 'functionCallCreate' })">
                + 添加工具
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingFunctionCallList"
                  @click="loadFunctionCallList"
                >
                  {{ loadingFunctionCallList ? '刷新中…' : '刷新' }}
                </button>
              </div>
              <div class="data-table-panel" :class="{ 'is-loading': loadingFunctionCallList }">
                <div v-if="loadingFunctionCallList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                  <div class="data-table-loading-backdrop" />
                  <div class="data-table-loading-content">
                    <span class="data-table-spinner" />
                    <span class="data-table-loading-text">加载工具列表…</span>
                  </div>
                </div>
                <div v-else-if="functionCallList.length > 0" class="table-scroll-x">
                  <div class="account-list fc-list">
                  <div class="list-header">
                    <span>工具名称</span>
                    <span>工具描述</span>
                    <span>创建时间</span>
                    <span>操作</span>
                  </div>
                  <div
                    v-for="item in functionCallList"
                    :key="item.id"
                    class="account-item fc-item"
                  >
                    <div class="account-info fc-info">
                      <span class="fc-name">{{ item.name }}</span>
                      <span class="fc-desc" :title="item.description">{{ item.description }}</span>
                      <span class="apikey-meta">{{ formatDate(item.createdAt) }}</span>
                    </div>
                    <div class="upload-actions">
                      <button
                        type="button"
                        class="edit-btn"
                        @click="router.push({ name: 'functionCallDetail', params: { id: item.id } })"
                      >
                        编辑
                      </button>
                      <button
                        type="button"
                        class="delete-btn"
                        @click="confirmDeleteFunctionCall(item)"
                      >
                        删除
                      </button>
                    </div>
                  </div>
                  </div>
                </div>
                <p v-else class="table-empty">暂无工具，点击上方按钮添加</p>
              </div>
            </div>
          </div>

        </template>

    <!-- 确认删除弹框 -->
    <div v-if="confirmDialog.visible" class="modal-overlay">
      <div class="modal-content modal-confirm">
        <div class="modal-header">
          <h2 class="modal-title">{{ confirmDialog.title }}</h2>
          <button type="button" class="modal-close" @click="cancelConfirm">×</button>
        </div>
        <div class="modal-body">
          <p class="confirm-message">{{ confirmDialog.message }}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-cancel" @click="cancelConfirm">取消</button>
          <button type="button" class="btn-confirm btn-danger" @click="executeConfirm">确定删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { KnowledgeBaseItem } from '@/api/knowledgeBase'
import { getKnowledgeBaseList, deleteKnowledgeBase } from '@/api/knowledgeBase'
import { getMemberList, createMember, deleteMember } from '@/api/auth'
import type { MemberInfo } from '@/api/auth'
import { getApiKeyList, createApiKey, disableApiKey, deleteApiKey } from '@/api/apikey'
import type { ApiKeyListItem, CreateApiKeyResult } from '@/api/apikey'
import { getModelKeyList, createModelKey, deleteModelKey } from '@/api/model'
import type { ModelApiKeyItem } from '@/api/model'
import { getFunctionCallList, deleteFunctionCall } from '@/api/functioncall'
import type { FunctionCallItem } from '@/api/functioncall'
import { getBizConfigList, getBizConfigItem, saveBizConfigItem, deleteBizConfigItem } from '@/api/config'
import type { BizConfigItem } from '@/api/config'
import { toastError, toastSuccess } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menuItems = [
  { id: 'documents', label: '文档知识库' },
  { id: 'functionCall', label: 'Function Call' },
  { id: 'apikey', label: 'API Key 管理' },
  { id: 'bizConfig', label: '外部服务配置' },
  { id: 'modelConfig', label: '模型配置' },
  { id: 'accounts', label: '账号管理' },
]

const currentSection = ref<string>('documents')

const knowledgeBaseList = ref<KnowledgeBaseItem[]>([])
const loadingKbList = ref(false)
const loadingApiKeyList = ref(false)
const loadingMemberList = ref(false)
const loadingModelKeyList = ref(false)
const loadingFunctionCallList = ref(false)
const loadingBizConfigList = ref(false)
const bizConfigList = ref<BizConfigItem[]>([])
const showBizConfigModal = ref(false)
const bizConfigEditing = ref<BizConfigItem | null>(null)
const submittingBizConfig = ref(false)
const loadingBizConfigDetail = ref(false)
const bizConfigFormError = ref('')
interface BizConfigPairRow {
  key: string
  value: string
}

const bizConfigForm = ref({
  configKey: '',
  encryptStorage: true,
  remark: '',
})

/** 弹窗内多行子键/子值，提交时 JSON.stringify 合并为 configValue */
const bizConfigPairs = ref<BizConfigPairRow[]>([{ key: '', value: '' }])

// 账号管理相关
const accountList = ref<MemberInfo[]>([])
const showAddAccountModal = ref(false)
const addingAccount = ref(false)
const addAccountError = ref('')
const addAccountForm = ref({
  username: '',
  password: '',
  nickname: '',
  phone: '',
})

// API Key 管理
const apiKeyList = ref<ApiKeyListItem[]>([])
const showCreateKeyModal = ref(false)
const creatingKey = ref(false)
const createKeyError = ref('')
const createKeyForm = ref({ name: '', expiresInDays: undefined as number | undefined })
const newKeyResult = ref<CreateApiKeyResult | null>(null)

// 模型配置相关
const modelKeyList = ref<ModelApiKeyItem[]>([])
const showCreateModelModal = ref(false)
const creatingModelKey = ref(false)
const createModelError = ref('')
const createModelForm = ref({
  modelName: '',
  modelProvider: '',
  customProvider: '',
  keyType: 'chat',
  apiKey: '',
})
const providerOptions = ['OpenAI', 'Anthropic', 'DeepSeek', 'Ollama', '自定义']

// Function Call 管理
const functionCallList = ref<FunctionCallItem[]>([])

// 确认弹框
const confirmDialog = ref({
  visible: false,
  title: '确认删除',
  message: '',
  onConfirm: null as (() => void) | null,
})

function showConfirm(message: string, onConfirm: () => void) {
  confirmDialog.value = { visible: true, title: '确认删除', message, onConfirm }
}

function cancelConfirm() {
  confirmDialog.value.visible = false
  confirmDialog.value.onConfirm = null
}

function executeConfirm() {
  confirmDialog.value.onConfirm?.()
  confirmDialog.value.visible = false
  confirmDialog.value.onConfirm = null
}

async function loadKnowledgeBaseList() {
  loadingKbList.value = true
  try {
    const res = await getKnowledgeBaseList()
    if (res.success && Array.isArray(res.data)) {
      knowledgeBaseList.value = res.data
    }
  } catch {
    // ignore
  } finally {
    loadingKbList.value = false
  }
}

/** 进入知识库管理页（默认基础信息） */
function goToKbDetail(kb: KnowledgeBaseItem) {
  router.push({ name: 'kbDetail', params: { kbId: kb.id } })
}

function confirmDeleteKnowledgeBase(kb: KnowledgeBaseItem) {
  showConfirm(
    `确定要删除知识库「${kb.name}」吗？其下所有文档与向量数据将一并删除。`,
    () => deleteKnowledgeBaseById(kb.id),
  )
}

async function deleteKnowledgeBaseById(id: number) {
  try {
    const res = await deleteKnowledgeBase(id)
    if (res.success) {
      knowledgeBaseList.value = knowledgeBaseList.value.filter((k) => k.id !== id)
    } else {
      toastError(res.message || '删除失败')
    }
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

function statusTextForAccount(status: number | undefined): string {
  if (status === 1) return '正常'
  if (status === 0) return '禁用'
  return status?.toString() ?? '未知'
}

function statusClass(status: number | undefined): string {
  if (status === 1) return 'status-normal'
  if (status === 0) return 'status-disabled'
  return ''
}


// 账号管理相关方法
function closeAddAccountModal() {
  showAddAccountModal.value = false
  addAccountForm.value = {
    username: '',
    password: '',
    nickname: '',
    phone: '',
  }
  addAccountError.value = ''
}

function confirmDelete(account: MemberInfo) {
  showConfirm(`确定要删除账号 "${account.username}" 吗？`, () => deleteMemberById(account.id))
}

async function deleteMemberById(id: number) {
  try {
    await deleteMember(id)
    accountList.value = accountList.value.filter((a) => a.id !== id)
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

async function submitAddAccount() {
  addAccountError.value = ''

  if (!addAccountForm.value.username || !addAccountForm.value.password) {
    addAccountError.value = '用户名和密码为必填项'
    return
  }

  addingAccount.value = true
  try {
    await createMember(addAccountForm.value)
    closeAddAccountModal()
    loadMemberList()
  } catch (e: any) {
    addAccountError.value = e.message || '添加失败'
  } finally {
    addingAccount.value = false
  }
}

async function loadMemberList() {
  loadingMemberList.value = true
  try {
    const res = await getMemberList()
    if (res.success && res.data) {
      accountList.value = res.data
    }
  } catch {
    // 忽略错误
  } finally {
    loadingMemberList.value = false
  }
}

// API Key 管理
function formatDate(s: string | null): string {
  if (!s) return '-'
  try {
    const d = new Date(s)
    return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch {
    return s
  }
}

async function loadApiKeyList() {
  loadingApiKeyList.value = true
  try {
    const res = await getApiKeyList()
    if (res.success && Array.isArray(res.data)) {
      apiKeyList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingApiKeyList.value = false
  }
}

function closeCreateKeyModal() {
  showCreateKeyModal.value = false
  createKeyForm.value = { name: '', expiresInDays: undefined }
  createKeyError.value = ''
}

function closeNewKeyModal() {
  newKeyResult.value = null
  loadApiKeyList()
}

async function copyNewKey() {
  if (!newKeyResult.value?.apiKey) return
  try {
    await navigator.clipboard.writeText(newKeyResult.value.apiKey)
    alert('已复制到剪贴板')
  } catch {
    alert('复制失败，请手动复制')
  }
}

async function submitCreateKey() {
  createKeyError.value = ''
  creatingKey.value = true
  try {
    const params: { name?: string; expiresInDays?: number } = {}
    if (createKeyForm.value.name.trim()) params.name = createKeyForm.value.name.trim()
    if (createKeyForm.value.expiresInDays != null && createKeyForm.value.expiresInDays > 0) {
      params.expiresInDays = createKeyForm.value.expiresInDays
    }
    const res = await createApiKey(params)
    if (res.success && res.data) {
      closeCreateKeyModal()
      newKeyResult.value = res.data
    } else {
      createKeyError.value = res.message || '创建失败'
    }
  } catch (e: any) {
    createKeyError.value = e.message || '创建失败'
  } finally {
    creatingKey.value = false
  }
}


function confirmDeleteKey(key: ApiKeyListItem) {
  showConfirm(`确定要删除 API Key「${key.name || key.apiKey}」吗？删除后不可恢复。`, () => deleteKeyById(key.id))
}

async function deleteKeyById(id: number) {
  try {
    await deleteApiKey(id)
    apiKeyList.value = apiKeyList.value.filter((k) => k.id !== id)
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

async function loadBizConfigList() {
  loadingBizConfigList.value = true
  try {
    const res = await getBizConfigList()
    if (res.success && Array.isArray(res.data)) {
      bizConfigList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingBizConfigList.value = false
  }
}

function addBizConfigPair() {
  bizConfigPairs.value.push({ key: '', value: '' })
}

function removeBizConfigPair(index: number) {
  if (bizConfigPairs.value.length <= 1) return
  bizConfigPairs.value.splice(index, 1)
}

function buildBizConfigJsonFromPairs(): { ok: true; json: string } | { ok: false; message: string } {
  const obj: Record<string, string> = {}
  for (let i = 0; i < bizConfigPairs.value.length; i++) {
    const row = bizConfigPairs.value[i]
    if (!row) continue
    const k = row.key.trim()
    const v = row.value
    if (!k && (v === '' || v === undefined)) continue
    if (!k) {
      return { ok: false, message: `第 ${i + 1} 行：已填写子值但缺少子键名` }
    }
    if (Object.prototype.hasOwnProperty.call(obj, k)) {
      return { ok: false, message: `子键重复：${k}` }
    }
    obj[k] = v
  }
  if (Object.keys(obj).length === 0) {
    return { ok: false, message: '请至少添加一组有效的子键与子值（可保留一行并填完整）' }
  }
  return { ok: true, json: JSON.stringify(obj) }
}

/** 使用列表接口返回的 displayValue（仅键名 JSON）预填子键，子值留空 */
function bizConfigPairsFromListDisplay(displayValue: string): BizConfigPairRow[] {
  const fallback: BizConfigPairRow[] = [{ key: '', value: '' }]
  if (!displayValue || displayValue === '****') {
    return fallback
  }
  try {
    const parsed: unknown = JSON.parse(displayValue)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return fallback
    }
    const keys = Object.keys(parsed as Record<string, unknown>)
    if (keys.length === 0) {
      return fallback
    }
    return keys.map((k) => ({ key: k, value: '' }))
  } catch {
    return fallback
  }
}

/** 使用详情接口返回的 configValue（明文 JSON）预填子键和子值 */
function bizConfigPairsFromPlainValue(configValue: string): BizConfigPairRow[] {
  const fallback: BizConfigPairRow[] = [{ key: '', value: '' }]
  if (!configValue) return fallback
  try {
    const parsed: unknown = JSON.parse(configValue)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return fallback
    }
    const obj = parsed as Record<string, string>
    const keys = Object.keys(obj)
    if (keys.length === 0) return fallback
    return keys.map((k) => ({ key: k, value: obj[k] ?? '' }))
  } catch {
    return fallback
  }
}

async function openBizConfigModal(row: BizConfigItem | null) {
  bizConfigEditing.value = row
  bizConfigFormError.value = ''
  if (row) {
    // 先用列表数据预填基本信息
    bizConfigPairs.value = bizConfigPairsFromListDisplay(row.displayValue)
    bizConfigForm.value = {
      configKey: row.configKey,
      encryptStorage: row.encryptStorage,
      remark: row.remark || '',
    }
    showBizConfigModal.value = true
    // 异步加载明文配置值
    loadingBizConfigDetail.value = true
    try {
      const res = await getBizConfigItem(row.configKey)
      if (res.success && res.data?.configValue) {
        bizConfigPairs.value = bizConfigPairsFromPlainValue(res.data.configValue)
      }
    } catch {
      // 加载失败时保持已有的键名预填
    } finally {
      loadingBizConfigDetail.value = false
    }
  } else {
    bizConfigPairs.value = [{ key: '', value: '' }]
    bizConfigForm.value = {
      configKey: '',
      encryptStorage: true,
      remark: '',
    }
    showBizConfigModal.value = true
  }
}

function closeBizConfigModal() {
  showBizConfigModal.value = false
  bizConfigEditing.value = null
  bizConfigFormError.value = ''
  bizConfigPairs.value = [{ key: '', value: '' }]
}

async function submitBizConfig() {
  bizConfigFormError.value = ''
  const key = bizConfigForm.value.configKey.trim()
  if (!key) {
    bizConfigFormError.value = '请输入业务配置键'
    return
  }
  const built = buildBizConfigJsonFromPairs()
  if (built.ok === false) {
    bizConfigFormError.value = built.message
    return
  }
  const configValueJson = built.json
  submittingBizConfig.value = true
  try {
    const res = await saveBizConfigItem(key, {
      configValue: configValueJson,
      encryptStorage: bizConfigForm.value.encryptStorage,
      remark: bizConfigForm.value.remark.trim() || undefined,
    })
    if (res.success) {
      toastSuccess('保存成功')
      closeBizConfigModal()
      await loadBizConfigList()
    } else {
      bizConfigFormError.value = res.message || '保存失败'
    }
  } catch (e: unknown) {
    bizConfigFormError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    submittingBizConfig.value = false
  }
}

function confirmDeleteBizConfig(row: BizConfigItem) {
  showConfirm(`确定删除外部服务配置「${row.configKey}」吗？删除后不可恢复。`, () => deleteBizConfigByKey(row.configKey))
}

async function deleteBizConfigByKey(configKey: string) {
  try {
    const res = await deleteBizConfigItem(configKey)
    if (res.success) {
      bizConfigList.value = bizConfigList.value.filter((r) => r.configKey !== configKey)
      toastSuccess('已删除')
    }
  } catch (e: unknown) {
    toastError(e instanceof Error ? e.message : '删除失败')
  }
}

// 模型配置相关方法
async function loadModelKeyList() {
  loadingModelKeyList.value = true
  try {
    const res = await getModelKeyList()
    if (res.success && Array.isArray(res.data)) {
      modelKeyList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingModelKeyList.value = false
  }
}

function closeCreateModelModal() {
  showCreateModelModal.value = false
  createModelForm.value = {
    modelName: '',
    modelProvider: '',
    customProvider: '',
    keyType: 'chat',
    apiKey: '',
  }
  createModelError.value = ''
}

async function submitCreateModelKey() {
  createModelError.value = ''

  const provider =
    createModelForm.value.modelProvider === '自定义'
      ? createModelForm.value.customProvider.trim()
      : createModelForm.value.modelProvider

  if (!createModelForm.value.modelName.trim()) {
    createModelError.value = '请输入模型名称'
    return
  }
  if (!provider) {
    createModelError.value = '请选择或输入模型提供商'
    return
  }
  if (!createModelForm.value.apiKey.trim()) {
    createModelError.value = '请输入 API Key'
    return
  }

  creatingModelKey.value = true
  try {
    const res = await createModelKey({
      modelName: createModelForm.value.modelName.trim(),
      modelProvider: provider,
      keyType: createModelForm.value.keyType,
      apiKey: createModelForm.value.apiKey.trim(),
    })
    if (res.success) {
      closeCreateModelModal()
      loadModelKeyList()
    } else {
      createModelError.value = res.message || '创建失败'
    }
  } catch (e: any) {
    createModelError.value = e.message || '创建失败'
  } finally {
    creatingModelKey.value = false
  }
}

function confirmDeleteModelKey(item: ModelApiKeyItem) {
  showConfirm(`确定要删除模型「${item.modelName}」的 API Key 吗？删除后不可恢复。`, () => deleteModelKeyById(item.id))
}

async function deleteModelKeyById(id: number) {
  try {
    await deleteModelKey(id)
    modelKeyList.value = modelKeyList.value.filter((k) => k.id !== id)
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

// 文档状态辅助
function docStatusLabel(status: string): string {
  switch (status) {
    case 'PENDING': return '待处理'
    case 'PARSING': return '解析中'
    case 'CLEANING': return '清洗中'
    case 'CHUNKING': return '分块中'
    case 'EMBEDDING': return '向量化中'
    case 'DONE': return '已完成'
    case 'FAILED': return '处理失败'
    default: return status
  }
}

function docStatusClass(status: string): string {
  switch (status) {
    case 'DONE': return 'doc-status-done'
    case 'FAILED': return 'doc-status-failed'
    case 'PENDING': return 'doc-status-pending'
    default: return 'doc-status-processing'
  }
}

function docTypeLabel(docType: string): string {
  switch (docType) {
    case 'default': return '通用文档'
    case 'short_text': return '短文本'
    case 'paper': return '论文'
    case 'contract': return '合同'
    case 'novel': return '小说'
    default: return docType
  }
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

// Function Call 相关方法
async function loadFunctionCallList() {
  loadingFunctionCallList.value = true
  try {
    const res = await getFunctionCallList()
    if (res.success && Array.isArray(res.data)) {
      functionCallList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingFunctionCallList.value = false
  }
}

function confirmDeleteFunctionCall(item: FunctionCallItem) {
  showConfirm(`确定要删除工具「${item.name}」吗？删除后不可恢复。`, () => deleteFunctionCallById(item.id))
}

async function deleteFunctionCallById(id: number) {
  try {
    await deleteFunctionCall(id)
    functionCallList.value = functionCallList.value.filter((k) => k.id !== id)
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

function keyTypeLabel(keyType: string): string {
  if (keyType === 'chat') return '对话'
  if (keyType === 'streaming_chat') return '流式对话'
  if (keyType === 'embedding') return '嵌入模型'
  if (keyType === 'moderation') return '审核'
  if (keyType === 'scoring') return '评分'
  return keyType
}

function providerClass(provider: string): string {
  const p = provider.toLowerCase()
  if (p === 'openai') return 'provider-openai'
  if (p === 'anthropic') return 'provider-anthropic'
  if (p === 'deepseek') return 'provider-deepseek'
  if (p === 'ollama') return 'provider-ollama'
  if (p === 'qwen' || p.includes('qwen')) return 'provider-qwen'
  if (p === 'cohere') return 'provider-cohere'
  return 'provider-default'
}

function keyTypeClass(keyType: string): string {
  if (keyType === 'chat') return 'type-chat'
  if (keyType === 'streaming_chat') return 'type-streaming'
  if (keyType === 'embedding') return 'type-embedding'
  if (keyType === 'moderation') return 'type-moderation'
  if (keyType === 'scoring') return 'type-scoring'
  return 'type-default'
}

function loadSectionData(section: string) {
  switch (section) {
    case 'documents': loadKnowledgeBaseList(); break
    case 'apikey': loadApiKeyList(); break
    case 'bizConfig': loadBizConfigList(); break
    case 'modelConfig': loadModelKeyList(); break
    case 'functionCall': loadFunctionCallList(); break
    case 'accounts': loadMemberList(); break
  }
}

watch(currentSection, (section) => {
  loadSectionData(section)
})

watch(
  () => route.query.section,
  (section) => {
    if (typeof section === 'string' && menuItems.some((m) => m.id === section)) {
      currentSection.value = section
    }
  },
)

onMounted(() => {
  const section = route.query.section as string | undefined
  if (section && menuItems.some((m) => m.id === section)) {
    currentSection.value = section
  }
  loadSectionData(currentSection.value)
})


</script>

<style scoped>
.body-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100%;
  min-height: 0;
}

/* 页面顶部 header 条：标题描述左，操作按钮右 */
.section-header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.5rem 2rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
  flex-shrink: 0;
}

.section-header-info {
  flex: 1;
  min-width: 0;
}

.section-header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
  flex-wrap: wrap;
}

/* 表格区主体 */
.section-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 1.5rem 2rem 2rem;
  min-height: 0;
}

/* 主操作按钮（上传/创建） */
.primary-action-btn {
  padding: 0.55rem 1.25rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-text-accent);
  border: 1px solid var(--color-text-accent);
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.2s ease;
  white-space: nowrap;
}

.primary-action-btn:hover:not(:disabled) {
  opacity: 0.88;
}

.primary-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.page-title {
  margin: 0 0 0.35rem;
  font-size: 1.375rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.page-desc {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

.user-info {
  margin: 0;
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
}

.user-info strong {
  color: var(--color-text-primary);
}

.type-select {
  padding: 0.45rem 0.75rem;
  font-size: 0.875rem;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  min-width: 140px;
  cursor: pointer;
}

.type-select:hover,
.type-select:focus {
  border-color: var(--color-border-focus);
  outline: none;
}

.file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.data-table-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.data-table-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 0.75rem;
}

.data-table-panel {
  position: relative;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--color-bg-card);
  flex: 1;
}

.data-table-panel.is-loading {
  min-height: 280px;
}

.data-table-panel > .upload-list,
.data-table-panel > .account-list,
.table-scroll-x > .upload-list,
.table-scroll-x > .account-list {
  margin-top: 0;
  border: none;
  border-radius: 0;
}

.table-scroll-x {
  overflow-x: auto;
  width: 100%;
}

.data-table-loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
}

.data-table-loading-backdrop {
  position: absolute;
  inset: 0;
  background: var(--color-bg-card);
  opacity: 0.88;
  backdrop-filter: blur(4px);
}

.data-table-loading-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
}

.data-table-spinner {
  width: 44px;
  height: 44px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-text-accent);
  border-radius: 50%;
  animation: data-table-spin 0.7s linear infinite;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--color-text-accent) 25%, transparent);
}

@keyframes data-table-spin {
  to {
    transform: rotate(360deg);
  }
}

.data-table-loading-text {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
}

.table-empty {
  margin: 0;
  padding: 4rem 1.5rem;
  text-align: center;
  font-size: 0.9375rem;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

.upload-list {
  margin-top: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-bg-card);
}

.upload-list .list-header {
  display: grid;
  grid-template-columns: 1fr 60px;
  align-items: center;
  padding: 0.75rem 1.25rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
  gap: 1rem;
}

/* 知识库列表 6 列布局 */
.kb-list .list-header {
  grid-template-columns: 1.2fr 1.5fr 80px 70px 150px 150px;
  padding: 0.75rem 1.25rem;
}

.kb-list .upload-item {
  grid-template-columns: 1.2fr 1.5fr 80px 70px 150px 150px;
  padding: 0.8rem 1.25rem;
}

.kb-list {
  min-width: 900px;
}

.kb-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-secondary);
}

/* 文档列表 9 列布局 */
.doc-list .list-header {
  grid-template-columns: 1.5fr 80px 100px 90px 70px 90px 150px 90px 130px;
  padding: 0.75rem 1.25rem;
}

.doc-list .upload-item {
  grid-template-columns: 1.5fr 80px 100px 90px 70px 90px 150px 90px 130px;
  padding: 0.8rem 1.25rem;
}

.doc-list {
  min-width: 1050px;
}

.doc-list .list-header span:last-child,
.doc-list .upload-item .upload-actions {
  text-align: center;
  justify-content: center;
}

/* 文档状态徽章 */
.doc-status-badge {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  width: fit-content;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
}

.doc-status-done {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.doc-status-failed {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-error, #ef4444);
}

.doc-status-pending {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

.doc-status-processing {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.upload-list .list-header span:last-child {
  text-align: center;
}

.upload-item {
  display: grid;
  grid-template-columns: 1fr 60px;
  align-items: center;
  gap: 1rem;
  padding: 0.8rem 1.25rem;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--color-border);
  transition: background-color 0.15s ease;
}

.upload-item:hover {
  background: var(--color-bg-input);
}

.upload-item:last-child {
  border-bottom: none;
}

.upload-item.success {
  background: transparent;
}

.upload-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
}

.kb-name-link {
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  font: inherit;
  cursor: pointer;
  color: var(--color-primary, #7c3aed);
}

.kb-name-link:hover {
  text-decoration: underline;
}

.doc-file-type {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  font-family: ui-monospace, monospace;
}

.doc-doc-type {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.doc-file-size {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.doc-progress {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.doc-progress.progress-done {
  color: #22c55e;
  font-weight: 500;
}

.doc-progress .progress-complete {
  color: #22c55e;
  font-weight: 500;
}

.doc-chunks {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.doc-chunks.chunks-complete {
  color: #22c55e;
}

.doc-created-at {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.upload-status {
  flex-shrink: 0;
  color: var(--color-text-secondary);
}

.upload-actions {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

/* 账号管理样式 */
.add-account-section {
  display: none;
}

.refresh-btn {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
  background: var(--color-bg-page);
}

.refresh-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.account-list {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-bg-card);
}

.list-header {
  padding: 0.75rem 1.25rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
  letter-spacing: 0.02em;
}

.account-item {
  padding: 0.8rem 1.25rem;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--color-border);
  transition: background-color 0.15s ease;
}

.account-item:hover {
  background: var(--color-bg-input);
}

.account-item:last-child {
  border-bottom: none;
}

.account-info {
  display: contents;
}

.account-username {
  font-weight: 500;
  color: var(--color-text-primary);
}

.account-nickname {
  color: var(--color-text-secondary);
}

.account-phone {
  color: var(--color-text-tertiary);
}

.account-status {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.account-status::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.account-status.status-normal {
  background: rgba(25, 135, 84, 0.1);
  color: #198754;
}

.account-status.status-normal::before {
  background: #198754;
}

.account-status.status-disabled {
  background: rgba(108, 117, 125, 0.1);
  color: #6c757d;
}

.account-status.status-disabled::before {
  background: #6c757d;
}

.delete-btn {
  padding: 0.25rem 0.6rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.delete-btn:hover {
  color: var(--color-error);
  background: rgba(220, 53, 69, 0.08);
}

.current-user-tag {
  padding: 0.2rem 0.6rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
  border-radius: 999px;
  flex-shrink: 0;
}

/* 外部服务配置 - grid 布局 */
.biz-config-list .list-header,
.biz-config-list .account-item {
  display: grid;
  grid-template-columns: minmax(100px, 1.4fr) 72px minmax(80px, 1.2fr) 130px 100px;
  align-items: center;
  gap: 0.75rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.biz-config-list {
  min-width: 540px;
}

.biz-config-list .list-header span:last-child {
  text-align: center;
}

.biz-config-info {
  display: contents;
}

.biz-config-key {
  font-family: ui-monospace, monospace;
  font-size: 0.8125rem;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.biz-config-flag {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.biz-config-remark {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.biz-config-pair-list {
  margin-top: 0.35rem;
}

.biz-config-pair-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.biz-config-pair-row .property-input {
  flex: 1;
  min-width: 0;
}

.biz-config-pair-row .property-remove-btn {
  flex-shrink: 0;
}

/* API Key 管理 - grid 布局 */
.apikey-list .list-header,
.apikey-list .account-item {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 60px;
  align-items: center;
  gap: 1rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.apikey-list {
  min-width: 480px;
}

.apikey-list .list-header span:last-child {
  text-align: center;
}

.apikey-masked {
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.apikey-meta {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.key-display {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.75rem;
  background: var(--color-bg-input);
  border-radius: 8px;
  border: 1px solid var(--color-border-hover);
}

.key-value {
  flex: 1;
  font-size: 0.8125rem;
  word-break: break-all;
  color: var(--color-text-primary);
}

.copy-key-btn {
  padding: 0.35rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  flex-shrink: 0;
}

.copy-key-btn:hover {
  border-color: var(--color-border-focus);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--color-bg-card);
  border-radius: 12px;
  width: 90%;
  max-width: 420px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.modal-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.modal-close {
  padding: 0.25rem 0.5rem;
  font-size: 1.5rem;
  line-height: 1;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
}

.modal-body {
  padding: 1.25rem;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.35rem;
}

.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.35rem;
}

.form-label-row .form-label {
  margin-bottom: 0;
}

.form-label-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-label .required {
  color: var(--color-error);
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  font-size: 0.9375rem;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-border-focus);
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-hint {
  margin: 0.35rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  line-height: 1.4;
}

.form-check {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  user-select: none;
}

.form-check input[type='checkbox'] {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
}

.form-error {
  margin: 0.5rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-error);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}

.btn-cancel,
.btn-confirm {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel {
  color: var(--color-text-primary);
  background: transparent;
  border: 1px solid var(--color-border-hover);
}

.btn-cancel:hover {
  border-color: var(--color-border-focus);
}

.btn-confirm {
  color: #fff;
  background: var(--color-text-accent);
  border: 1px solid var(--color-text-accent);
}

.btn-confirm:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 账号管理 - grid 布局 */
.accounts-list .list-header,
.accounts-list .account-item {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 80px 80px;
  align-items: center;
  gap: 1rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.accounts-list {
  min-width: 520px;
}

.accounts-list .list-header span:last-child {
  text-align: center;
}

.accounts-list .account-item .delete-btn,
.accounts-list .account-item .current-user-tag {
  justify-self: center;
}

/* 模型配置 - grid 布局 */
.model-list .list-header,
.model-list .account-item {
  display: grid;
  grid-template-columns: 1.5fr 80px 80px 1fr 140px 60px;
  align-items: center;
  gap: 1rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.model-list {
  min-width: 680px;
}

.model-list .list-header span:last-child {
  text-align: center;
}

.model-info {
  display: contents;
}

.model-name {
  font-weight: 500;
  color: var(--color-text-primary);
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
}

.model-provider-tag {
  justify-self: start;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  background: rgba(108, 117, 125, 0.1);
  color: #6c757d;
}

.provider-openai { background: rgba(16, 163, 127, 0.1); color: #10a37f; }
.provider-anthropic { background: rgba(204, 120, 50, 0.1); color: #cc7832; }
.provider-deepseek { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.provider-ollama { background: rgba(108, 117, 125, 0.1); color: #6c757d; }
.provider-qwen { background: rgba(124, 58, 237, 0.1); color: #7c3aed; }
.provider-cohere { background: rgba(239, 68, 68, 0.1); color: #ef4444; }

.model-type-tag {
  justify-self: start;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  background: rgba(108, 117, 125, 0.1);
  color: #6c757d;
}

.type-chat { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.type-streaming { background: rgba(6, 182, 212, 0.1); color: #0891b2; }
.type-embedding { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }
.type-moderation { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.type-scoring { background: rgba(217, 119, 6, 0.1); color: #d97706; }

/* 确认弹框 */
.modal-confirm {
  max-width: 360px;
}

.confirm-message {
  margin: 0;
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.btn-danger {
  background: var(--color-error, #dc3545);
  border-color: var(--color-error, #dc3545);
}

.btn-danger:hover:not(:disabled) {
  opacity: 0.9;
}

/* Function Call - grid 布局 */
.fc-list .list-header,
.fc-list .account-item {
  display: grid;
  grid-template-columns: 150px 1fr 140px 140px;
  align-items: center;
  gap: 1rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.fc-list {
  min-width: 540px;
}

.fc-list .list-header span:last-child {
  text-align: center;
}

.fc-info {
  display: contents;
}

.fc-name {
  font-weight: 500;
  color: var(--color-text-primary);
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
}

.fc-desc {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-btn {
  padding: 0.25rem 0.6rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.edit-btn:hover {
  color: var(--color-text-accent);
  background: rgba(59, 130, 246, 0.08);
}

.modal-wide {
  max-width: 600px;
}

.form-textarea {
  resize: vertical;
  min-height: 2.5rem;
  font-family: inherit;
  line-height: 1.5;
}
</style>