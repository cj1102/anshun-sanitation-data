package com.anshun.dms.storage;

import com.anshun.dms.mapper.StorageCleanupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Transactional outbox consumer for MinIO deletions. Enqueue calls participate in the caller's database
 * transaction; deleting an attachment and scheduling its object cleanup therefore commit atomically.
 * MinIO deletion is idempotent, so duplicate work across application instances is safe.
 */
@Service
public class StorageCleanupService {
    private static final Logger log = LoggerFactory.getLogger(StorageCleanupService.class);
    private static final int BATCH_SIZE = 25;
    private static final int MAX_BACKOFF_SECONDS = 3600;

    private final StorageCleanupMapper mapper;
    private final MinioStorageService storage;

    public StorageCleanupService(StorageCleanupMapper mapper, MinioStorageService storage) {
        this.mapper = mapper;
        this.storage = storage;
    }

    public void enqueue(String objectName) {
        mapper.enqueue(objectName);
    }

    @Scheduled(initialDelayString = "${app.storage.cleanup.initial-delay-ms:15000}",
            fixedDelayString = "${app.storage.cleanup.fixed-delay-ms:30000}")
    public void processDueTasks() {
        List<StorageCleanupMapper.CleanupTask> tasks = mapper.selectDue(LocalDateTime.now(), BATCH_SIZE);
        for (StorageCleanupMapper.CleanupTask task : tasks) process(task);
    }

    void process(StorageCleanupMapper.CleanupTask task) {
        try {
            storage.delete(task.objectName());
            mapper.complete(task.cleanupId());
        } catch (RuntimeException exception) {
            int delaySeconds = Math.min(MAX_BACKOFF_SECONDS, 15 * (1 << Math.min(task.attempts(), 8)));
            mapper.reschedule(task.cleanupId(), LocalDateTime.now().plusSeconds(delaySeconds),
                    trim(exception.getMessage(), 500));
            log.warn("Object cleanup deferred: cleanupId={}, attempts={}, errorType={}", task.cleanupId(),
                    task.attempts() + 1, exception.getClass().getSimpleName());
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null) return "unknown storage error";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
