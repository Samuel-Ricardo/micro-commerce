package com.js.microservices.order_service.domain.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserDetailsDTO(
    @NotBlank
    @Email
    val email: String,
    @NotBlank
    val firstName: String,
    @NotBlank
    val lastName: String
)
