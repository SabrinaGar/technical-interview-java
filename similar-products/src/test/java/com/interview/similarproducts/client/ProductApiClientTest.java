package com.interview.similarproducts.client;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.interview.similarproducts.exception.ProductNotFoundException;
import com.interview.similarproducts.exception.UpstreamUnavailableException;
import com.interview.similarproducts.model.ProductDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProductApiClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://product-api");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ProductApiClient client = new ProductApiClient(builder.build());

    @Test
    void returnsTheSimilarIdsOfAProduct() {
        server.expect(requestTo("http://product-api/product/1/similarids"))
                .andRespond(withSuccess("[\"2\", \"3\"]", MediaType.APPLICATION_JSON));

        assertThat(client.getSimilarIds("1")).containsExactly("2", "3");
    }

    @Test
    void mapsANotFoundFetchingSimilarIdsToProductNotFound() {
        server.expect(requestTo("http://product-api/product/99/similarids"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getSimilarIds("99"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void mapsAnUpstreamErrorFetchingSimilarIdsToUpstreamUnavailable() {
        server.expect(requestTo("http://product-api/product/1/similarids"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getSimilarIds("1"))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void mapsATimeoutFetchingSimilarIdsToUpstreamUnavailable() {
        server.expect(requestTo("http://product-api/product/1/similarids"))
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.getSimilarIds("1"))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void returnsTheDetailOfAProduct() {
        server.expect(requestTo("http://product-api/product/2"))
                .andRespond(withSuccess(
                        "{\"id\": \"2\", \"name\": \"Dress\", \"price\": 19.99, \"availability\": true}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.getProduct("2"))
                .contains(new ProductDetail("2", "Dress", new BigDecimal("19.99"), true));
    }

    @Test
    void returnsEmptyWhenTheProductDoesNotExist() {
        server.expect(requestTo("http://product-api/product/404"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.getProduct("404")).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheUpstreamFailsFetchingAProduct() {
        server.expect(requestTo("http://product-api/product/2"))
                .andRespond(withServerError());

        assertThat(client.getProduct("2")).isEmpty();
    }
}
