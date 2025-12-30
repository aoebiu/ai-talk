package info.mengnan.aitalk.server.content;

import com.alibaba.dashscope.tokenizers.Tokenizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Token 计数服务
 * 用于计算消息的 token 数量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCounting {

    private final Tokenizer tokenizer;
    private static final int threshold = 1200;

    /**
     * 计算文本的 token 数量
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            return tokenizer.encode(text, "none").size();
        } catch (Exception e) {
            log.warn("Failed to calculate token, use estimation method: {}", e.getMessage());
            return estimateTokenCount(text);
        }
    }

    /**
     * 估算 token 数量（降级方案）
     */
    public int estimateTokenCount(String text) {
        int chineseCount = 0;
        int otherCount = 0;

        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) {
                chineseCount++;
            } else {
                otherCount++;
            }
        }

        // 中文按1.5字符=1token，英文按4字符=1token估算
        return (int) Math.ceil(chineseCount / 1.5 + otherCount / 4.0);
    }

}
