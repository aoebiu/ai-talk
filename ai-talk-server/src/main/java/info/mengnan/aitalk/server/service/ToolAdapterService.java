package info.mengnan.aitalk.server.service;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.common.json.JSONObject;
import info.mengnan.aitalk.common.util.JSONUtil;
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
     * 将数据库实体转换
     */
    private ToolDescription convertToDescription(ChatToolDescription entity) {
        ToolDescription description = new ToolDescription();
        description.setName(entity.getName());
        description.setDescription(entity.getDescription());
        description.setProperty(JSONUtil.parseObj(entity.getProperty()).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.valueOf(e.getValue())
                )));
        description.setRequired(JSONUtil.toList(entity.getRequired(), String.class));
        description.setExecute(entity.getExecute());
        return description;
    }
}
