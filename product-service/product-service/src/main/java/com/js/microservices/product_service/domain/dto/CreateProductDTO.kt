package com.js.microservices.product_service.domain.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateProductDTO (
    @NotNull
    val id: String,
    @NotBlank
    val name: String,
    @NotBlank
    val description: String,
    @NotBlank
    val skuCode: String,
    @NotNull
    val price: BigDecimal,
)