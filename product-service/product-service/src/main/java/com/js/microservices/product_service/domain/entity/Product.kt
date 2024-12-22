package com.js.microservices.product_service.domain.entity

import com.js.microservices.product_service.domain.vo.SkuCode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class Product (
    @NotNull
    private val id: String,
    @NotBlank
    private val name: String,
    @NotBlank
    private val description: String,
    @NotNull
    private val skuCode: SkuCode,
    @NotNull
    private val price: BigDecimal,
) {}