package com.magadiflo.api.rest.app.controllers;

import com.magadiflo.api.rest.app.models.documents.Product;
import com.magadiflo.api.rest.app.models.services.IProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {
    private final IProductService productService;

    @Value("${config.uploads.path}")
    private String uploadsPath;

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

    @DeleteMapping(path = "/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return this.productService.findById(id)
                .flatMap(productDB -> this.productService.delete(productDB).then(Mono.just(true)))
                .map(isDeleted -> new ResponseEntity<Void>(HttpStatus.NO_CONTENT))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/upload/{id}")
    public Mono<ResponseEntity<Product>> uploadImage(@PathVariable String id, @RequestPart FilePart imageFile) {
        return this.productService.findById(id)
                .flatMap(productDB -> {
                    String imageName = UUID.randomUUID().toString() + "-" + imageFile.filename()
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", "");
                    productDB.setImage(imageName);

                    return imageFile.transferTo(new File(this.uploadsPath + productDB.getImage()))
                            .then(this.productService.saveProduct(productDB));
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/product-with-image")
    public Mono<ResponseEntity<Product>> createProductWithImage(Product product, @RequestPart FilePart imageFile) {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDate.now());
        }

        String imageName = UUID.randomUUID().toString() + "-" + imageFile.filename()
                .replace(" ", "")
                .replace(":", "")
                .replace("\\", "");
        product.setImage(imageName);

        return imageFile.transferTo(new File(this.uploadsPath + product.getImage()))
                .then(this.productService.saveProduct(product)
                        .map(productDB -> ResponseEntity
                                .created(URI.create("/api/v1/products/" + productDB.getId()))
                                .body(productDB))
                );
    }

}
