package com.js.microservices.order_service.application.service;

import com.js.microservices.order_service.application.error.InsufficientStockException;
import com.js.microservices.order_service.domain.dto.PlaceOrderDTO;
import com.js.microservices.order_service.domain.dto.usecase.HaveStockDTO;
import com.js.microservices.order_service.domain.dto.usecase.broker.ProduceOrderPlacedEventDTO;
import com.js.microservices.order_service.domain.entity.Order;
import com.js.microservices.order_service.domain.service.OrderService;
import com.js.microservices.order_service.domain.usecase.CreateOrderUseCase;
import com.js.microservices.order_service.domain.usecase.HaveStockUseCase;
import com.js.microservices.order_service.domain.usecase.SaveOrderUseCase;
import com.js.microservices.order_service.domain.usecase.broker.ProduceOrderPlacedEventUseCase;
import com.js.microservices.order_service.domain.vo.SkuCode;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;


@Service @AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final HaveStockUseCase haveStock;
    private final CreateOrderUseCase createOrder;
    private final SaveOrderUseCase saveOrder;
    private final ProduceOrderPlacedEventUseCase produceOrderPlacedEvent;


    @NotNull
    @Override
    public Order placeOrder(@NotNull PlaceOrderDTO order) {
        var isAvailable = haveStock.execute(new HaveStockDTO(new SkuCode(order.getSkuCode()), order.getQuantity()));
        if (!isAvailable) throw new InsufficientStockException(order.getSkuCode());


        var orderPlaced = saveOrder.execute(createOrder.execute(order));

        produceOrderPlacedEvent.execute(ProduceOrderPlacedEventDTO
                .builder()
                .order(orderPlaced)
                .user(order.getUserDetails())
                .build()
        );

        return orderPlaced;
    }
}
