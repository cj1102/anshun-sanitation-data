package com.anshun.dms.mapper;

import com.anshun.dms.dto.PositionPageQuery;
import com.anshun.dms.vo.LeaseVO;
import com.anshun.dms.vo.PositionVO;
import com.anshun.dms.vo.PositionValuationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PositionMapper {
    long countPage(PositionPageQuery query);
    List<PositionVO> selectPage(PositionPageQuery query);
    PositionVO selectByCode(@Param("code") String code);
    /** Serializes contract scheduling for one position inside the caller's transaction. */
    PositionVO selectActiveForUpdate(@Param("code") String code);
    int insert(PositionVO position);
    int update(@Param("code") String code, @Param("position") PositionVO position,
               @Param("expectedVersion") int expectedVersion);
    long countBlockingLeases(@Param("code") String code);
    int logicalDelete(@Param("code") String code, @Param("expectedVersion") int expectedVersion);
    PositionValuationVO selectValuation(@Param("code") String code);
    List<LeaseVO> selectLeaseHistory(@Param("code") String code);
}
