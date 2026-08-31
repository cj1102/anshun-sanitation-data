package com.anshun.dms.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LeaseAttachmentVO(Long attachmentId, String originalFilename, String contentType, Long fileSize,
                                String uploaderUsername, LocalDateTime createTime) { }
