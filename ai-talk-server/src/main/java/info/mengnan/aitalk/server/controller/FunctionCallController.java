package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.repository.entity.ChatToolDescription;
import info.mengnan.aitalk.repository.service.ToolDescriptionService;
import info.mengnan.aitalk.server.param.FunctionCallRequest;
import info.mengnan.aitalk.server.param.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Function Call 工具管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/functioncall")
@RequiredArgsConstructor
public class FunctionCallController {

    private final ToolDescriptionService toolDescriptionService;

    /**
     * 获取当前用户的所有工具列表
     */
    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<ChatToolDescription> tools = toolDescriptionService.findAllByMemberId(memberId);
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

        toolDescriptionService.insert(entity);
        log.info("User {} created function call tool: {} (id={})", memberId, entity.getName(), entity.getId());

        return R.ok(entity);
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

        toolDescriptionService.update(tool);
        return R.ok(tool);
    }

    /**
     * 删除工具
     */
    @DeleteMapping("/{id}")
    public R delete(@PathVariable Long id) {
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
}
