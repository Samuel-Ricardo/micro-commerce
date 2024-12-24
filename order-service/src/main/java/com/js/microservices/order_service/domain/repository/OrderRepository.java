package com.js.microservices.order_service.domain.repository;

import com.js.microservices.order_service.domain.model.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrderRepository extends JpaRepository<OrderModel, String> {}
