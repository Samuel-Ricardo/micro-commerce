package com.js.microservices.product_service.domain.usecase

import com.js.microservices.product_service.domain.entity.Product

interface GetAllProductsUseCase {
    fun execute(): List<Product>
}