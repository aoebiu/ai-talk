package info.mengnan.aitalk.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.rag.tools.ToolDescription;
import info.mengnan.aitalk.rag.tools.Tools;
import info.mengnan.aitalk.repository.entity.ChatToolDescription;
import info.mengnan.aitalk.repository.service.ToolDescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具配置类
 * 负责从数据库加载工具描述并创建动态工具
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAdapterService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Tools tools;
    private final ToolDescriptionService toolDescriptionService;


    public Map<ToolSpecification, ToolExecutor> dynamicTools() {
        log.info("Creating and initializing dynamic tools...");

        // 从数据库查询所有工具描述
        List<ChatToolDescription> toolEntities = toolDescriptionService.findAll();
        List<ToolDescription> toolDescriptions = toolEntities.stream()
                .map(this::convertToDescription)
                .collect(Collectors.toList());

        // 创建动态工具
        Map<ToolSpecification, ToolExecutor> dynamicTools = tools.createDynamicTools(toolDescriptions);

        log.info("Dynamic tools initialized successfully with {} tools", dynamicTools.size());
        return dynamicTools;
    }

    /**
     * 将数据库实体转换为DTO
     */
    private ToolDescription convertToDescription(ChatToolDescription entity) {
        ToolDescription dto = new ToolDescription();
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setProperty(parseJsonToMap(entity.getProperty()));
        dto.setRequired(parseJsonToList(entity.getRequired()));
        dto.setExecute(entity.getExecute());
        return dto;
    }

    /**
     * 解析JSON对象字符串为Map
     */
    private Map<String, String> parseJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (Exception e) {
            log.error("Failed to parse JSON to Map: {}", json, e);
            return Map.of();
        }
    }

    /**
     * 解析JSON数组字符串为List
     */
    private List<String> parseJsonToList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return List.of();
        }
        try {
            return mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("Failed to parse JSON to List: {}", json, e);
            return List.of();
        }
    }
}
