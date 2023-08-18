package com.magadiflo.api.rest.app.handlers.config;

import com.magadiflo.api.rest.app.models.documents.Product;
import com.magadiflo.api.rest.app.models.services.IProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RouterFunctionConfigTest {
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private IProductService productService;

    @Test
    void should_list_all_products() {
        WebTestClient.ResponseSpec response = this.webTestClient.get().uri("/api/v2/products")
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        response.expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Product.class)
                .hasSize(14);
    }

    @Test
    void should_list_all_products_with_consumeWith() {
        WebTestClient.ResponseSpec response = this.webTestClient.get().uri("/api/v2/products")
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        response.expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Product.class)
                .consumeWith(listEntityExchangeResult -> {
                    List<Product> products = listEntityExchangeResult.getResponseBody();

                    Assertions.assertNotNull(products);
                    Assertions.assertFalse(products.isEmpty());
                    Assertions.assertEquals(14, products.size());
                });
    }

    @Test
    void should_show_details_of_a_product() {
        Product productDB = this.productService.findByName("Celular Huawey").block();

        WebTestClient.ResponseSpec response = this.webTestClient.get()
                .uri("/api/v2/products/{id}", Collections.singletonMap("id", productDB.getId()))
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        response.expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Celular Huawey");
    }
}