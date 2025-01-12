package com.js.microservices.notification.application.error

import org.springframework.http.HttpStatus

open class SendNotificationError(
 val to: String
): AppError("Error on send notification to $to", HttpStatus.INTERNAL_SERVER_ERROR)
