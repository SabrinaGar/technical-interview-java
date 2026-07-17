package com.interview.similarproducts.client;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.interview.similarproducts.config.CacheConfig;
import com.interview.similarproducts.exception.ProductNotFoundException;
import com.interview.similarproducts.exception.UpstreamUnavailableException;
import com.interview.similarproducts.model.ProductDetail;

@Component
public class ProductApiClient {

    private static final Logger log = LoggerFactory.getLogger(ProductApiClient.class);

    private final RestClient restClient;

    public ProductApiClient(RestClient productRestClient) {
        this.restClient = productRestClient;
    }

    /**
     * Without the similar ids there is no answer to give, so upstream failures
     * other than a 404 are propagated for the API to report them.
     */
    @Cacheable(cacheNames = CacheConfig.SIMILAR_IDS_CACHE, sync = true)
    public List<String> getSimilarIds(String productId) {
        try {
            List<String> ids = restClient.get()
                    .uri("/product/{productId}/similarids", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {
                    });
            return ids != null ? ids : List.of();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (RestClientException e) {
            log.warn("Could not fetch similar ids for product {}: {}", productId, e.getMessage());
            throw new UpstreamUnavailableException(productId, e);
        }
    }

    /**
     * Returns empty when the product cannot be retrieved (not found, upstream
     * error or timeout) so the caller can degrade gracefully.
     */
    @Cacheable(cacheNames = CacheConfig.PRODUCTS_CACHE, sync = true)
    public Optional<ProductDetail> getProduct(String productId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/product/{productId}", productId)
                    .retrieve()
                    .body(ProductDetail.class));
        } catch (RestClientException e) {
            log.warn("Could not fetch product {}: {}", productId, e.getMessage());
            return Optional.empty();
        }
    }
}
