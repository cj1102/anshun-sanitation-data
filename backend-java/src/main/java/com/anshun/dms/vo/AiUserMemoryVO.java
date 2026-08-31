package com.anshun.dms.vo;

import java.time.LocalDateTime;

/** A user-owned, explicitly saved long-term preference or work-context note. */
public record AiUserMemoryVO(Long memoryId, String memoryType, String content, String source,
                             LocalDateTime createTime) { }
