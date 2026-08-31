package com.anshun.dms.storage;

import com.anshun.dms.common.StorageException;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.BucketExistsArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class MinioStorageService {
    private static final long DEFAULT_MAX_SIZE = 20L * 1024 * 1024;
    private final MinioClient client;
    private final String bucket;
    private final long maxFileSize;
    private final UploadFileInspector fileInspector;

    public MinioStorageService(UploadFileInspector fileInspector,
                               @Value("${app.storage.endpoint}") String endpoint,
                               @Value("${app.storage.access-key}") String accessKey,
                               @Value("${app.storage.secret-key}") String secretKey,
                               @Value("${app.storage.bucket}") String bucket,
                               @Value("${app.storage.max-file-size:" + DEFAULT_MAX_SIZE + "}") long maxFileSize) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
        this.maxFileSize = maxFileSize;
        this.fileInspector = fileInspector;
    }

    public StoredObject upload(long leaseId, MultipartFile file) {
        UploadFileInspector.Inspection inspection = fileInspector.inspectAttachment(file, maxFileSize);
        return uploadToPath(file, "leases/" + leaseId, "附件", inspection.contentType());
    }

    public StoredObject uploadKnowledge(MultipartFile file) {
        UploadFileInspector.Inspection inspection = fileInspector.inspectKnowledge(file, maxFileSize);
        return uploadToPath(file, "knowledge", "知识库文件", inspection.contentType());
    }

    public void validateKnowledge(MultipartFile file) {
        fileInspector.inspectKnowledge(file, maxFileSize);
    }

    private StoredObject uploadToPath(MultipartFile file, String directory, String fileLabel, String trustedContentType) {
        String cleanedName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
        String originalName = StringUtils.getFilename(cleanedName);
        if (!StringUtils.hasText(originalName)) originalName = "attachment";
        String objectName = directory + "/" + UUID.randomUUID() + "-" + originalName;
        try (InputStream input = file.getInputStream()) {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(objectName).stream(input, file.getSize(), -1)
                    .contentType(trustedContentType).build());
            return new StoredObject(objectName, originalName, trustedContentType, file.getSize());
        } catch (Exception exception) {
            throw new StorageException(fileLabel + "存储服务暂时不可用，请稍后重试", exception);
        }
    }

    public InputStream download(String objectName) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception exception) {
            throw new StorageException("附件文件暂时无法读取", exception);
        }
    }

    public void delete(String objectName) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception exception) {
            throw new StorageException("附件文件删除失败，请稍后重试", exception);
        }
    }

    private void ensureBucket() throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    public record StoredObject(String objectName, String originalFilename, String contentType, long size) { }
}
