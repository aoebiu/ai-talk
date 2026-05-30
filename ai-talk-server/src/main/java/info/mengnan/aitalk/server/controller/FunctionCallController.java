package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.common.util.JSONUtil;
import info.mengnan.aitalk.rag.constant.promptTemplate.PromptTemplateConstant;
import info.mengnan.aitalk.rag.service.DirectModelInvoker;
import info.mengnan.aitalk.rag.service.PromptTemplateManager;
import info.mengnan.aitalk.repository.entity.ChatToolDescription;
import info.mengnan.aitalk.repository.repo.ToolDescriptionRepository;
import info.mengnan.aitalk.server.param.functionCall.FunctionCallRequest;
import info.mengnan.aitalk.server.param.functionCall.FunctionCallScriptGenerateRequest;
import info.mengnan.aitalk.server.param.functionCall.FunctionCallTestCaseGenerateRequest;
import info.mengnan.aitalk.server.param.functionCall.FunctionCallTestRequest;
import info.mengnan.aitalk.server.param.functionCall.ToolCapabilityAnalysisResult;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.repository.enums.AsyncTaskType;
import info.mengnan.aitalk.server.service.AsyncTaskService;
import info.mengnan.aitalk.server.service.ToolAdapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Function Call 工具管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/functioncall")
@RequiredArgsConstructor
public class FunctionCallController {

    private final ToolDescriptionRepository toolDescriptionService;
    private final DirectModelInvoker directModelInvoker;
    private final PromptTemplateManager promptTemplateManager;
    private final ToolAdapterService toolAdapterService;
    private final AsyncTaskService asyncTaskService;

