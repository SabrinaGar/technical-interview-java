package com.interview.similarproducts.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.interview.similarproducts.model.ProductDetail;
import com.interview.similarproducts.service.SimilarProductsService;

@RestController
public class SimilarProductsController {

    private final SimilarProductsService similarProductsService;

    public SimilarProductsController(SimilarProductsService similarProductsService) {
        this.similarProductsService = similarProductsService;
    }

    @GetMapping("/product/{productId}/similar")
    public List<ProductDetail> getSimilarProducts(@PathVariable String productId) {
        return similarProductsService.getSimilarProducts(productId);
    }
}
