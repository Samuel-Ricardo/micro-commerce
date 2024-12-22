package com.js.microservices.product_service.domain.service;

import com.js.microservices.product_service.domain.dto.CreateProductDTO;
import com.js.microservices.product_service.domain.entity.Product;

import java.util.List;

public interface ProductService {

    Product create(CreateProductDTO dto);

    List<Product> getAll();
}
