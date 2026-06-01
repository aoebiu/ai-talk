package info.mengnan.dialogerai.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.common.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.*;

// 需要 graalvm
// 并且执行 sudo ${JAVA_HOME}/lib/installer/bin/gu install js
@Slf4j
public class Tools {

    private final Engine sharedEngine;
    private final ContextPool contextPool;
    private final Map<String, Object> bindings;
    private final ConfigProvider configProvider;

    private static final int DEFAULT_POOL_SIZE = 4;
    private static final String BINDINGS_CONFIG = "META-INF/bindings_config";

    private static final String FUNCTION_EXECUTE_SCRIPT =
            """
                    (function() {
                      const params = %s;
                      %s
                      return execute(params);
                    })();
                    """;

    private static final String DEFAULT_EXECUTE_SCRIPT =
            """
                    (function() {
                      const params = %s;
                      return (%s);
                    })();
                    """;

    /**
     * @param configProvider 配置提供者（需外部传入，因为依赖数据库等外部资源）
     */
    public Tools(ConfigProvider configProvider) {
        this(configProvider, DEFAULT_POOL_SIZE);
    }

    /**
     * @param configProvider 配置提供者（需外部传入，因为依赖数据库等外部资源）
     * @param poolSize Context 对象池大小
     */
    public Tools(ConfigProvider configProvider, int poolSize) {
        this.configProvider = configProvider;
        this.sharedEngine = Engine.create();
        this.contextPool = new ContextPool(poolSize);

        // 从配置文件加载绑定声明，格式：bindingName:全限定类名
        this.bindings = loadBindings();

        for (int i = 0; i < poolSize; i++) {
            contextPool.add(createContext());
        }
    }

    /**
     * 从 META-INF/bindings_config 加载绑定，委托给 {@link BindingLoader}。
     */
    private Map<String, Object> loadBindings() {
        return BindingLoader.load(BINDINGS_CONFIG);
    }

    /**
     * 创建一个绑定好依赖的 Context
     */
    private Context createContext() {
        Context context = Context.newBuilder("js")
                .engine(sharedEngine)
                .allowHostAccess(true)
                .build();

        // 配置文件声明的绑定
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            context.getBindings("js").putMember(entry.getKey(), entry.getValue());
        }

        // ConfigProvider 需要外部注入，手动绑定
        if (configProvider != null) {
            context.getBindings("js").putMember("config", configProvider);
        }

        return context;
    }

    /**
     * 关闭所有资源
     */
    public void shutdown() {
        contextPool.shutdown();
        sharedEngine.close();
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
            Context context = null;
            try {
                // 验证执行脚本是否存在
                if (desc.getExecute() == null || desc.getExecute().trim().isEmpty()) {
                    log.error("Tool {} has no execute script defined", desc.getName());
                    return "execution failed: no execute script defined";
                }

                // 从池中借出 Context
                context = contextPool.borrow();

                // 解析参数
                JSONObject jsonObject = JSONUtil.parseObj(request.arguments());
                log.info("Executing tool: {} with arguments: {}", desc.getName(), request.arguments());

                // 构建包装的 JavaScript 代码，将参数注入到执行环境中
                String executeScript = desc.getExecute().trim();
                String wrappedScript;
                // 检查脚本是否定义了 execute 函数
                if (executeScript.contains("function execute")) {
                    wrappedScript = String.format(FUNCTION_EXECUTE_SCRIPT,
                            JSONUtil.toJsonStr(jsonObject),
                            executeScript
                    );
                } else {
                    // 否则直接执行脚本并期望返回值
                    wrappedScript = String.format(DEFAULT_EXECUTE_SCRIPT,
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
                    return JSONUtil.parseObj(result.toString()).toString();
                } else {
                    return result.toString();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for context: {}", desc.getName(), e);
                return "execution failed: interrupted while waiting for context";
            } catch (Exception e) {
                log.error("Failed to execute tool: {}", desc.getName(), e);
                return "execution failed: " + e.getMessage();
            } finally {
                if (context != null) {
                    contextPool.returnContext(context);
                }
            }
        };
    }
}
