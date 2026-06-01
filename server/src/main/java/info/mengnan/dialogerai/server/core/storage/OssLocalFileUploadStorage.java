package info.mengnan.dialogerai.server.core.storage;

import co.elastic.clients.elasticsearch.indices.StorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * OSS 文件存储实现（待接入）
 */
@Slf4j
// @Component
public class OssLocalFileUploadStorage extends FileUploadStorage {

    @Override
    public String getType() {
        return "OSS";
    }

    @Override
    protected StoredFile doStore(InputStream inputStream, String storedName,
                                  String originalFilename, Long memberId) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Path doResolvePath(String storedName) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDelete(String storedName) throws Exception {
        throw new UnsupportedOperationException();
    }
}
