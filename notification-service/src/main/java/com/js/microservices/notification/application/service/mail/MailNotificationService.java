package com.js.microservices.notification.application.service.mail;

import com.js.microservices.notification.application.error.SendNotificationError;
import com.js.microservices.notification.domain.dto.SendOrderPlacedMailNotificationDTO;
import com.js.microservices.notification.domain.service.NotificationService;
import com.js.microservices.notification.domain.usecase.SendOrderPlacedMailNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class MailNotificationService implements NotificationService {

    private final SendOrderPlacedMailNotificationUseCase sendNotification;

    @Override
    public void sendOrderPlaced(@NotNull SendOrderPlacedMailNotificationDTO notification) throws SendNotificationError {
        sendNotification.execute(notification);
    }
}
