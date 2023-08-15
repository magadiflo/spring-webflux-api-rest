package com.magadiflo.api.rest.app.controllers;

import com.magadiflo.api.rest.app.models.documents.Product;
import com.magadiflo.api.rest.app.models.services.IProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    private final IProductService productService;

    public ProductController(IProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<Product>>> getAllProducts() {
        return Mono.just(ResponseEntity.ok(this.productService.findAll()));
    }

    @GetMapping(path = "/{id}")
    public Mono<ResponseEntity<Product>> getProduct(@PathVariable String id) {
        return this.productService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Product>> createProduct(@RequestBody Product product) {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDate.now());
        }
        return this.productService.saveProduct(product)
                .map(productDB -> ResponseEntity
                        .created(URI.create("/api/v1/products/" + productDB.getId()))
                        .body(productDB));
    }

    @PutMapping(path = "/{id}")
    public Mono<ResponseEntity<Product>> updateProduct(@PathVariable String id, @RequestBody Product product) {
        return this.productService.findById(id)
                .flatMap(productDB -> {
                    productDB.setName(product.getName());
                    productDB.setPrice(product.getPrice());
                    productDB.setCategory(product.getCategory());
                    return this.productService.saveProduct(productDB);
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
