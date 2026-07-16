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
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(PRODUCTS_CACHE, SIMILAR_IDS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.ttl())
                .maximumSize(properties.maxSize()));
        return cacheManager;
    }
}
