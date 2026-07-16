package com.interview.similarproducts.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import com.interview.similarproducts.client.ProductApiClient;
import com.interview.similarproducts.model.ProductDetail;

@Service
public class SimilarProductsService {

    private final ProductApiClient productApiClient;

    public SimilarProductsService(ProductApiClient productApiClient) {
        this.productApiClient = productApiClient;
    }

    /**
     * Returns the details of the products similar to the given one, keeping the
     * similarity order. Details are fetched concurrently and products that
     * cannot be retrieved are omitted from the result.
     */
    public List<ProductDetail> getSimilarProducts(String productId) {
        List<String> similarIds = productApiClient.getSimilarIds(productId);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Optional<ProductDetail>>> futures = similarIds.stream()
                    .map(id -> executor.submit(() -> productApiClient.getProduct(id)))
                    .toList();
            return futures.stream()
                    .map(this::getResult)
                    .flatMap(Optional::stream)
                    .toList();
        }
    }

    private Optional<ProductDetail> getResult(Future<Optional<ProductDetail>> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }
}
