package com.js.microservices.product_service.application.usecase;

import static com.js.microservices.product_service.application.factory.product.ProductFactory.newProductEntityList;
import com.js.microservices.product_service.domain.entity.Product;
import com.js.microservices.product_service.domain.repository.ProductRepository;
import com.js.microservices.product_service.domain.usecase.GetAllProductsUseCase;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component @AllArgsConstructor
public class GetAllProductsUseCaseImpl implements GetAllProductsUseCase {

    private final ProductRepository repository;

    @NotNull
    @Override
    public List<Product> execute() {
        return newProductEntityList(repository.findAll());
    }

}
