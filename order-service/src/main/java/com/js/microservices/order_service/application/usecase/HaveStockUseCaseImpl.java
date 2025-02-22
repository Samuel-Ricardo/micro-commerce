package com.js.microservices.order_service.application.usecase;

import com.js.microservices.order_service.domain.dto.usecase.HaveStockDTO;
import com.js.microservices.order_service.domain.gateway.InventoryGateway;
import com.js.microservices.order_service.domain.usecase.HaveStockUseCase;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component @AllArgsConstructor
public class HaveStockUseCaseImpl implements HaveStockUseCase {

    private final InventoryGateway gateway;

    @Override
    public boolean execute(@NotNull HaveStockDTO product) {
        //TODO: Connect with Inventory Microservice to check stock
        return gateway.isInStock(product.getSkuCode().getCode(), product.getQuantity());
    }
}
