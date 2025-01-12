package com.js.microservices.notification.domain.gateway.mail

import com.js.microservices.notification.application.error.mail.SendMailError
import com.js.microservices.notification.domain.dto.SendMailDTO
import kotlin.Throws

interface MailGateway {
    /**
     * Sends an email.
     *
     * @throws SendMailError if there is an error sending the email
     */
    @Throws(SendMailError::class)
    fun send(mail: SendMailDTO)
}