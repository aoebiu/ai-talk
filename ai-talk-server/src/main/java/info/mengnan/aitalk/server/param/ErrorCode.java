package info.mengnan.aitalk.server.param;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 通用
    BUSINESS_ERROR("0001", "业务异常"),

    // 文件
    FILE_EMPTY("1001", "文件不能为空"),
    FILE_INVALID("1002", "文件名无效"),
    FILE_UPLOAD_FAILED("1003", "文件上传失败"),

    // 知识库
    KB_NOT_FOUND("2001", "知识库不存在"),
    KB_NAME_EMPTY("2002", "知识库名称不能为空"),
    KB_NOT_DRAFT("2003", "仅草稿知识库可编辑"),

    // 文档
    DOC_DUPLICATE("3001", "该知识库中已存在同名文档"),
    DOC_NOT_FOUND("3002", "文档不存在"),
    DOC_NOT_READY("3003", "文档尚未处理完成，请稍后再试"),
    DOC_CONTENT_EMPTY("3004", "未找到可展示的文档内容"),
    ;

    private final String code;
    private final String message;
}
