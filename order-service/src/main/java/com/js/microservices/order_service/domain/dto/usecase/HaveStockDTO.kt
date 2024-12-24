package com.js.microservices.order_service.domain.dto.usecase

import com.js.microservices.order_service.domain.vo.SkuCode

data class HaveStockDTO(
    val skuCode: SkuCode,
    val quantity: Int
)
