package com.anshun.dms.mapper;

import com.anshun.dms.dto.LeasePageQuery;
import com.anshun.dms.vo.LeaseVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDate;

public interface LeaseMapper {
    long countPage(LeasePageQuery query);
    List<LeaseVO> selectPage(LeasePageQuery query);
    LeaseVO selectById(@Param("id") long id);
    long countOverlappingLeases(@Param("adPositionCode") String adPositionCode,
                                @Param("leaseStartDate") LocalDate leaseStartDate,
                                @Param("leaseEndDate") LocalDate leaseEndDate,
                                @Param("excludeLeaseId") Long excludeLeaseId);
    int insert(LeaseVO lease);
    int update(@Param("lease") LeaseVO lease, @Param("expectedVersion") int expectedVersion);
    int submitForApproval(@Param("id") long id, @Param("expectedVersion") int expectedVersion,
                          @Param("username") String username);
    int decideApproval(@Param("id") long id, @Param("expectedVersion") int expectedVersion,
                       @Param("approvalStatus") String approvalStatus, @Param("username") String username,
                       @Param("comment") String comment);
    int logicalDelete(@Param("id") long id, @Param("expectedVersion") int expectedVersion);
}
