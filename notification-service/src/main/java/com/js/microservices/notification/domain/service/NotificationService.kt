package com.js.microservices.notification.domain.service

import com.js.microservices.notification.application.error.SendNotificationError
import com.js.microservices.notification.domain.dto.SendOrderPlacedMailNotificationDTO

interface NotificationService {
    @Throws(SendNotificationError::class)
    fun sendOrderPlaced(notification: SendOrderPlacedMailNotificationDTO)
}