package info.mengnan.aitalk.server.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    /**
     * 上传状态: success(成功), duplicate(重复), error(失败)
     */
    private String status;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 原始文件名（仅成功时返回）
     */
    private String originalFileName;

    /**
     * 处理后的文件名（仅成功时返回）
     */
    private String fileName;

    /**
     * ES索引名
     */
    private String indexName;

    /**
     * 构造成功响应
     */
    public static DocumentUploadResponse success(String originalFileName, String fileName, String indexName) {
        return DocumentUploadResponse.builder()
                .status("success")
                .message("文件上传并处理成功")
                .originalFileName(originalFileName)
                .fileName(fileName)
                .indexName(indexName)
                .build();
    }

    /**
     * 构造重复文件响应
     */
    public static DocumentUploadResponse duplicate(String indexName) {
        return DocumentUploadResponse.builder()
                .status("duplicate")
                .message("文件已存在，无需重复上传")
                .indexName(indexName)
                .build();
    }

    /**
     * 构造错误响应
     */
    public static DocumentUploadResponse error(String message) {
        return DocumentUploadResponse.builder()
                .status("error")
                .message(message)
                .build();
    }
}
