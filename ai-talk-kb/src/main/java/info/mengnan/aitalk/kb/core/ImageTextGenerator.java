package info.mengnan.aitalk.kb.core;

public interface ImageTextGenerator {

    /**
     * 将图片字节流转换为文本描述
     *
     * @param data 图片数据
     * @param prompt 模型提示词标识
     * @param mimeType 图片 MIME 类型
     * @return 图片描述文本
     */
    String imageToText(byte[] data, String prompt, String mimeType);
}
