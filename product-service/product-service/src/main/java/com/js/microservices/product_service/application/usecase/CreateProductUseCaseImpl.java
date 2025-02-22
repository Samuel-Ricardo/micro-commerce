package com.js.microservices.product_service.application.usecase;

import com.js.microservices.product_service.domain.dto.CreateProductDTO;
import com.js.microservices.product_service.domain.entity.Product;
import com.js.microservices.product_service.domain.model.ProductModel;
import com.js.microservices.product_service.domain.repository.ProductRepository;
import com.js.microservices.product_service.domain.usecase.CreateProductUseCase;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.js.microservices.product_service.application.factory.product.ProductFactory.newProductEntity;
import static com.js.microservices.product_service.application.factory.product.ProductFactory.newProductModel;


@Component @AllArgsConstructor
public class CreateProductUseCaseImpl implements CreateProductUseCase {

    private final ProductRepository repository;

    @NotNull
    @Override
    public Product execute(@NotNull CreateProductDTO product) {
        return newProductEntity(repository.save(newProductModel(product)));
    }
}
