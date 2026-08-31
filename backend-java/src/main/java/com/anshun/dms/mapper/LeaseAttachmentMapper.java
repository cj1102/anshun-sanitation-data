package com.anshun.dms.mapper;

import com.anshun.dms.vo.LeaseAttachmentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LeaseAttachmentMapper {
    boolean leaseExists(@Param("leaseId") long leaseId);
    List<LeaseAttachmentVO> selectList(@Param("leaseId") long leaseId);
    AttachmentStorageRecord selectStorageRecord(@Param("leaseId") long leaseId, @Param("attachmentId") long attachmentId);
    Integer selectUserId(@Param("username") String username);
    int insert(AttachmentStorageRecord record);
    int delete(@Param("leaseId") long leaseId, @Param("attachmentId") long attachmentId);

    record AttachmentStorageRecord(Long attachmentId, Long leaseId, String originalFilename, String objectName,
                                   String contentType, Long fileSize, Integer uploaderId, String uploaderUsername) { }
}
