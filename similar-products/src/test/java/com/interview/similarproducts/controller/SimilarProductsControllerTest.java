package com.interview.similarproducts.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.interview.similarproducts.exception.ProductNotFoundException;
import com.interview.similarproducts.exception.UpstreamUnavailableException;
import com.interview.similarproducts.model.ProductDetail;
import com.interview.similarproducts.service.SimilarProductsService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimilarProductsController.class)
class SimilarProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimilarProductsService similarProductsService;

    @Test
    void returnsSimilarProductsAsJson() throws Exception {
        when(similarProductsService.getSimilarProducts("1")).thenReturn(List.of(
                new ProductDetail("2", "Dress", new BigDecimal("19.99"), true)));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("2"))
                .andExpect(jsonPath("$[0].name").value("Dress"))
                .andExpect(jsonPath("$[0].price").value(19.99))
                .andExpect(jsonPath("$[0].availability").value(true));
    }

    @Test
    void returnsEmptyArrayWhenThereAreNoSimilarProducts() throws Exception {
        when(similarProductsService.getSimilarProducts("1")).thenReturn(List.of());

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void returnsNotFoundWhenTheProductDoesNotExist() throws Exception {
        when(similarProductsService.getSimilarProducts("99")).thenThrow(new ProductNotFoundException("99"));

        mockMvc.perform(get("/product/99/similar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found: 99"));
    }

    @Test
    void returnsBadGatewayWhenTheUpstreamApiFails() throws Exception {
        when(similarProductsService.getSimilarProducts("1"))
                .thenThrow(new UpstreamUnavailableException("1", new RuntimeException("boom")));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Could not retrieve similar products for product: 1"));
    }
}
