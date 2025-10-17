package info.mengnan.aitalk.rag.tools;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.aitalk.common.json.JSONObject;
import info.mengnan.aitalk.common.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 需要graalvm
// 并且执行 sudo ${JAVA_HOME}/lib/installer/bin/gu install js
@Slf4j
public class Tools {

    private final Context context;

    public Tools() {
        // 创建 polyglot 上下文
        this.context = Context.newBuilder("js")
                .allowAllAccess(true)
                .build();
    }


    /**
     * 根据工具描述列表创建动态工具
     * @param toolDescriptions 工具描述列表
     * @return 工具规范和执行器的映射
     */
    public Map<ToolSpecification, ToolExecutor> createDynamicTools(List<ToolDescription> toolDescriptions) {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

        if (toolDescriptions == null || toolDescriptions.isEmpty()) {
            log.warn("No tool descriptions provided, returning empty tools map");
            return tools;
        }

        for (ToolDescription desc : toolDescriptions) {
            try {
                // 构建工具规范
                JsonObjectSchema.Builder schemaBuilder = getBuilder(desc);

                // 创建工具规范
                ToolSpecification toolSpec = ToolSpecification.builder()
                        .name(desc.getName())
                        .description(desc.getDescription())
                        .parameters(schemaBuilder.build())
                        .build();

                // 创建工具执行器
                ToolExecutor executor = createExecutor(desc);

                tools.put(toolSpec, executor);
                log.info("Successfully created dynamic tool: {}", desc.getName());

            } catch (Exception e) {
                log.error("Failed to create tool from description: {}", desc.getName(), e);
            }
        }

        return tools;
    }

    private JsonObjectSchema.Builder getBuilder(ToolDescription desc) {
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();

        // 添加属性
        for (Map.Entry<String, String> e : desc.getProperty().entrySet()) {
            schemaBuilder.addStringProperty(e.getKey(), e.getValue());
        }

        // 添加必需字段
        if (desc.getRequired() != null && !desc.getRequired().isEmpty()) {
            for (String req : desc.getRequired()) {
                schemaBuilder.required(req);
            }
        }
        return schemaBuilder;
    }

    /**
     * 根据工具描述创建执行器
     */
    private ToolExecutor createExecutor(ToolDescription desc) {
        return (request, memoryId) -> {
            try {
                // 验证执行脚本是否存在
                if (desc.getExecute() == null || desc.getExecute().trim().isEmpty()) {
                    log.error("Tool {} has no execute script defined", desc.getName());
                    return "执行失败: 工具未定义执行脚本";
                }

                // 解析参数
                JSONObject jsonObject = JSONUtil.parseObj(request.arguments());
                log.info("Executing tool: {} with arguments: {}", desc.getName(), request.arguments());

                // 构建包装的 JavaScript 代码，将参数注入到执行环境中
                String executeScript = desc.getExecute().trim();
                String wrappedScript;
                // 检查脚本是否定义了 execute 函数
                if (executeScript.contains("function execute")) {
                    wrappedScript = String.format(
                            """
                                    (function() {
                                      const params = %s;
                                      %s
                                      return execute(params);
                                    })();""",
                            JSONUtil.toJsonStr(jsonObject),
                            executeScript
                    );
                } else {
                    // 否则直接执行脚本并期望返回值
                    wrappedScript = String.format(
                            """
                                    (function() {
                                      const params = %s;
                                      return (%s);
                                    })();""",
                            JSONUtil.toJsonStr(jsonObject),
                        executeScript
                    );
                }

                // 创建并执行脚本
                Source source = Source.newBuilder("js", wrappedScript, desc.getName()).build();
                Value result = context.eval(source);

                // 处理不同类型的返回值
                if (result.isNull()) {
                    return "null";
                } else if (result.isBoolean()) {
                    return String.valueOf(result.asBoolean());
                } else if (result.isNumber()) {
                    return String.valueOf(result.asDouble());
                } else if (result.isString()) {
                    return result.asString();
                } else if (result.hasArrayElements()) {
                    // 处理数组返回值
                    return JSONUtil.parseArray(result).toString();
                } else if (result.hasMembers()) {
                    // 处理对象返回值
                    return JSONUtil.parseObj(result).toString();
                } else {
                    return result.toString();
                }

            } catch (Exception e) {
                log.error("Failed to execute tool: {}", desc.getName(), e);
                return "执行失败: " + e.getMessage();
            }
        };
    }
}
