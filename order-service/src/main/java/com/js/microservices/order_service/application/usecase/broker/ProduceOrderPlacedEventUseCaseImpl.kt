package com.js.microservices.order_service.application.usecase.broker

import com.js.microservices.order_service.domain.dto.usecase.broker.ProduceOrderPlacedEventDTO
import com.js.microservices.order_service.domain.event.OrderPlacedEvent
import com.js.microservices.order_service.domain.producer.OrderProducer
import com.js.microservices.order_service.domain.usecase.broker.ProduceOrderPlacedEventUseCase
import org.springframework.stereotype.Component

@Component
class ProduceOrderPlacedEventUseCaseImpl(
    private val producer: OrderProducer
) : ProduceOrderPlacedEventUseCase {

    override fun execute(dto: ProduceOrderPlacedEventDTO) {
        val event = OrderPlacedEvent.newBuilder()
            .setOrderNumber(dto.order.orderNumber)
            .setEmail(dto.user.email)
            .setFirstName(dto.user.firstName)
            .setLastName(dto.user.lastName)
            .build()

        producer.produceOrderPlacedEvent(event)
    }
}