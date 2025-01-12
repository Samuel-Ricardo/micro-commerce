package com.js.microservices.notification.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Builder @Getter
public class SendOrderPlacedMailNotificationDTO {
    String to;
    String firstName;
    String lastName;
    String orderNumber;
}
