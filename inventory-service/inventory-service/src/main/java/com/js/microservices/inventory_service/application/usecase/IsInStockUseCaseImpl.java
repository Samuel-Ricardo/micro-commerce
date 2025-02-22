package com.js.microservices.inventory_service.application.usecase;

import com.js.microservices.inventory_service.domain.dto.IsInStockDTO;
import com.js.microservices.inventory_service.domain.repository.InventoryRepository;
import com.js.microservices.inventory_service.domain.usecase.IsInStockUseCase;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component @AllArgsConstructor
public class IsInStockUseCaseImpl implements IsInStockUseCase {

    private final InventoryRepository repository;

    @Override
    public boolean execute(@Valid IsInStockDTO dto) {
        return repository.existsBySkuCodeAndQuantityIsGreaterThanEqual(
                dto.getSkuCode().getCode(), dto.getQuantity()
        );
    }
}
