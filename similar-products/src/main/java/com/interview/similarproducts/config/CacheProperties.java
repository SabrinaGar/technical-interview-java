package com.interview.similarproducts.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product-api.cache")
public record CacheProperties(Duration ttl, Duration failureTtl, long maxSize) {
}
