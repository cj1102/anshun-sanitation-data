package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AiRateLimitServiceTest {
    @Test
    void keepsAWorkingSingleInstanceFallbackWhenRedisIsDisabled() {
        AiRateLimitService service = new AiRateLimitService(2, false, mock(StringRedisTemplate.class));

        service.check("tester");
        service.check("tester");

        assertThatThrownBy(() -> service.check("tester"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("过于频繁");
    }
}
