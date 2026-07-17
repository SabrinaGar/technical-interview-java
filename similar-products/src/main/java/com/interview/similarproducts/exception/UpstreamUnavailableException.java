package com.interview.similarproducts.exception;

/**
 * Thrown when the upstream product API fails on a call the service cannot
 * degrade gracefully from, such as retrieving the similar ids.
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String productId, Throwable cause) {
        super("Could not retrieve similar products for product: " + productId, cause);
    }
}
