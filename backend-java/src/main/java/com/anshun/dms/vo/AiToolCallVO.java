package com.anshun.dms.vo;

/** Safe, user-facing summary of a tool invoked during the current Agent turn. */
public record AiToolCallVO(String toolName, String summary, boolean success, long durationMs) { }
