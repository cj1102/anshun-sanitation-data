package com.anshun.dms.mapper;

import com.anshun.dms.vo.AiUserMemoryVO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiUserMemoryMapper {
    Integer selectUserId(@Param("username") String username);
    List<AiUserMemoryVO> selectList(@Param("userId") int userId, @Param("limit") int limit);
    AiUserMemoryVO selectByHash(@Param("userId") int userId, @Param("contentHash") String contentHash);

    @Options(useGeneratedKeys = true, keyProperty = "memoryId", keyColumn = "memory_id")
    int insert(MemoryDraft draft);
    int delete(@Param("userId") int userId, @Param("memoryId") long memoryId);

    final class MemoryDraft {
        private Long memoryId;
        private final Integer userId;
        private final String memoryType;
        private final String content;
        private final String contentHash;
        private final String source;

        public MemoryDraft(Integer userId, String memoryType, String content, String contentHash, String source) {
            this.userId = userId;
            this.memoryType = memoryType;
            this.content = content;
            this.contentHash = contentHash;
            this.source = source;
        }
        public Long getMemoryId() { return memoryId; }
        public void setMemoryId(Long memoryId) { this.memoryId = memoryId; }
        public Integer getUserId() { return userId; }
        public String getMemoryType() { return memoryType; }
        public String getContent() { return content; }
        public String getContentHash() { return contentHash; }
        public String getSource() { return source; }
    }
}
