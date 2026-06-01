package info.mengnan.dialogerai.server.param.document;

import info.mengnan.dialogerai.common.util.JSONUtil;
import lombok.Data;

/**
 * 文档清洗规则配置，随上传请求传入，每条规则均可独立开关。
 */
@Data
public class CleaningConfig {

    public static CleaningConfig fromJson(String json) {
        return (json != null && !json.isBlank())
                ? JSONUtil.toBean(json, CleaningConfig.class)
                : new CleaningConfig();
    }

    public String toJsonString() {
        return JSONUtil.toJsonStr(this);
    }

    /** 空白规范化：合并连续空格/Tab，超过2个连续换行压缩为2个，去除行首尾空白 */
    private boolean normalizeWhitespace = true;

    /** 断行合并：将单个换行（非段落分隔）合并为空格，保留双换行作为段落边界（PDF 常见问题） */
    private boolean mergeLineBreaks = false;

    /** 低价值段落过滤：丢弃极短段落（长度 < minParagraphLength）及纯符号/数字段落 */
    private boolean filterLowValueParagraphs = false;

    /** 重复段落去重：移除内容完全相同的重复段落（常见于模板文档） */
    private boolean deduplicateParagraphs = false;

    /** 低价值段落的最小字符长度阈值，仅在 filterLowValueParagraphs=true 时生效 */
    private int minParagraphLength = 10;
}
