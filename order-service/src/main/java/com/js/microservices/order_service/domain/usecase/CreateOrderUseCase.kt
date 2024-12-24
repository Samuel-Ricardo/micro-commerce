package com.js.microservices.order_service.domain.usecase

import com.js.microservices.order_service.domain.dto.PlaceOrderDTO
import com.js.microservices.order_service.domain.entity.Order

interface CreateOrderUseCase {
    fun execute(order: PlaceOrderDTO): Order
}