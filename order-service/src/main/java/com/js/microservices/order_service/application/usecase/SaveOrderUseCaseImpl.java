package com.js.microservices.order_service.application.usecase;

import static com.js.microservices.order_service.application.factory.OrderFactory.*;
import com.js.microservices.order_service.domain.entity.Order;
import com.js.microservices.order_service.domain.repository.OrderRepository;
import com.js.microservices.order_service.domain.usecase.SaveOrderUseCase;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component @AllArgsConstructor
public class SaveOrderUseCaseImpl implements SaveOrderUseCase {

    private final OrderRepository repository;

    @NotNull
    @Override
    public Order execute(@NotNull Order order) {
        return newOrderEntity(repository
                .save(newOrderModel(order)));
    }
}
