package com.anshun.dms.security;

import com.anshun.dms.common.BusinessException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {

    @Test
    void revokeUsesLocalFallbackWhenRedisIsUnavailable() {
        Fixture fixture = new Fixture(false, "token-1");
        doThrow(new IllegalStateException("redis unavailable"))
                .when(fixture.values).set(anyString(), anyString(), any());

        fixture.service.revoke("jwt");

        assertThat(fixture.service.isRevoked("jwt")).isTrue();
        verify(fixture.values).set(anyString(), anyString(), any());
    }

    @Test
    void revokeFailsClosedWhenConfiguredAndRedisIsUnavailable() {
        Fixture fixture = new Fixture(true, "token-2");
        doThrow(new IllegalStateException("redis unavailable"))
                .when(fixture.values).set(anyString(), anyString(), any());

        assertThatThrownBy(() -> fixture.service.revoke("jwt"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.status().value()).isEqualTo(503));
    }

    @Test
    void isRevokedReadsRedisWhenTokenIsNotInLocalFallback() {
        Fixture fixture = new Fixture(false, "token-3");
        when(fixture.redis.hasKey("auth:blacklist:token-3")).thenReturn(true);

        assertThat(fixture.service.isRevoked("jwt")).isTrue();
    }

    @Test
    void isRevokedTreatsTokenWithoutIdAsInvalid() {
        Fixture fixture = new Fixture(false, null);

        assertThat(fixture.service.isRevoked("jwt")).isTrue();
    }

    @Test
    void isRevokedFailsClosedWhenRedisLookupFails() {
        Fixture fixture = new Fixture(true, "token-4");
        when(fixture.redis.hasKey(anyString())).thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> fixture.service.isRevoked("jwt"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.status().value()).isEqualTo(503));
    }

    private static final class Fixture {
        private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        private final ValueOperations<String, String> values = mock(ValueOperations.class);
        private final JwtTokenService jwt = mock(JwtTokenService.class);
        private final TokenBlacklistService service;

        private Fixture(boolean failClosed, String tokenId) {
            Claims claims = mock(Claims.class);
            when(claims.getId()).thenReturn(tokenId);
            when(claims.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(300)));
            when(jwt.parse("jwt")).thenReturn(claims);
            when(redis.opsForValue()).thenReturn(values);
            service = new TokenBlacklistService(redis, jwt, failClosed);
        }
    }
}
