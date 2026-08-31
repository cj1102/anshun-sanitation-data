package com.anshun.dms.audit;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTest {

    @Test
    void listNormalizesFiltersAndReturnsTypedPage() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("log_id", 7L, "module_name", "LEASE")));
        AuditLogService service = new AuditLogService(jdbc);

        AuditLogService.AuditLogPage page = service.list(0, 500, " LEASE ", " admin ");

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.data()).containsExactly(Map.of("log_id", 7L, "module_name", "LEASE"));
        verify(jdbc).queryForObject(anyString(), eq(Long.class), aryEq(new Object[]{"LEASE", "%admin%"}));
        verify(jdbc).queryForList(anyString(), aryEq(new Object[]{"LEASE", "%admin%", 100, 0}));
    }
}
