package com.magadiflo.api.rest.app.models.services;

import com.magadiflo.api.rest.app.models.documents.Category;
import com.magadiflo.api.rest.app.models.documents.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IProductService {
    Flux<Product> findAll();

    Flux<Product> findAllWithNameUpperCase();

    Flux<Product> findAllWithNameUpperCaseAndRepeat();

    Mono<Product> findById(String id);

    Mono<Product> saveProduct(Product product);

    Mono<Void> delete(Product product);

    Flux<Category> findAllCategories();

    Mono<Category> findCategory(String id);

    Mono<Category> saveCategory(Category category);

    Mono<Product> findByName(String name);
}
