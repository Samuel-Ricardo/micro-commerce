package com.js.microservices.order_service.domain.service

import com.js.microservices.order_service.domain.dto.PlaceOrderDTO
import com.js.microservices.order_service.domain.entity.Order

interface OrderService {
    fun placeOrder(order: PlaceOrderDTO): Order
}