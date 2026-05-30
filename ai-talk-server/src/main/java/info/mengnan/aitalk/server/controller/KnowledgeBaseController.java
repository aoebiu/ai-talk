package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.server.exception.BusinessException;
import info.mengnan.aitalk.server.param.ErrorCode;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.server.param.knowledgebase.KnowledgeBaseCreateRequest;
import info.mengnan.aitalk.server.param.knowledgebase.KnowledgeBaseUpdateRequest;
import info.mengnan.aitalk.server.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public R create(@RequestBody KnowledgeBaseCreateRequest request) {
        if (request.getName() == null || request.getName().isBlank())
            return R.error(ErrorCode.KB_NAME_EMPTY);

        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(knowledgeBaseService.create(request, memberId));
    }

    @PutMapping("/{kbId}")
    public R update(@PathVariable("kbId") Long kbId, @RequestBody KnowledgeBaseUpdateRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(knowledgeBaseService.update(kbId, memberId, request));

    }

    @PostMapping("/{kbId}/activate")
    public R activate(@PathVariable("kbId") Long kbId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(knowledgeBaseService.activateDraft(kbId, memberId));
    }

    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(knowledgeBaseService.list(memberId));
    }

    @GetMapping("/{kbId}")
    public R get(@PathVariable("kbId") Long kbId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(knowledgeBaseService.getKnowledgeBase(kbId, memberId));
    }

    @DeleteMapping("/{kbId}")
    public R delete(@PathVariable("kbId") Long kbId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.deleteKnowledgeBase(kbId, memberId);
        return R.ok();
    }
}
