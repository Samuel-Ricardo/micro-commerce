package com.js.microservices.order_service.domain.usecase

import com.js.microservices.order_service.domain.dto.usecase.HaveStockDTO

interface HaveStockUseCase {
    fun execute(product: HaveStockDTO): Boolean
}