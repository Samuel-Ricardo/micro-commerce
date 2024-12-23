package com.js.microservices.product_service.application.factory.product;

import com.js.microservices.product_service.domain.dto.CreateProductDTO;
import com.js.microservices.product_service.domain.dto.ProductCreatedDTO;
import com.js.microservices.product_service.domain.entity.Product;
import com.js.microservices.product_service.domain.model.ProductModel;
import com.js.microservices.product_service.domain.vo.SkuCode;


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

    public static ProductModel newProductModel(CreateProductDTO dto) {
        return ProductModel
                .builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .skuCode(dto.getSkuCode())
                .price(dto.getPrice())
                .build();
    }

    public static Product newProductEntity(ProductModel productModel) {
        return new Product(
            productModel.getId(),
            productModel.getName(),
            productModel.getDescription(),
            new SkuCode(productModel.getSkuCode()),
            productModel.getPrice()
        );
    }

    public static List<Product> newProductEntityList(List<ProductModel> models) {
        return models.stream().map(ProductFactory::newProductEntity).toList();
    }
}