package com.anshun.dms.vo;

import java.time.LocalDateTime;
import java.util.Map;

/** A user-owned write draft. Creating this object never changes business tables. */
public record AiPendingActionVO(String actionId, String actionType, String title, String summary,
                                Map<String, Object> fields, String requiredPermission,
                                String status, LocalDateTime expiresAt) { }
