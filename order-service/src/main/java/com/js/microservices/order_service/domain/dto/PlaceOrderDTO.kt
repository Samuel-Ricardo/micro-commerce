package com.js.microservices.order_service.domain.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class PlaceOrderDTO(
    @NotBlank
    val skuCode: String,
    @NotNull
    val price: BigDecimal,
    @NotNull
    val quantity: Int,
    @NotNull
    val userDetails: UserDetailsDTO
)
