package com.js.microservices.product_service.application.service;

import com.js.microservices.product_service.domain.dto.CreateProductDTO;
import com.js.microservices.product_service.domain.entity.Product;
import com.js.microservices.product_service.domain.service.ProductService;
import com.js.microservices.product_service.domain.usecase.CreateProductUseCase;
import com.js.microservices.product_service.domain.usecase.GetAllProductsUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;


@Service @Slf4j
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {


    private final CreateProductUseCase createProduct;
    private final GetAllProductsUseCase getAllProducts;

    @Override
    public Product create(CreateProductDTO dto) {
        log.info("Creating product: {}", dto);
        return createProduct.execute(dto);
    }

    @Override
    public List<Product> getAll() {
        log.info("Getting all products");
        return getAllProducts.execute();
    }
}
