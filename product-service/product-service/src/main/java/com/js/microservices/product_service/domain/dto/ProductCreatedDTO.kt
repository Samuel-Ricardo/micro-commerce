package com.js.microservices.product_service.domain.dto

import java.math.BigDecimal

data class ProductCreatedDTO(
    val id: String,
    val name: String,
    val description: String,
    val skuCode: String,
    val price: BigDecimal
)
