package com.js.microservices.order_service.domain.gateway

import groovy.util.logging.Slf4j
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.Logger;
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

@Slf4j
interface InventoryGateway {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(InventoryGateway::class.java)
    }

    @GetExchange("/api/v1/inventory")
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @Retry(name = "inventory")
    fun isInStock(@RequestParam skuCode: String, @RequestParam quantity: Int): Boolean

    fun fallbackMethod(skuCode: String, quantity: Int, throwable: Throwable): Boolean {
        log.info("Cannot get inventory for skucode {}, failure reason: {}", skuCode, throwable.message)
        return false
    }
}