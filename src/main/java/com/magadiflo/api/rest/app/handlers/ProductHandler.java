package com.magadiflo.api.rest.app.handlers;

import com.magadiflo.api.rest.app.models.documents.Product;
import com.magadiflo.api.rest.app.models.services.IProductService;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;

@Component
public class ProductHandler {

    private final IProductService productService;

    public ProductHandler(IProductService productService) {
        this.productService = productService;
    }

    public Mono<ServerResponse> listAllProducts(ServerRequest request) {
        Flux<Product> productFlux = this.productService.findAll();
        return ServerResponse.ok().body(productFlux, Product.class);
    }

    public Mono<ServerResponse> showDetails(ServerRequest request) {
        String id = request.pathVariable("id");
        return this.productService.findById(id)
                .flatMap(productDB -> ServerResponse.ok().bodyValue(productDB))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> createProduct(ServerRequest request) {
        RequestPath requestPath = request.requestPath();
        Mono<Product> productMono = request.bodyToMono(Product.class);
        return productMono
                .flatMap(product -> {
                    if (product.getCreateAt() == null) {
                        product.setCreateAt(LocalDate.now());
                    }
                    return this.productService.saveProduct(product);
                })
                .flatMap(productDB -> ServerResponse
                        .created(URI.create(requestPath.value() + "/" + productDB.getId()))
                        .bodyValue(productDB));
    }

    public Mono<ServerResponse> updateProduct(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Product> productMono = request.bodyToMono(Product.class);
        Mono<Product> productMonoDB = this.productService.findById(id);

        return productMonoDB.zipWith(productMono, (productDB, product) -> {
                    productDB.setName(product.getName());
                    productDB.setPrice(product.getPrice());
                    productDB.setCategory(product.getCategory());
                    return productDB;
                })
                .flatMap(this.productService::saveProduct)
                .flatMap(productDB -> ServerResponse.ok().bodyValue(productDB))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}

