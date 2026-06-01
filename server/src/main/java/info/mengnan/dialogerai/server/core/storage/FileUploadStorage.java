package info.mengnan.dialogerai.server.core.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 文件存储抽象基类，定义存储操作的统一模板
 */
public abstract class FileUploadStorage {

    /**
     * 返回当前存储类型。
     */
    public abstract String getType();

    /**
     * 执行文件存储。
     *
     * @param inputStream      文件输入流
     * @param storedName       存储文件名（已生成）
     * @param originalFilename 原始文件名
     * @param memberId         会员 ID
     * @return 存储结果
     */
    protected abstract StoredFile doStore(InputStream inputStream, String storedName,
                                          String originalFilename, Long memberId) throws IOException;

    /**
     * 解析本地可访问的文件路径。对于本地存储直接返回 Path；
     * 对于远程存储（OSS）会先下载到临时目录再返回临时文件 Path
     */
    protected abstract Path doResolvePath(String storedName) throws Exception;

    /**
     * 删除已存储的文件。
     */
    protected abstract void doDelete(String storedName) throws Exception;

    /**
     * 生成存储文件名：{memberId}_{uuid}{extension}
     */
    protected String generateStoredName(String originalFilename, Long memberId) {
        String extension = getFileExtension(originalFilename);
        return memberId + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    /**
     * 存储文件（调用 doStore 模板）
     */
    public final StoredFile store(InputStream inputStream, String originalFilename, Long memberId) throws IOException {
        String storedName = generateStoredName(originalFilename, memberId);
        return doStore(inputStream, storedName, originalFilename, memberId);
    }

    /**
     * 解析路径（调用 doResolvePath 模板）
     */
    public final Path resolvePath(String storedName) throws Exception {
        return doResolvePath(storedName);
    }

    /**
     * 删除文件（调用 doDelete 模板）
     */
    public final void delete(String storedName) throws Exception {
        doDelete(storedName);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 文件存储后的结果信息。
     */
    @Getter
    @AllArgsConstructor
    public static class StoredFile {

        private final String storageType;

        /** 存储文件名：本地为 storedName，OSS 为 object key */
        private final String storedName;

        /** 访问路径：本地为本地文件系统路径字符串，OSS 为下载 URL */
        private final String accessPath;
    }

}
