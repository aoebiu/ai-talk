package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.server.content.DocumentEmbedding;
import info.mengnan.aitalk.server.param.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    // TODO 手动分割没办法尽善尽美,所以应该加一个手动分割文本段的功能
    // TODO 向量化应该异步处理
    private final DocumentEmbedding documentService;

    /**
     * 上传文档并向量化
     */
    @PostMapping("/upload")
    public R uploadDocument(@RequestParam("file") MultipartFile file,
                            @RequestParam("type") String type) {
        if (file.isEmpty()) {
            return R.error("文件不能为空");
        }
        log.info("File upload request received: {}", file.getOriginalFilename());

        String savedFileName = null;
        Long memberId = StpUtil.getLoginIdAsLong();
        try {
            savedFileName = documentService.uploadAndProcessDocument(memberId, file, type);
        } catch (Exception e) {
            log.error("file Upload Failed", e);
            return R.error("文件上传失败:" + e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();

        response.put("fileName", savedFileName);
        response.put("originalFileName", file.getOriginalFilename());

        return R.ok("文件上传并处理成功", response);


    }
}