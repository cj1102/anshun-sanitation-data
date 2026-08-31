package com.anshun.dms.storage;

import com.anshun.dms.common.StorageException;
import com.anshun.dms.mapper.StorageCleanupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StorageCleanupServiceTest {
    @Mock StorageCleanupMapper mapper;
    @Mock MinioStorageService storage;
    @InjectMocks StorageCleanupService cleanupService;

    @Test
    void completesTaskAfterIdempotentObjectDeletion() {
        StorageCleanupMapper.CleanupTask task = new StorageCleanupMapper.CleanupTask(7L, "leases/1/file.pdf", 0);

        cleanupService.process(task);

        verify(storage).delete("leases/1/file.pdf");
        verify(mapper).complete(7L);
        verify(mapper, never()).reschedule(any(Long.class), any(LocalDateTime.class), any());
    }

    @Test
    void reschedulesTaskWhenObjectStorageIsUnavailable() {
        StorageCleanupMapper.CleanupTask task = new StorageCleanupMapper.CleanupTask(8L, "leases/1/file.pdf", 2);
        org.mockito.Mockito.doThrow(new StorageException("MinIO unavailable", new RuntimeException()))
                .when(storage).delete(task.objectName());

        cleanupService.process(task);

        verify(mapper, never()).complete(8L);
        verify(mapper).reschedule(eq(8L), any(LocalDateTime.class), eq("MinIO unavailable"));
    }
}
