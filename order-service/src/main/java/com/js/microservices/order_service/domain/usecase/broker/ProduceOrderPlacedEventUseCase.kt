package com.js.microservices.order_service.domain.usecase.broker

import com.js.microservices.order_service.domain.dto.usecase.broker.ProduceOrderPlacedEventDTO
import com.js.microservices.order_service.domain.entity.Order

interface ProduceOrderPlacedEventUseCase {
    fun execute(dto: ProduceOrderPlacedEventDTO)
}