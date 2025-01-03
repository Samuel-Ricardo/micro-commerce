package com.js.microservices.product_service.domain.entity

import com.js.microservices.product_service.domain.vo.SkuCode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class Product (
    @NotNull
     val id: String,
    @NotBlank
     val name: String,
    @NotBlank
     val description: String,
    @NotNull
     val skuCode: SkuCode,
    @NotNull
     val price: BigDecimal,
) {}