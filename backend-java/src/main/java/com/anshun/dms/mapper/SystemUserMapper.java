package com.anshun.dms.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SystemUserMapper {
    long countUsers(@Param("search") String search);
    List<Map<String, Object>> selectUsers(@Param("search") String search, @Param("limit") int limit,
                                          @Param("offset") int offset);
    List<Map<String, Object>> selectRoles();
    List<Map<String, Object>> selectPermissions();
    int countUser(@Param("userId") long userId);
    int countActiveRoles(@Param("roleCodes") List<String> roleCodes);
    List<Long> selectAdminUserIdsForUpdate();
    int deleteUserRoles(@Param("userId") long userId);
    int insertUserRoles(@Param("userId") long userId, @Param("roleCodes") List<String> roleCodes);
    int incrementTokenVersion(@Param("userId") long userId);
}
