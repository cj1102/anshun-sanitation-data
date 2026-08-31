package com.anshun.dms.mapper;

import com.anshun.dms.vo.AiKnowledgeDocumentVO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiKnowledgeMapper {
    Integer selectUserId(@Param("username") String username);
    List<AiKnowledgeDocumentVO> selectDocuments();
    KnowledgeDocumentRecord selectDocument(@Param("documentId") long documentId);
    /** Bounded lexical candidate pool; vector hits are loaded independently by primary key. */
    List<KnowledgeChunkCandidate> selectAccessibleChunks(@Param("roles") List<String> roles);
    /** Rechecks document state and role ACL when hydrating untrusted vector-store hits. */
    List<KnowledgeChunkCandidate> selectAccessibleChunksByIds(@Param("chunkIds") List<Long> chunkIds,
                                                               @Param("roles") List<String> roles);
    List<KnowledgeChunkCandidate> selectChunksByDocument(@Param("documentId") long documentId);
    List<KnowledgeChunkCandidate> selectAllChunks();

    @Options(useGeneratedKeys = true, keyProperty = "documentId", keyColumn = "document_id")
    int insertDocument(KnowledgeDocumentDraft draft);
    int insertChunks(@Param("chunks") List<KnowledgeChunkDraft> chunks);
    int logicalDelete(@Param("documentId") long documentId);

    record KnowledgeDocumentRecord(Long documentId, String title, String objectName, String visibleRoles) { }
    record KnowledgeChunkCandidate(Long chunkId, Long documentId, String title, Integer chunkNo,
                                   Integer pageStart, Integer pageEnd, String chunkText) { }
    record KnowledgeChunkDraft(Long documentId, Integer chunkNo, Integer pageStart, Integer pageEnd, String chunkText) { }

    final class KnowledgeDocumentDraft {
        private Long documentId;
        private final String title;
        private final String originalFilename;
        private final String objectName;
        private final String contentType;
        private final Long fileSize;
        private final String visibleRoles;
        private final Integer uploaderId;
        private final String uploaderUsername;

        public KnowledgeDocumentDraft(String title, String originalFilename, String objectName, String contentType,
                                      Long fileSize, String visibleRoles, Integer uploaderId, String uploaderUsername) {
            this.title = title;
            this.originalFilename = originalFilename;
            this.objectName = objectName;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.visibleRoles = visibleRoles;
            this.uploaderId = uploaderId;
            this.uploaderUsername = uploaderUsername;
        }
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public String getTitle() { return title; }
        public String getOriginalFilename() { return originalFilename; }
        public String getObjectName() { return objectName; }
        public String getContentType() { return contentType; }
        public Long getFileSize() { return fileSize; }
        public String getVisibleRoles() { return visibleRoles; }
        public Integer getUploaderId() { return uploaderId; }
        public String getUploaderUsername() { return uploaderUsername; }
    }
}
