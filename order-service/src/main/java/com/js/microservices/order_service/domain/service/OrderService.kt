package com.js.microservices.order_service.domain.service

import com.js.microservices.order_service.domain.dto.PlaceOrderDTO

interface OrderService {
    fun placeOrder(order: PlaceOrderDTO)
}