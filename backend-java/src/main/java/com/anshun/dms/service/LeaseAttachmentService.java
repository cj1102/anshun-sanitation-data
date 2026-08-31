package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.common.TransactionCallbacks;
import com.anshun.dms.mapper.LeaseAttachmentMapper;
import com.anshun.dms.storage.MinioStorageService;
import com.anshun.dms.storage.StorageCleanupService;
import com.anshun.dms.vo.LeaseAttachmentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
public class LeaseAttachmentService {
    private static final Logger log = LoggerFactory.getLogger(LeaseAttachmentService.class);
    private final LeaseAttachmentMapper attachmentMapper;
    private final MinioStorageService storage;
    private final StorageCleanupService storageCleanup;

    public LeaseAttachmentService(LeaseAttachmentMapper attachmentMapper, MinioStorageService storage,
                                  StorageCleanupService storageCleanup) {
        this.attachmentMapper = attachmentMapper;
        this.storage = storage;
        this.storageCleanup = storageCleanup;
    }

    public List<LeaseAttachmentVO> list(long leaseId) {
        requireLease(leaseId);
        return attachmentMapper.selectList(leaseId);
    }

    @Transactional
    public void upload(long leaseId, MultipartFile file, String username) {
        requireLease(leaseId);
        MinioStorageService.StoredObject stored = storage.upload(leaseId, file);
        TransactionCallbacks.afterRollback(() -> enqueueCleanupQuietly(stored.objectName()));
        try {
            Integer userId = username == null || "anonymous".equals(username) ? null : attachmentMapper.selectUserId(username);
            attachmentMapper.insert(new LeaseAttachmentMapper.AttachmentStorageRecord(null, leaseId, stored.originalFilename(),
                    stored.objectName(), stored.contentType(), stored.size(), userId, username == null ? "anonymous" : username));
        } catch (RuntimeException exception) {
            deleteUploadedObjectQuietly(stored.objectName());
            throw exception;
        }
    }

    public DownloadedAttachment download(long leaseId, long attachmentId) {
        LeaseAttachmentMapper.AttachmentStorageRecord record = requireAttachment(leaseId, attachmentId);
        return new DownloadedAttachment(record.originalFilename(), record.contentType(), record.fileSize(), storage.download(record.objectName()));
    }

    @Transactional
    public void delete(long leaseId, long attachmentId) {
        LeaseAttachmentMapper.AttachmentStorageRecord record = requireAttachment(leaseId, attachmentId);
        if (attachmentMapper.delete(leaseId, attachmentId) != 1) {
            throw BusinessException.conflict("附件状态已发生变化，请刷新后重试");
        }
        storageCleanup.enqueue(record.objectName());
    }

    private void requireLease(long leaseId) {
        if (!attachmentMapper.leaseExists(leaseId)) throw BusinessException.notFound("合同不存在");
    }
    private LeaseAttachmentMapper.AttachmentStorageRecord requireAttachment(long leaseId, long attachmentId) {
        LeaseAttachmentMapper.AttachmentStorageRecord record = attachmentMapper.selectStorageRecord(leaseId, attachmentId);
        if (record == null) throw BusinessException.notFound("附件不存在");
        return record;
    }

    private void deleteUploadedObjectQuietly(String objectName) {
        try {
            storage.delete(objectName);
        } catch (RuntimeException exception) {
            log.error("Uploaded object compensation failed: objectName={}, errorType={}", objectName,
                    exception.getClass().getSimpleName());
        }
    }

    private void enqueueCleanupQuietly(String objectName) {
        try {
            storageCleanup.enqueue(objectName);
        } catch (RuntimeException exception) {
            log.error("Unable to enqueue rolled-back upload cleanup: objectName={}, errorType={}", objectName,
                    exception.getClass().getSimpleName());
        }
    }

    public record DownloadedAttachment(String filename, String contentType, long size, InputStream stream) { }
}
