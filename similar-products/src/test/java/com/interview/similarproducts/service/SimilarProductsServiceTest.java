package com.interview.similarproducts.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.interview.similarproducts.client.ProductApiClient;
import com.interview.similarproducts.exception.ProductNotFoundException;
import com.interview.similarproducts.model.ProductDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarProductsServiceTest {

    @Mock
    private ProductApiClient productApiClient;

    @InjectMocks
    private SimilarProductsService similarProductsService;

    private final ProductDetail shirt = new ProductDetail("2", "Shirt", new BigDecimal("9.99"), true);
    private final ProductDetail dress = new ProductDetail("3", "Dress", new BigDecimal("19.99"), false);

    @Test
    void returnsSimilarProductDetailsKeepingSimilarityOrder() {
        when(productApiClient.getSimilarIds("1")).thenReturn(List.of("2", "3"));
        when(productApiClient.getProduct("2")).thenReturn(Optional.of(shirt));
        when(productApiClient.getProduct("3")).thenReturn(Optional.of(dress));

        List<ProductDetail> result = similarProductsService.getSimilarProducts("1");

        assertThat(result).containsExactly(shirt, dress);
    }

    @Test
    void omitsProductsThatCannotBeRetrieved() {
        when(productApiClient.getSimilarIds("1")).thenReturn(List.of("2", "404", "3"));
        when(productApiClient.getProduct("2")).thenReturn(Optional.of(shirt));
        when(productApiClient.getProduct("404")).thenReturn(Optional.empty());
        when(productApiClient.getProduct("3")).thenReturn(Optional.of(dress));

        List<ProductDetail> result = similarProductsService.getSimilarProducts("1");

        assertThat(result).containsExactly(shirt, dress);
    }

    @Test
    void returnsEmptyListWhenThereAreNoSimilarProducts() {
        when(productApiClient.getSimilarIds("1")).thenReturn(List.of());

        assertThat(similarProductsService.getSimilarProducts("1")).isEmpty();
    }

    @Test
    void propagatesNotFoundWhenTheGivenProductDoesNotExist() {
        when(productApiClient.getSimilarIds("99")).thenThrow(new ProductNotFoundException("99"));

        assertThatThrownBy(() -> similarProductsService.getSimilarProducts("99"))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
