package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.repository.entity.ChatOption;
import info.mengnan.aitalk.repository.service.ChatOptionService;
import info.mengnan.aitalk.server.param.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/option")
@RequiredArgsConstructor
public class OptionController {

    private final ChatOptionService chatOptionService;

    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<ChatOption> options = chatOptionService.findByMemberId(memberId);
        return R.ok(options);
    }

    @PostMapping("/create")
    public R create(@RequestBody ChatOption option) {
        Long memberId = StpUtil.getLoginIdAsLong();
        option.setId(null);
        option.setMemberId(memberId);
        chatOptionService.insert(option);
        return R.ok(option);
    }

    @PutMapping("/{id}")
    public R update(@PathVariable("id") Long id, @RequestBody ChatOption option) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatOption existing = chatOptionService.findById(id);
        if (existing == null)
            return R.error("配置不存在");
        if (!memberId.equals(existing.getMemberId()))
            return R.unauthorized();

        option.setId(id);
        option.setMemberId(memberId);
        chatOptionService.update(option);
        return R.ok(option);
    }

    @DeleteMapping("/{id}")
    public R delete(@PathVariable("id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatOption existing = chatOptionService.findById(id);
        if (existing == null)
            return R.error("配置不存在");
        if (!memberId.equals(existing.getMemberId()))
            return R.unauthorized();

        chatOptionService.deleteById(id);
        return R.ok();
    }
}
