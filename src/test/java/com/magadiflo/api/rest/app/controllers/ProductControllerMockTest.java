package com.magadiflo.api.rest.app.controllers;

import com.magadiflo.api.rest.app.models.documents.Category;
import com.magadiflo.api.rest.app.models.documents.Product;
import com.magadiflo.api.rest.app.models.services.IProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ProductControllerMockTest {
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private IProductService productService;

    @Test
    void should_list_all_products() {
        WebTestClient.ResponseSpec response = this.webTestClient.get().uri("/api/v1/products")
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        response.expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Product.class)
                .hasSize(14);
    }

    @Test
    void should_list_all_products_with_consumeWith() {
        WebTestClient.ResponseSpec response = this.webTestClient.get().uri("/api/v1/products")
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
                .uri("/api/v1/products/{id}", Collections.singletonMap("id", productDB.getId()))
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        response.expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Celular Huawey");
    }

    @Test
    void should_create_a_product() {
        Category categoryDB = this.productService.findCategoryByName("Muebles").block();
        Product product = new Product("Escoba", 25.70, categoryDB);

        WebTestClient.ResponseSpec response = this.webTestClient.post()
                .uri("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)        //<-- Request
                .accept(MediaType.APPLICATION_JSON)    //<-- Response
                .bodyValue(product)
                .exchange();

        response.expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Product.class)
                .consumeWith(productEntityExchangeResult -> {
                    Product productTest = productEntityExchangeResult.getResponseBody();

                    Assertions.assertNotNull(productTest);
                    Assertions.assertEquals(product.getName(), productTest.getName());
                    Assertions.assertEquals(product.getPrice(), productTest.getPrice());
                    Assertions.assertNotNull(product.getCategory());
                    Assertions.assertEquals(product.getCategory().getId(), productTest.getCategory().getId());
                    Assertions.assertEquals(product.getCategory().getName(), productTest.getCategory().getName());
                });
    }

    @Test
    void should_update_a_product() {
        Product productToUpdateDB = this.productService.findByName("Celular Huawey").block();
        Category categoryDB = this.productService.findCategoryByName("Muebles").block();

        Product productRequest = new Product("Sillón 3 cuerpos", 1600.00, categoryDB);

        WebTestClient.ResponseSpec response = this.webTestClient.put()
                .uri("/api/v1/products/{id}", Collections.singletonMap("id", productToUpdateDB.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(productRequest)
                .exchange();

        response.expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Product.class)
                .consumeWith(productEntityExchangeResult -> {
                    Product productTest = productEntityExchangeResult.getResponseBody();

                    Assertions.assertNotNull(productTest);
                    Assertions.assertEquals(productRequest.getName(), productTest.getName());
                    Assertions.assertEquals(productRequest.getPrice(), productTest.getPrice());
                    Assertions.assertNotNull(productRequest.getCategory());
                    Assertions.assertEquals(productRequest.getCategory().getId(), productTest.getCategory().getId());
                    Assertions.assertEquals(productRequest.getCategory().getName(), productTest.getCategory().getName());
                });
    }

    @Test
    void should_delete_a_product() {
        Product productDB = this.productService.findByName("Silla de oficina").block();
        WebTestClient.ResponseSpec response = this.webTestClient.delete()
                .uri("/api/v1/products/{id}", Collections.singletonMap("id", productDB.getId()))
                .exchange();

        response.expectStatus().isNoContent()
                .expectBody()
                .isEmpty();
    }
}