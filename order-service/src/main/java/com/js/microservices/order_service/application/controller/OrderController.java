package com.js.microservices.order_service.application.controller;

import com.js.microservices.order_service.domain.dto.PlaceOrderDTO;
import com.js.microservices.order_service.domain.entity.Order;
import com.js.microservices.order_service.domain.service.OrderService;
import groovy.util.logging.Slf4j;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import static org.springframework.http.HttpStatus.CREATED;

import org.springframework.web.bind.annotation.*;

@RestController() @RequestMapping("/api/v1/orders")
@AllArgsConstructor @Slf4j
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(CREATED)
    public String placeOrder(@RequestBody @Valid PlaceOrderDTO order) {
        Order result = service.placeOrder(order);
        return "Order " + result.getOrderNumber() + " has been placed with success";
    }

}
