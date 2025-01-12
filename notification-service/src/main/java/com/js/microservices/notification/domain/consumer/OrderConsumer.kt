package com.js.microservices.notification.domain.consumer

import com.js.microservices.order.event.OrderPlacedEvent


interface OrderConsumer {
    fun listenOrderPlacedEvent(event: OrderPlacedEvent)
}