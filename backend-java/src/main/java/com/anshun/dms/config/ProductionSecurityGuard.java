package com.anshun.dms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Refuses to start the production profile with repository demo credentials.
 * On the first production boot, BOOTSTRAP_ADMIN_PASSWORD may replace the known
 * demo password exactly once; subsequent starts leave the changed password intact.
 */
@Component
@Profile("prod")
public class ProductionSecurityGuard implements ApplicationRunner {
    static final String DEMO_JWT_SECRET = "YW5zaHVuLWFkLWRtcy1kZW1vLXNlY3JldC1rZXktMjAyNi0xMjM0NTY=";
    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityGuard.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String jwtSecret;
    private final String storageSecret;
    private final String databasePassword;
    private final String bootstrapAdminPassword;

    public ProductionSecurityGuard(JdbcTemplate jdbc,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${app.jwt.secret}") String jwtSecret,
                                   @Value("${app.storage.secret-key}") String storageSecret,
                                   @Value("${spring.datasource.password:}") String databasePassword,
                                   @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String bootstrapAdminPassword) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
        this.storageSecret = storageSecret;
        this.databasePassword = databasePassword;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        rejectDemoInfrastructureSecrets();
        rotateDemoAdministratorPassword();
    }

    private void rejectDemoInfrastructureSecrets() {
        if (DEMO_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("生产环境禁止使用仓库内置 JWT 演示密钥，请设置 JWT_SECRET");
        }
        if ("minioadmin-change-me".equals(storageSecret)) {
            throw new IllegalStateException("生产环境禁止使用 MinIO 演示密钥，请设置 MINIO_SECRET_KEY");
        }
        if (databasePassword == null || databasePassword.isBlank()
                || "change-me-in-production".equals(databasePassword)) {
            throw new IllegalStateException("生产环境禁止使用空密码或 MySQL 演示密码，请设置安全的数据源密码");
        }
    }

    private void rotateDemoAdministratorPassword() {
        List<String> hashes = jdbc.query(
                "SELECT password_hash FROM t_user WHERE username='admin' AND status='active'",
                (resultSet, rowNumber) -> resultSet.getString(1));
        if (hashes.isEmpty() || !passwordEncoder.matches("admin123", hashes.get(0))) return;
        if (!isStrongBootstrapPassword(bootstrapAdminPassword)) {
            throw new IllegalStateException(
                    "生产数据库仍启用演示管理员 admin，请设置至少 12 位且同时包含字母和数字的 BOOTSTRAP_ADMIN_PASSWORD");
        }
        int changed = jdbc.update("""
                UPDATE t_user
                SET password_hash=?, token_version=token_version+1, failed_login_attempts=0, locked_until=NULL
                WHERE username='admin' AND status='active' AND password_hash=?
                """, passwordEncoder.encode(bootstrapAdminPassword), hashes.get(0));
        if (changed != 1) {
            throw new IllegalStateException("初始化生产管理员密码时发生并发冲突，请重新启动应用");
        }
        log.info("Production demo administrator password was replaced during secure bootstrap");
    }

    private boolean isStrongBootstrapPassword(String password) {
        return password != null && password.length() >= 12
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit)
                && !"admin123".equalsIgnoreCase(password);
    }
}
