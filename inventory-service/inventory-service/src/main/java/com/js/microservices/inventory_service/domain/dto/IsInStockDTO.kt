package com.js.microservices.inventory_service.domain.dto

import com.js.microservices.inventory_service.domain.vo.SkuCode
import jakarta.validation.constraints.NotNull


data class IsInStockDTO(
    @NotNull
    val skuCode: SkuCode,
    @NotNull
    val quantity: Int
)
