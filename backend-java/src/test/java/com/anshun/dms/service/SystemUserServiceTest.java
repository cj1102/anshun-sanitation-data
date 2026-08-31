package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.mapper.SystemUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {
    @Mock SystemUserMapper mapper;
    @InjectMocks SystemUserService service;

    @Test
    void roleAssignmentIsNormalizedAndInvalidatesExistingTokens() {
        when(mapper.countUser(9)).thenReturn(1);
        when(mapper.countActiveRoles(List.of("OPERATOR", "FINANCE"))).thenReturn(2);
        when(mapper.selectAdminUserIdsForUpdate()).thenReturn(List.of(1L));
        when(mapper.insertUserRoles(9, List.of("OPERATOR", "FINANCE"))).thenReturn(2);
        when(mapper.incrementTokenVersion(9)).thenReturn(1);

        service.assignRoles(9, List.of("operator", "FINANCE", "operator"));

        var order = inOrder(mapper);
        order.verify(mapper).deleteUserRoles(9);
        order.verify(mapper).insertUserRoles(9, List.of("OPERATOR", "FINANCE"));
        order.verify(mapper).incrementTokenVersion(9);
    }

    @Test
    void unknownRoleIsRejectedBeforeChangingAssignments() {
        when(mapper.countUser(9)).thenReturn(1);
        when(mapper.countActiveRoles(List.of("UNKNOWN"))).thenReturn(0);

        assertThatThrownBy(() -> service.assignRoles(9, List.of("UNKNOWN")))
                .isInstanceOf(BusinessException.class);
        verify(mapper, never()).deleteUserRoles(9);
    }

    @Test
    void lastAdministratorCannotBeDemoted() {
        when(mapper.countUser(1)).thenReturn(1);
        when(mapper.countActiveRoles(List.of("VIEWER"))).thenReturn(1);
        when(mapper.selectAdminUserIdsForUpdate()).thenReturn(List.of(1L));

        assertThatThrownBy(() -> service.assignRoles(1, List.of("VIEWER")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最后一个管理员");
        verify(mapper, never()).deleteUserRoles(1);
    }
}
