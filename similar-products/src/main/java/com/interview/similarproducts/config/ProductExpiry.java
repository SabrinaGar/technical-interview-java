package com.interview.similarproducts.config;

import java.time.Duration;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Expiry;
import com.interview.similarproducts.model.ProductDetail;

/**
 * Variable expiration for the products cache: successful lookups live longer
 * than cached failures, so a broken or slow upstream is retried sooner without
 * sacrificing the hit rate of healthy products.
 */
class ProductExpiry implements Expiry<Object, Object> {

    private final long successTtlNanos;
    private final long failureTtlNanos;

    ProductExpiry(Duration successTtl, Duration failureTtl) {
        this.successTtlNanos = successTtl.toNanos();
        this.failureTtlNanos = failureTtl.toNanos();
    }

    @Override
    public long expireAfterCreate(Object key, Object value, long currentTime) {
        return isSuccess(value) ? successTtlNanos : failureTtlNanos;
    }

    @Override
    public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
        return expireAfterCreate(key, value, currentTime);
    }

    @Override
    public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
        return currentDuration;
    }

    private boolean isSuccess(Object value) {
        // Spring stores the unwrapped Optional: a ProductDetail on success or a
        // NullValue marker when the product could not be retrieved.
        return value instanceof ProductDetail
                || (value instanceof Optional<?> optional && optional.isPresent());
    }
}
