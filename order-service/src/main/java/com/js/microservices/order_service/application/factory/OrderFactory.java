package com.js.microservices.order_service.application.factory;

import com.js.microservices.order_service.domain.entity.Order;
import com.js.microservices.order_service.domain.model.OrderModel;
import com.js.microservices.order_service.domain.vo.Price;
import com.js.microservices.order_service.domain.vo.SkuCode;

public class OrderFactory {

    public static OrderModel newOrderModel(Order order) {
        return OrderModel.builder()
                .orderNumber(order.getOrderNumber())
                .skuCode(order.getSkuCode().getCode())
                .price(order.getPrice().getValue())
                .quantity(order.getQuantity())
                .build();
    }

    public static Order newOrderEntity(OrderModel order) {
        return new Order(
                order.getId(),
                order.getOrderNumber(),
                new SkuCode(order.getSkuCode()),
                new Price(order.getPrice()),
                order.getQuantity()
        );
    }

}
