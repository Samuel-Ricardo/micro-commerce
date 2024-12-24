package com.js.microservices.order_service.application.usecase;

import com.js.microservices.order_service.domain.dto.PlaceOrderDTO;
import com.js.microservices.order_service.domain.entity.Order;
import com.js.microservices.order_service.domain.usecase.CreateOrderUseCase;
import com.js.microservices.order_service.domain.vo.Price;
import com.js.microservices.order_service.domain.vo.SkuCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {
    @NotNull
    @Override
    public Order execute(@NotNull PlaceOrderDTO order) {
        return new Order(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                new SkuCode(order.getSkuCode()),
                Price.totalPrice(order.getPrice(), order.getQuantity()),
                order.getQuantity()
        );
    }
}
