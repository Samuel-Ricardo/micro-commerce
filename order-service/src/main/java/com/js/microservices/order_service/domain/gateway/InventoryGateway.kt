package com.js.microservices.order_service.domain.gateway

import com.js.microservices.order_service.domain.dto.usecase.HaveStockDTO
import com.js.microservices.order_service.domain.vo.SkuCode
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(value = "inventory", url = "\${inventory.service.url}")
interface InventoryGateway {
    @RequestMapping(method = [RequestMethod.GET], value = ["/api/v1/inventory"])
    fun isInStock(@RequestParam skuCode: String, @RequestParam quantity: Int): Boolean
}