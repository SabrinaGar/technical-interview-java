package com.interview.similarproducts.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS_CACHE = "products";
    public static final String SIMILAR_IDS_CACHE = "similar-ids";

    @Bean
    public CacheManager cacheManager(CacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache(SIMILAR_IDS_CACHE, Caffeine.newBuilder()
                .expireAfterWrite(properties.ttl())
                .maximumSize(properties.maxSize())
                .build());
        cacheManager.registerCustomCache(PRODUCTS_CACHE, Caffeine.newBuilder()
                .expireAfter(new ProductExpiry(properties.ttl(), properties.failureTtl()))
                .maximumSize(properties.maxSize())
                .build());
        return cacheManager;
    }
}
