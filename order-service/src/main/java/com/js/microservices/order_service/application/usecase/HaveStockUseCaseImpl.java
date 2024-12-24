package com.js.microservices.order_service.application.usecase;

import com.js.microservices.order_service.domain.dto.usecase.HaveStockDTO;
import com.js.microservices.order_service.domain.usecase.HaveStockUseCase;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class HaveStockUseCaseImpl implements HaveStockUseCase {

    @Override
    public boolean execute(@NotNull HaveStockDTO product) {
        //TODO: Connect with Inventory Microservice to check stock
        return true;
    }
}
