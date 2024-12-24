package com.js.microservices.order_service.application.error

import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY


data class InsufficientStockException(
    val skuCode: String
) : AppError("Product with SkuCode $skuCode is not in stock", UNPROCESSABLE_ENTITY)