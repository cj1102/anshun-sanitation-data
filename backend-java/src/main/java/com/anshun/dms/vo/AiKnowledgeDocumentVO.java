package com.anshun.dms.vo;

import java.time.LocalDateTime;

public record AiKnowledgeDocumentVO(Long documentId, String title, String originalFilename, String contentType,
                                    Long fileSize, String visibleRoles, String uploaderUsername,
                                    Integer chunkCount, LocalDateTime createTime) { }
