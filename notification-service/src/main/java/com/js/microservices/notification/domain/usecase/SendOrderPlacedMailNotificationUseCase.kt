package com.js.microservices.notification.domain.usecase

import com.js.microservices.notification.application.error.mail.SendMailError
import com.js.microservices.notification.domain.dto.SendOrderPlacedMailNotificationDTO
import kotlin.jvm.Throws

interface SendOrderPlacedMailNotificationUseCase {

    @Throws(SendMailError::class)
    fun execute(notification: SendOrderPlacedMailNotificationDTO)
}