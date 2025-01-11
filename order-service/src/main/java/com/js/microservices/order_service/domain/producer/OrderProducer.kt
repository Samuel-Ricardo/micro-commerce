package com.js.microservices.order_service.domain.producer

import com.js.microservices.order_service.domain.event.OrderPlacedEvent

interface OrderProducer {
    fun produceOrderPlacedEvent(order: OrderPlacedEvent)
}