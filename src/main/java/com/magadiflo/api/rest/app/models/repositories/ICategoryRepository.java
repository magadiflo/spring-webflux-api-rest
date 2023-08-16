package com.magadiflo.api.rest.app.models.repositories;

import com.magadiflo.api.rest.app.models.documents.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ICategoryRepository extends ReactiveMongoRepository<Category, String> {
}
