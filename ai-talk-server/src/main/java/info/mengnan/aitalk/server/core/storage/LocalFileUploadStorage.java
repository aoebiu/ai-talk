package info.mengnan.aitalk.server.core.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储实现。
 */
@Slf4j
@Component
public class LocalFileUploadStorage extends FileUploadStorage {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public String getType() {
        return "LOCAL";
    }

    @Override
    protected StoredFile doStore(InputStream inputStream, String storedName,
                                  String originalFilename, Long memberId) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path storedPath = uploadPath.resolve(storedName);
        Files.copy(inputStream, storedPath, StandardCopyOption.REPLACE_EXISTING);
        return new StoredFile("LOCAL", storedName, storedPath.toString());
    }

    @Override
    protected Path doResolvePath(String storedName) {
        return Paths.get(uploadDir).resolve(storedName);
    }

    @Override
    protected void doDelete(String storedName) {
        Path path = Paths.get(uploadDir).resolve(storedName);
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("temporary file deletion failed: {}", path, e);
        }
    }
}
