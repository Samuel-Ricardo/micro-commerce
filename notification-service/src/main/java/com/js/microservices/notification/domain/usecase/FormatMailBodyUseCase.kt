package com.js.microservices.notification.domain.usecase

import com.js.microservices.notification.domain.dto.FormatMailBodyDTO

interface FormatMailBodyUseCase {
    fun execute(mail: FormatMailBodyDTO): String
}