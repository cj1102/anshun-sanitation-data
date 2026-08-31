package com.anshun.dms.mapper;

import com.anshun.dms.model.auth.AuthUser;
import com.anshun.dms.model.auth.UserTokenState;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

public interface AuthMapper {
    AuthUser findByUsername(@Param("username") String username);
    UserTokenState findTokenState(@Param("userId") long userId);
    int insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash,
                   @Param("nickname") String nickname);
    int assignViewerRole(@Param("username") String username);
    int recordLoginFailure(@Param("userId") long userId, @Param("maxAttempts") int maxAttempts,
                           @Param("lockedUntil") LocalDateTime lockedUntil);
    int resetLoginFailures(@Param("userId") long userId);
    List<String> selectRoleCodes(@Param("userId") long userId);
    List<String> selectPermissionCodes(@Param("userId") long userId);
}
