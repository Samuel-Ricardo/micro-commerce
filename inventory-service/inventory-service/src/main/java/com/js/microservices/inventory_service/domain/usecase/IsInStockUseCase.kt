package com.js.microservices.inventory_service.domain.usecase

import com.js.microservices.inventory_service.domain.dto.IsInStockDTO

interface IsInStockUseCase {
    fun execute(dto: IsInStockDTO): Boolean
}