package com.js.microservices.notification.application.usecase;

import com.js.microservices.notification.application.error.mail.SendMailError;
import com.js.microservices.notification.domain.dto.FormatMailBodyDTO;
import com.js.microservices.notification.domain.dto.SendMailDTO;
import com.js.microservices.notification.domain.dto.SendOrderPlacedMailNotificationDTO;
import com.js.microservices.notification.domain.gateway.mail.MailGateway;
import com.js.microservices.notification.domain.usecase.FormatMailBodyUseCase;
import com.js.microservices.notification.domain.usecase.SendOrderPlacedMailNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class SendOrderPlacedMailNotificationUseCaseImpl implements SendOrderPlacedMailNotificationUseCase {

    private final MailGateway gateway;
    private final FormatMailBodyUseCase formatter;

    @Override
    public void execute(@NotNull SendOrderPlacedMailNotificationDTO notification) throws SendMailError {
        
        var body = FormatMailBodyDTO.builder()
                .orderNumber(notification.getOrderNumber())
                .firstName(notification.getFirstName())
                .lastName(notification.getLastName())
                .build();
        
        var mail = SendMailDTO.builder()
                .to(notification.getTo())
                .subject(String.format("Your Order of number %s is placed successfully!", notification.getOrderNumber()))
                .body(formatter.execute(body))
                .build();

        gateway.send(mail);
    }
}
