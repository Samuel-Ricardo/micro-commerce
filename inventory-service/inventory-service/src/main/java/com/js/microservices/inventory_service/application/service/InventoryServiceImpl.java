package com.js.microservices.inventory_service.application.service;

import com.js.microservices.inventory_service.domain.dto.IsInStockDTO;
import com.js.microservices.inventory_service.domain.service.InventoryService;
import com.js.microservices.inventory_service.domain.usecase.IsInStockUseCase;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service @Slf4j
@AllArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final IsInStockUseCase isInStockUseCase;

    public boolean isInStock(@Valid IsInStockDTO isInStockDTO) {
        return isInStockUseCase.execute(isInStockDTO);
    }

}
