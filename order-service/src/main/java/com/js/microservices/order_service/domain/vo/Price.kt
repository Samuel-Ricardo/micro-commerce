package com.js.microservices.order_service.domain.vo

import java.math.BigDecimal

data class Price(
    val value: BigDecimal
) {
    companion object {
        @JvmStatic
        fun totalPrice(price: BigDecimal, quantity: Int): Price {
            return Price(price.multiply(BigDecimal(quantity)))
        }
    }
}
