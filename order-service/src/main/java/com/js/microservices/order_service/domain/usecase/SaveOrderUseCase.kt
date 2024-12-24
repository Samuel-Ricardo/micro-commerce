package com.js.microservices.order_service.domain.usecase

import com.js.microservices.order_service.domain.entity.Order

interface SaveOrderUseCase {
    fun execute(order: Order): Order
}