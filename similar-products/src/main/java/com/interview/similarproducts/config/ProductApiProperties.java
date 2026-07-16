package com.interview.similarproducts.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product-api")
public record ProductApiProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
