package info.mengnan.aitalk.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.aitalk.server.content.DocumentEmbedding;
import info.mengnan.aitalk.server.param.DocumentUploadResult;
import info.mengnan.aitalk.server.param.DocumentUploadResponse;
import info.mengnan.aitalk.server.param.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

        Long memberId = StpUtil.getLoginIdAsLong();
        try {
            DocumentUploadResult result = documentService.uploadAndProcessDocument(memberId, file, type);

            DocumentUploadResponse response;
            if ("success".equals(result.getStatus())) {
                response = DocumentUploadResponse.success(
                        file.getOriginalFilename(),
                        result.getFilename(),
                        result.getIndexName()
                );
                return R.ok(result.getMessage(), response);
            } else if ("duplicate".equals(result.getStatus())) {
                response = DocumentUploadResponse.duplicate(result.getIndexName());
                return R.ok(result.getMessage(), response);
            } else {
                response = DocumentUploadResponse.error(result.getMessage());
                return R.error(response.getMessage());
            }
        } catch (Exception e) {
            log.error("file Upload Failed", e);
            return R.error("文件上传失败:" + e.getMessage());
        }
    }
}

