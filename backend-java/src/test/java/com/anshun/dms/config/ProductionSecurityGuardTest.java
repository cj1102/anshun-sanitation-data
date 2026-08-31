package com.anshun.dms.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionSecurityGuardTest {
    private static final String SECURE_JWT = "c2VjdXJlLXByb2R1Y3Rpb24tand0LXNlY3JldC1hdC1sZWFzdC0zMi1ieXRlcw==";

    @Test
    void productionRefusesRepositoryDemoInfrastructureSecrets() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ProductionSecurityGuard guard = new ProductionSecurityGuard(jdbc, encoder,
                ProductionSecurityGuard.DEMO_JWT_SECRET, "secure-minio-secret", "secure-db-secret", "StrongAdmin123");

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT");
        verify(jdbc, never()).query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any());
    }

    @Test
    void productionRefusesBlankEffectiveDatasourcePassword() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ProductionSecurityGuard guard = new ProductionSecurityGuard(jdbc, encoder,
                SECURE_JWT, "secure-minio-secret", "", "StrongAdmin123");

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("数据源密码");
        verify(jdbc, never()).query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any());
    }

    @Test
    void productionRequiresStrongPasswordWhenDemoAdministratorIsStillActive() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any()))
                .thenReturn(List.of("demo-hash"));
        when(encoder.matches("admin123", "demo-hash")).thenReturn(true);
        ProductionSecurityGuard guard = new ProductionSecurityGuard(jdbc, encoder,
                SECURE_JWT, "secure-minio-secret", "secure-db-secret", "short");

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_ADMIN_PASSWORD");
        verify(jdbc, never()).update(anyString(), any(), any());
    }

    @Test
    void productionReplacesKnownDemoAdministratorPasswordExactlyOnce() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any()))
                .thenReturn(List.of("demo-hash"));
        when(encoder.matches("admin123", "demo-hash")).thenReturn(true);
        when(encoder.encode("StrongAdmin123")).thenReturn("new-hash");
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        ProductionSecurityGuard guard = new ProductionSecurityGuard(jdbc, encoder,
                SECURE_JWT, "secure-minio-secret", "secure-db-secret", "StrongAdmin123");

        guard.run(null);

        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), parameters.capture());
        assertThat(parameters.getValue()).containsExactly("new-hash", "demo-hash");
    }
}
