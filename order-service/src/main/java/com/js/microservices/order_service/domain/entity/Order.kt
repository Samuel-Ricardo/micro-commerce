package com.js.microservices.order_service.domain.entity

import com.js.microservices.order_service.domain.vo.Price
import com.js.microservices.order_service.domain.vo.SkuCode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

class Order (
    val id: UUID,
    @NotBlank
    val orderNumber: String,
    @NotNull
    val skuCode: SkuCode,
    @NotNull
    val price: Price,
    @NotNull
    val quantity: Int
)