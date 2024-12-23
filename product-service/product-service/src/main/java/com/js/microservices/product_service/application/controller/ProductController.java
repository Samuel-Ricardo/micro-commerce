package com.js.microservices.product_service.application.controller;

import com.js.microservices.product_service.domain.dto.CreateProductDTO;
import com.js.microservices.product_service.domain.dto.ProductCreatedDTO;
import com.js.microservices.product_service.domain.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.js.microservices.product_service.application.factory.product.ProductFactory.toCreatedDTO;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    @ResponseStatus(CREATED)
    public ProductCreatedDTO createProduct(@Valid @RequestBody CreateProductDTO dto) {
        return toCreatedDTO(service.create(dto));
    }

    @GetMapping
    @ResponseStatus(OK)
    public List<ProductCreatedDTO> getAllProducts() {
        return toCreatedDTO(service.getAll());
    }
}