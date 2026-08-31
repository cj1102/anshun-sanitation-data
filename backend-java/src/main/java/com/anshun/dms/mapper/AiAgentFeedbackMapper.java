package com.anshun.dms.mapper;

import org.apache.ibatis.annotations.Param;

public interface AiAgentFeedbackMapper {
    Integer selectActiveUserId(@Param("username") String username);

    /**
     * The INSERT ... SELECT keeps ownership in the SQL condition, so a user cannot submit feedback
     * for another user's run even if they guess its numeric id.
     */
    int upsertForOwnedRun(@Param("runId") long runId, @Param("userId") int userId,
                          @Param("rating") String rating, @Param("comment") String comment);
}
