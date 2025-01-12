package com.js.microservices.notification.application.error.mail

import com.js.microservices.notification.application.error.SendNotificationError

class SendMailError(
 val mail: String
): SendNotificationError(mail)
