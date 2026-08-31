package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.mapper.LeaseAttachmentMapper;
import com.anshun.dms.storage.MinioStorageService;
import com.anshun.dms.storage.StorageCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseAttachmentServiceTest {
    @Mock LeaseAttachmentMapper mapper;
    @Mock MinioStorageService storage;
    @Mock StorageCleanupService storageCleanup;
    @InjectMocks LeaseAttachmentService service;

    @Test
    void deletesDatabaseRecordAndEnqueuesObjectWithoutDeletingMinioInline() {
        when(mapper.selectStorageRecord(3L, 9L)).thenReturn(record());
        when(mapper.delete(3L, 9L)).thenReturn(1);

        service.delete(3L, 9L);

        verify(mapper).delete(3L, 9L);
        verify(storageCleanup).enqueue("leases/3/file.pdf");
        verify(storage, never()).delete("leases/3/file.pdf");
    }

    @Test
    void doesNotEnqueueCleanupWhenConcurrentDeleteWins() {
        when(mapper.selectStorageRecord(3L, 9L)).thenReturn(record());
        when(mapper.delete(3L, 9L)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(3L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态已发生变化");

        verify(storageCleanup, never()).enqueue("leases/3/file.pdf");
    }

    private LeaseAttachmentMapper.AttachmentStorageRecord record() {
        return new LeaseAttachmentMapper.AttachmentStorageRecord(9L, 3L, "file.pdf", "leases/3/file.pdf",
                "application/pdf", 12L, 1, "admin");
    }
}
