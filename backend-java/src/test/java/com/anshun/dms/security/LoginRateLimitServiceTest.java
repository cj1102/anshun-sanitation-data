package com.anshun.dms.security;

import com.anshun.dms.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LoginRateLimitServiceTest {
    @Test
    void localFallbackLimitsUsernameBeforeExpensivePasswordChecks() {
        LoginRateLimitService service = new LoginRateLimitService(mock(StringRedisTemplate.class), false, 2, 20);

        service.check("alice", "127.0.0.1");
        service.check("alice", "127.0.0.1");

        assertThatThrownBy(() -> service.check("alice", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("过于频繁");
    }
}
