package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.dialogerai.repository.enums.DocumentStatus;
import info.mengnan.dialogerai.repository.entity.DocumentInfo;
import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import info.mengnan.dialogerai.repository.repo.DocumentInfoRepository;
import info.mengnan.dialogerai.repository.repo.KnowledgeBaseRepository;
import info.mengnan.dialogerai.server.exception.BusinessException;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.document.DocumentContentResponse;
import info.mengnan.dialogerai.server.param.document.DocumentInfoResponse;
import info.mengnan.dialogerai.server.service.DocumentProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;
    private final DocumentInfoRepository documentInfoRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentProcessService documentProcessService;

    /**
     * 在指定知识库中上传文档，立即返回 documentId 和 taskId
     */
    @PostMapping("/upload")
    public R uploadDocument(@RequestParam("file") MultipartFile file,
                            @RequestParam("kbId") Long kbId,
                            @RequestParam("type") String type,
                            @RequestParam(value = "cleaningConfig", required = false) String cleaningJson) {
        if (file.isEmpty())
            return R.error(ErrorCode.FILE_EMPTY);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank())
            return R.error(ErrorCode.FILE_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        try {
            return R.ok(documentProcessService.upload(file, kbId, type, cleaningJson, memberId));
        } catch (IOException e) {
            log.error("file upload failed: {}", originalFilename, e);
            return R.error(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 获取指定知识库下的文档列表。
     */
    @GetMapping("/list")
    public R listDocuments(@RequestParam("kbId") Long kbId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndMemberId(kbId, memberId);
        if (kb == null)
            return R.error(ErrorCode.KB_NOT_FOUND);

        List<DocumentInfoResponse> docs = documentInfoRepository.findByKbId(kbId)
                .stream()
                .map(DocumentInfoResponse::from)
                .collect(Collectors.toList());
        return R.ok(docs);
    }

    @GetMapping("/{documentId}")
    public R getDocument(@PathVariable("documentId") Long documentId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        DocumentInfo doc = documentInfoRepository.findByIdAndMemberId(documentId, memberId);
        if (doc == null)
            return R.error(ErrorCode.DOC_NOT_FOUND);

        return R.ok(DocumentInfoResponse.from(doc));
    }

    @GetMapping("/{documentId}/content")
    public R getDocumentContent(@PathVariable("documentId") Long documentId,
                                @RequestParam(value = "maxSegments", defaultValue = "500") Integer maxSegments) {
        Long memberId = StpUtil.getLoginIdAsLong();
        DocumentInfo doc = documentInfoRepository.findByIdAndMemberId(documentId, memberId);
        if (doc == null)
            return R.error(ErrorCode.DOC_NOT_FOUND);

        if (!DocumentStatus.DONE.name().equals(doc.getStatus()))
            return R.error(ErrorCode.DOC_NOT_READY);

        int limit = Math.min(Math.max(maxSegments, 1), 2000);
        List<String> segments = embeddingStoreRegistry.fetchDocumentSegments(
                doc.getIndexName(), documentId, limit);
        if (segments.isEmpty())
            return R.error(ErrorCode.DOC_CONTENT_EMPTY);

        String content = String.join("\n\n", segments);
        return R.ok(new DocumentContentResponse(
                doc.getId(),
                doc.getOriginalName(),
                doc.getStatus(),
                segments.size(),
                content,
                segments
        ));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public R deleteDocument(@PathVariable("documentId") Long documentId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        DocumentInfo doc = documentInfoRepository.findByIdAndMemberId(documentId, memberId);
        if (doc == null)
            return R.error(ErrorCode.DOC_NOT_FOUND);

        try {
            embeddingStoreRegistry.deleteDocumentSegments(doc.getIndexName(), documentId);
        } catch (Exception e) {
            log.warn("ES segment deletion failed: documentId={}, indexName={}",
                    documentId, doc.getIndexName(), e);
        }

        documentInfoRepository.deleteById(documentId);

        log.info("document deleted: documentId={}, kbId={}, indexName={}",
                documentId, doc.getKbId(), doc.getIndexName());
        return R.ok();
    }
}
