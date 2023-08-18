package com.magadiflo.api.rest.app.models.repositories;

import com.magadiflo.api.rest.app.models.documents.Product;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface IProductRepository extends ReactiveMongoRepository<Product, String> {
    Mono<Product> findByName(String name);

    @Query("{'name' : ?0}")
    Mono<Product> findProduct(String name);
}
