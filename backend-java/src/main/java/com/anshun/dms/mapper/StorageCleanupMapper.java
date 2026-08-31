package com.anshun.dms.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StorageCleanupMapper {
    int enqueue(@Param("objectName") String objectName);
    List<CleanupTask> selectDue(@Param("now") LocalDateTime now, @Param("limit") int limit);
    int complete(@Param("cleanupId") long cleanupId);
    int reschedule(@Param("cleanupId") long cleanupId, @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                   @Param("lastError") String lastError);

    record CleanupTask(long cleanupId, String objectName, int attempts) { }
}
