package com.anshun.dms.integration;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.PositionSaveRequest;
import com.anshun.dms.service.PositionService;
import com.anshun.dms.service.AuthService;
import com.anshun.dms.service.SystemUserService;
import com.anshun.dms.dto.LoginRequest;
import com.anshun.dms.storage.MinioStorageService;
import com.anshun.dms.storage.StorageCleanupService;
import com.anshun.dms.vo.PositionVO;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Testcontainers(disabledWithoutDocker = true)
class MySqlApplicationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("anshun_integration_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired PositionService positionService;
    @Autowired AuthService authService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SystemUserService systemUserService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StorageCleanupService storageCleanupService;
    @MockitoBean MinioStorageService minioStorageService;

    @Test
    void emptyDatabaseRunsEveryFlywayMigrationAndStartsApplicationContext() {
        MigrationInfo current = flyway.info().current();

        assertThat(current).isNotNull();
        assertThat(Integer.parseInt(current.getVersion().toString())).isGreaterThanOrEqualTo(18);
        assertThat(flyway.info().applied()).hasSizeGreaterThanOrEqualTo(18);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()", Integer.class))
                .isGreaterThanOrEqualTo(20);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'storage_cleanup_task'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_user WHERE username = 'admin'", Integer.class)).isEqualTo(1);
    }

    @Test
    void storageCleanupOutboxIsConsumedAgainstRealMySql() {
        String objectName = "integration/" + UUID.randomUUID() + ".pdf";
        storageCleanupService.enqueue(objectName);

        storageCleanupService.processDueTasks();

        org.mockito.Mockito.verify(minioStorageService).delete(objectName);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM storage_cleanup_task WHERE object_name=?",
                Integer.class, objectName)).isZero();
    }

    @Test
    void concurrentUpdatesWithSameVersionAllowExactlyOneWriter() throws Exception {
        String code = "IT-CONCURRENT-001";
        jdbc.update("""
                INSERT INTO t_ad_position
                    (ad_position_code, ad_location, single_side_area, total_ad_area, ad_specification,
                     district, road_name, status, remark, version, deleted)
                VALUES (?, '并发测试初始位置', '6m×3m', 18, '双面立柱式',
                        '西秀区', '测试大道', 'vacant', 'integration-test', 0, 0)
                """, code);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<UpdateOutcome> first = updateTask(code, "并发更新 A", ready, start);
            Callable<UpdateOutcome> second = updateTask(code, "并发更新 B", ready, start);
            Future<UpdateOutcome> firstResult = executor.submit(first);
            Future<UpdateOutcome> secondResult = executor.submit(second);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<UpdateOutcome> outcomes = List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS));

            assertThat(outcomes).containsExactlyInAnyOrder(UpdateOutcome.SUCCESS, UpdateOutcome.CONFLICT);
            assertThat(jdbc.queryForObject("SELECT version FROM t_ad_position WHERE ad_position_code = ?", Integer.class, code))
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT ad_location FROM t_ad_position WHERE ad_position_code = ?", String.class, code))
                    .isIn("并发更新 A", "并发更新 B");
        } finally {
            executor.shutdownNow();
            jdbc.update("DELETE FROM t_ad_position WHERE ad_position_code = ?", code);
        }
    }

    @Test
    void failedLoginLockUsesApplicationTimeInsteadOfDatabaseServerTimezone() {
        String username = "timezone-lock-user";
        jdbc.update("""
                INSERT INTO t_user (username, password_hash, nickname, role, status)
                VALUES (?, ?, '时区锁定测试', 'user', 'active')
                """, username, passwordEncoder.encode("CorrectPass1"));
        try {
            for (int attempt = 0; attempt < 5; attempt++) {
                assertThatThrownBy(() -> authService.login(new LoginRequest(username, "WrongPass1"), "127.0.0.9"))
                        .isInstanceOfSatisfying(BusinessException.class,
                                error -> assertThat(error.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
            }
            assertThat(jdbc.queryForObject("SELECT failed_login_attempts FROM t_user WHERE username=?", Integer.class, username))
                    .isEqualTo(5);
            assertThatThrownBy(() -> authService.login(new LoginRequest(username, "CorrectPass1"), "127.0.0.9"))
                    .isInstanceOfSatisfying(BusinessException.class,
                            error -> assertThat(error.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        } finally {
            jdbc.update("DELETE FROM t_user WHERE username=?", username);
        }
    }

    @Test
    void lastAdministratorCannotBeRemovedInRealTransaction() {
        Long adminId = jdbc.queryForObject("SELECT user_id FROM t_user WHERE username='admin'", Long.class);

        assertThatThrownBy(() -> systemUserService.assignRoles(adminId, List.of("VIEWER")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最后一个管理员");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.role_id=ur.role_id
                WHERE ur.user_id=? AND r.role_code='ADMIN'
                """, Integer.class, adminId)).isEqualTo(1);
    }

    @Test
    void jwtAuthenticationRbacAndTokenVersionAreEnforcedThroughRealSecurityFilters() throws Exception {
        String username = "rbac-http-user";
        jdbc.update("""
                INSERT INTO t_user (username, password_hash, nickname, role, status)
                VALUES (?, ?, 'RBAC HTTP 测试', 'user', 'active')
                """, username, passwordEncoder.encode("ViewerPass1"));
        Long userId = jdbc.queryForObject("SELECT user_id FROM t_user WHERE username=?", Long.class, username);
        jdbc.update("""
                INSERT INTO sys_user_role (user_id, role_id)
                SELECT ?, role_id FROM sys_role WHERE role_code='VIEWER'
                """, userId);

        try {
            mockMvc.perform(get("/api/system/users"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));

            String viewerToken = loginToken(username, "ViewerPass1");
            mockMvc.perform(get("/api/system/users").header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));

            systemUserService.assignRoles(userId, List.of("OPERATOR"));
            mockMvc.perform(get("/api/positions").header("Authorization", "Bearer " + viewerToken))
                    .andExpect(status().isUnauthorized());

            String operatorToken = loginToken(username, "ViewerPass1");
            mockMvc.perform(get("/api/positions").header("Authorization", "Bearer " + operatorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            String adminToken = loginToken("admin", "admin123");
            mockMvc.perform(get("/api/system/users").header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            jdbc.update("DELETE FROM t_user WHERE username=?", username);
        }
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("token").asText();
    }

    private Callable<UpdateOutcome> updateTask(String code, String location, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                PositionVO updated = positionService.update(code, request(code, location));
                return updated.version() == 1 ? UpdateOutcome.SUCCESS : UpdateOutcome.UNEXPECTED;
            } catch (BusinessException exception) {
                return exception.status().value() == 409 ? UpdateOutcome.CONFLICT : UpdateOutcome.UNEXPECTED;
            }
        };
    }

    private PositionSaveRequest request(String code, String location) {
        return new PositionSaveRequest(code, location, "6m×3m", 18, "双面立柱式",
                null, null, "西秀区", "测试大道", "vacant", "integration-test", 0);
    }

    private enum UpdateOutcome { SUCCESS, CONFLICT, UNEXPECTED }
}