    /**
     * 获取当前用户的所有工具列表
     */
    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<ChatToolDescription> tools = toolDescriptionService.findByMemberId(memberId);
        return R.ok(tools);
    }

    /**
     * 根据 ID 获取工具详情
     */
    @GetMapping("/{id}")
    public R getById(@PathVariable("id") Long id) {
        ChatToolDescription tool = toolDescriptionService.findById(id);
        if (tool == null)
            return R.error("工具不存在");

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberId.equals(tool.getMemberId()))
            return R.unauthorized();
        return R.ok(tool);
    }

    /**
     * 创建新工具
     */
    @PostMapping("/create")
    public R create(@Validated @RequestBody FunctionCallRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatToolDescription existing = toolDescriptionService.findByNameAndMemberId(request.getName(), memberId);
        if (existing != null) {
            return R.error("工具名称已存在");
        }

        ChatToolDescription entity = new ChatToolDescription();
        entity.setMemberId(memberId);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setProperty(request.getProperty());
        entity.setRequired(request.getRequired());
        entity.setExecute(request.getExecute());
        entity.setGeneratePrompt(request.getGeneratePrompt());

        toolDescriptionService.insert(entity);
        log.info("User {} created function call tool: {}, id={}", memberId, entity.getName(), entity.getId());

        return R.ok(entity);
    }

    /**
     * 根据用户提示词，异步两阶段生成工具描述、属性列表与执行脚本
     * 立即返回 taskId，前端轮询 /api/task/{taskId} 获取进度
     * Phase 1: 分析用户需求需要哪些运行时能力
     * Phase 2: 按需拼接能力说明，生成完整工具元数据
     */
    @PostMapping("/generate/script")
    public R generateScript(@Validated @RequestBody FunctionCallScriptGenerateRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();
        String taskId = asyncTaskService.createTask(memberId, AsyncTaskType.GENERATE_SCRIPT, List.of("能力分析", "生成工具元数据"));

        asyncTaskService.submitTask(taskId, () -> {
            String userPrompt = request.getPrompt();
            try {
                // Phase 1: 能力分析
                asyncTaskService.updateStepRunning(taskId, 1);
                Map<String, Object> phase1Variables = Map.of("prompt", userPrompt);
                String analysisResult = directModelInvoker.directInvoke(
                        "generateScript.capabilityAnalyzer",
                        "tool_capability_analysis", phase1Variables);
                ToolCapabilityAnalysisResult capabilities = JSONUtil.toBean(analysisResult, ToolCapabilityAnalysisResult.class);

                // Phase 2: 动态拼接提示词并生成工具元数据
                asyncTaskService.updateStepRunning(taskId, 2);
                String baseTemplate = promptTemplateManager.getTemplate("tool_metadata_generation").template();

                String result = directModelInvoker.directInvokeRaw(
                        "generateScript.toolMetadataGenerator", generatePrompt(baseTemplate, capabilities, request.getPrompt()));

                asyncTaskService.completeTask(taskId, result);
            } catch (Exception e) {
                log.error("generateScript async task failed, taskId={}", taskId, e);
                asyncTaskService.failTask(taskId, e.getMessage());
            }
        });

        return R.ok(Map.of("taskId", taskId));
    }


    private String generatePrompt(String baseTemplate, ToolCapabilityAnalysisResult capabilities,String prompt) {
        StringBuilder composedPrompt = new StringBuilder(baseTemplate);

        int rulesIndex = composedPrompt.indexOf("## Rules");
        if (rulesIndex == -1) {
            rulesIndex = composedPrompt.length();
        }
        StringBuilder capabilitySection = new StringBuilder();
        if (capabilities.isNeedsHttp()) {
            capabilitySection.append(PromptTemplateConstant.TOOL_HTTP_CAPABILITY_SNIPPET);
        }
        if (capabilities.isNeedsConfig()) {
            capabilitySection.append(PromptTemplateConstant.TOOL_CONFIG_CAPABILITY_SNIPPET);
        }
        if (capabilities.isNeedsJwt()) {
            capabilitySection.append(PromptTemplateConstant.TOOL_JWT_CAPABILITY_SNIPPET);
        }
        composedPrompt.insert(rulesIndex, capabilitySection);
        return composedPrompt.toString().replace("{{prompt}}", prompt);
    }

    /**
     * 更新工具
     */
    @PutMapping("/{id}")
    public R update(@PathVariable("id") Long id, @Validated @RequestBody FunctionCallRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatToolDescription tool = toolDescriptionService.findById(id);
        if (tool == null)
            return R.error("工具不存在");

        if (!memberId.equals(tool.getMemberId()))
            return R.unauthorized();

        tool.setName(request.getName());
        tool.setDescription(request.getDescription());
        tool.setProperty(request.getProperty());
        tool.setRequired(request.getRequired());
        tool.setExecute(request.getExecute());
        tool.setGeneratePrompt(request.getGeneratePrompt());

        toolDescriptionService.update(tool);
        return R.ok(tool);
    }

    /**
     * 删除工具
     */
    @DeleteMapping("/{id}")
    public R delete(@PathVariable("id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatToolDescription tool = toolDescriptionService.findById(id);
        if (tool == null) {
            return R.error("工具不存在");
        }
        if (!memberId.equals(tool.getMemberId())) {
            return R.unauthorized();
        }

        toolDescriptionService.deleteById(id);
        log.info("User {} deleted function call tool: {} id={}", memberId, tool.getName(), id);

        return R.ok();
    }

    /**
     * 测试工具执行
     */
    @PostMapping("/{id}/test")
    public R test(@PathVariable("id") Long id, @RequestBody FunctionCallTestRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatToolDescription tool = toolDescriptionService.findById(id);
        if (tool == null) {
            return R.error("工具不存在");
        }
        if (!memberId.equals(tool.getMemberId())) {
            return R.unauthorized();
        }

        try {
            String result = toolAdapterService.executeTool(tool, request.getParameters());
            return R.ok(Map.of("result", result != null ? result : "null"));
        } catch (Exception e) {
            log.error("测试工具执行失败：{}", tool.getName(), e);
            return R.error("执行失败：" + e.getMessage());
        }
    }

    /**
     * 根据工具信息，使用 AI 生成指定数量的批量测试用例
     */
    @PostMapping("/{id}/generate/testcases")
    public R generateTestCases(@PathVariable("id") Long id,
                               @RequestBody FunctionCallTestCaseGenerateRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatToolDescription tool = toolDescriptionService.findById(id);
        if (tool == null) return R.error("工具不存在");
        if (!memberId.equals(tool.getMemberId())) return R.unauthorized();

        Map<String, Object> testCaseVars = new HashMap<>();
        testCaseVars.put("count", request.getCount());
        testCaseVars.put("toolName", tool.getName());
        testCaseVars.put("toolDescription", tool.getDescription() != null ? tool.getDescription() : "");
        testCaseVars.put("executeScript", tool.getExecute() != null ? tool.getExecute() : "");
        testCaseVars.put("toolProperty", tool.getProperty() != null ? tool.getProperty() : "{}");
        testCaseVars.put("toolRequired", tool.getRequired() != null ? tool.getRequired() : "[]");

        try {
            String result = directModelInvoker.directInvoke(
                    "generateScript.testCases",
                    "tool_case_generation",
                    testCaseVars);
            return R.ok(Map.of("testCases", result));
        } catch (Exception e) {
            log.error("生成测试用例失败：{}", tool.getName(), e);
            return R.error("生成失败：" + e.getMessage());
        }
    }
}
