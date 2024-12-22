package com.js.microservices.product_service.application.factory.product;

import com.js.microservices.product_service.domain.dto.ProductCreatedDTO;
import com.js.microservices.product_service.domain.entity.Product;

import java.util.List;

public class ProductFactory {

    public static ProductCreatedDTO toCreatedDTO(Product product) {
        return new ProductCreatedDTO(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getSkuCode().getValue(),
            product.getPrice()
        );
    }

    public static List<ProductCreatedDTO> toCreatedDTO(List<Product> products) {
        return products
                .stream()
                .map(ProductFactory::toCreatedDTO)
                .toList();
    }
}