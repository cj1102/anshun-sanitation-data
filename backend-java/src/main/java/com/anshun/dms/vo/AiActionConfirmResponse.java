package com.anshun.dms.vo;

import java.util.Map;

/** Returned after an explicitly confirmed, idempotent Agent action. */
public record AiActionConfirmResponse(String actionId, String status, String message, Map<String, Object> result) { }
