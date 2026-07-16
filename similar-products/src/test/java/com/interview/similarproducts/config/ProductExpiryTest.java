package com.interview.similarproducts.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.interview.similarproducts.model.ProductDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProductExpiryTest {

    private static final Duration SUCCESS_TTL = Duration.ofSeconds(30);
    private static final Duration FAILURE_TTL = Duration.ofSeconds(5);

    private final AtomicLong nanos = new AtomicLong();

    private final Cache<Object, Object> cache = Caffeine.newBuilder()
            .ticker(nanos::get)
            .expireAfter(new ProductExpiry(SUCCESS_TTL, FAILURE_TTL))
            .build();

    private final ProductDetail detail = new ProductDetail("1", "Shirt", new BigDecimal("9.99"), true);

    @Test
    void cachedFailuresExpireBeforeSuccesses() {
        cache.put("ok", detail);
        cache.put("ko", new Object());

        advance(FAILURE_TTL.plusSeconds(1));

        assertThat(cache.getIfPresent("ko")).isNull();
        assertThat(cache.getIfPresent("ok")).isEqualTo(detail);
    }

    @Test
    void successesExpireAfterTheirOwnTtl() {
        cache.put("ok", detail);

        advance(SUCCESS_TTL.plusSeconds(1));

        assertThat(cache.getIfPresent("ok")).isNull();
    }

    @Test
    void readingAnEntryDoesNotExtendItsLifetime() {
        cache.put("ko", new Object());

        advance(FAILURE_TTL.minusSeconds(1));
        assertThat(cache.getIfPresent("ko")).isNotNull();

        advance(Duration.ofSeconds(2));
        assertThat(cache.getIfPresent("ko")).isNull();
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }
}
