package com.magadiflo.api.rest.app.models.repositories;

import com.magadiflo.api.rest.app.models.documents.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface IProductRepository extends ReactiveMongoRepository<Product, String> {
}
