package info.mengnan.aitalk.rag.service;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import info.mengnan.aitalk.rag.constant.promptTemplate.PromptTemplateConstant;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提示词模板管理器
 * 用于管理直接模型调用的提示词模板
 */
@Slf4j
public class PromptTemplateManager {

    // 缓存已加载的模板
    private final Map<String, PromptTemplate> templateCache = new ConcurrentHashMap<>();

    // 默认模板映射
    private final Map<String, PromptTemplate> defaultTemplates = Map.of(
            "title_generation", PromptTemplateConstant.TITLE_GENERATION_PROMPT_TEMPLATE,
            "query_router", PromptTemplateConstant.QUERY_ROUTER_PROMPT_TEMPLATE,
            "compression", PromptTemplateConstant.COMPRESSION_PROMPT_TEMPLATE,
            "content_injector", PromptTemplateConstant.CONTENT_INJECTOR_PROMPT_TEMPLATE
    );

    /**
     * 获取指定名称的模板
     * @param name 模板名称
     * @return PromptTemplate
     */
    public PromptTemplate getTemplate(String name) {
        return templateCache.computeIfAbsent(name, this::loadTemplate);
    }

    /**
     * 使用模板创建提示词
     * @param name 模板名称
     * @param variables 模板变量
     * @return Prompt
     */
    public Prompt createPrompt(String name, Map<String, Object> variables) {
        PromptTemplate template = getTemplate(name);
        if (template == null) {
            log.warn("Template not found: {}", name);
            throw new IllegalArgumentException("Template not found: " + name);
        }
        return template.apply(variables);
    }

    /**
     * 动态注册新模板
     * @param name 模板名称
     * @param template 模板内容
     */
    public void registerTemplate(String name, String template) {
        try {
            PromptTemplate promptTemplate = PromptTemplate.from(template);
            templateCache.put(name, promptTemplate);
            log.info("Registered new template: {}", name);
        } catch (Exception e) {
            log.error("Failed to register template: {}", name, e);
            throw new IllegalArgumentException("Invalid template format: " + e.getMessage(), e);
        }
    }

    /**
     * 加载模板
     * @param name 模板名称
     * @return PromptTemplate or null if not found
     */
    private PromptTemplate loadTemplate(String name) {
        // First check default templates
        PromptTemplate defaultTemplate = defaultTemplates.get(name);
        if (defaultTemplate != null) {
            log.debug("Loaded default template: {}", name);
            return defaultTemplate;
        }

        // TODO: In a real implementation, this could also load from database or external source
        log.warn("Template not found: {}", name);
        return null;
    }

    /**
     * 清除模板缓存
     */
    public void clearCache() {
        templateCache.clear();
        log.info("Cleared template cache");
    }
}