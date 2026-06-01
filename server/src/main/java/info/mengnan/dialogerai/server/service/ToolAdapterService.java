package info.mengnan.dialogerai.server.service;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.dialogerai.common.util.JSONUtil;
import info.mengnan.dialogerai.repository.entity.ChatToolDescription;
import info.mengnan.dialogerai.repository.repo.ToolDescriptionRepository;
import info.mengnan.dialogerai.tool.ToolDescription;
import info.mengnan.dialogerai.tool.Tools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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

    private final ToolDescriptionRepository toolDescriptionService;
    private final BizConfigService bizConfigService;

    private static final String NODE_MODULES_PATH = System.getProperty("user.dir") + "/tool";


    public Map<ToolSpecification, ToolExecutor> dynamicTools(Long memberId) {
        log.info("Creating and initializing dynamic tools...");

        Tools tools = new Tools(
                key -> bizConfigService.getPlainValue(memberId, key)
        );

        // 从数据库查询所有工具描述
        List<ChatToolDescription> toolEntities = toolDescriptionService.findByMemberId(memberId);
        List<ToolDescription> toolDescriptions = toolEntities.stream()
                .map(this::convertToDescription)
                .collect(Collectors.toList());

        // 创建动态工具
        Map<ToolSpecification, ToolExecutor> dynamicTools = tools.createDynamicTools(toolDescriptions);

        log.info("Dynamic tools initialized successfully with {} tools", dynamicTools.size());
        return dynamicTools;
    }

    /**
     * 将数据库实体转换为工具描述
     */
    private ToolDescription convertToDescription(ChatToolDescription entity) {
        ToolDescription description = new ToolDescription();
        description.setName(entity.getName());
        description.setDescription(entity.getDescription());
        if(!entity.getProperty().isEmpty()) {
            description.setProperty(JSONUtil.parseObj(entity.getProperty()).entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.valueOf(e.getValue())
                    )));
        } else {
            description.setProperty(new HashMap<>());
        }
        if(!entity.getRequired().isEmpty()) {
            description.setRequired(JSONUtil.toList(entity.getRequired(), String.class));
        } else {
            description.setRequired(new ArrayList<>());
        }

        description.setExecute(entity.getExecute());
        return description;
    }

    /**
     * 执行单个工具的脚本（用于测试）
     * @param tool 工具实体
     * @param parameters 测试参数 (JSON 字符串)
     * @return 执行结果
     */
    public String executeTool(ChatToolDescription tool, String parameters) {
        Tools tools = new Tools(
                key -> bizConfigService.getPlainValue(tool.getMemberId(), key)
        );

        ToolDescription toolDescription = convertToDescription(tool);
        Map<ToolSpecification, ToolExecutor> toolMap = tools.createDynamicTools(List.of(toolDescription));

        if (toolMap.isEmpty()) {
            throw new RuntimeException("工具执行器创建失败");
        }

        ToolExecutor executor = toolMap.values().iterator().next();
        dev.langchain4j.agent.tool.ToolExecutionRequest executionRequest = dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                .arguments(parameters != null ? parameters : "{}")
                .build();

        return executor.execute(executionRequest, null);
    }
}
