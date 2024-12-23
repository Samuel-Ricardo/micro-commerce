package com.js.microservices.product_service.domain.repository;

import com.js.microservices.product_service.domain.model.ProductModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<ProductModel, String> {}
