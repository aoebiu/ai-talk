package info.mengnan.aitalk.server.param;

import lombok.Data;

/**
 * 文档上传结果
 */
@Data
public class DocumentUploadResult {

    /**
     * 上传状态: success(成功), duplicate(重复), error(失败)
     */
    private String status;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * ES索引名
     */
    private String indexName;

    /**
     * 提示信息
     */
    private String message;

    private DocumentUploadResult(String status, String filename, String indexName, String message) {
        this.status = status;
        this.filename = filename;
        this.indexName = indexName;
        this.message = message;
    }

    /**
     * 上传成功
     */
    public static DocumentUploadResult success(String filename, String indexName) {
        return new DocumentUploadResult("success", filename, indexName, "文件上传并处理成功");
    }

    /**
     * 文件重复
     */
    public static DocumentUploadResult duplicate(String message, String indexName) {
        return new DocumentUploadResult("duplicate", null, indexName, message);
    }

    /**
     * 上传失败
     */
    public static DocumentUploadResult error(String message) {
        return new DocumentUploadResult("error", null, null, message);
    }
}
