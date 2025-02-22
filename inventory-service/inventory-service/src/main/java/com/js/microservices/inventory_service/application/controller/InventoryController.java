package com.js.microservices.inventory_service.application.controller;

import com.js.microservices.inventory_service.domain.dto.IsInStockDTO;
import com.js.microservices.inventory_service.domain.service.InventoryService;
import com.js.microservices.inventory_service.domain.vo.SkuCode;
import lombok.AllArgsConstructor;
import static org.springframework.http.HttpStatus.OK;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@AllArgsConstructor
public class InventoryController {

    private final InventoryService service;

    @GetMapping
    @ResponseStatus(OK)
    public boolean isInStock(@RequestParam String skuCode, @RequestParam Integer quantity) {
        return service.isInStock(
                new IsInStockDTO(new SkuCode(skuCode), quantity)
        );
    }

}
