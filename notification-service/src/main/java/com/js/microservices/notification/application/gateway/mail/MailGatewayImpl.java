package com.js.microservices.notification.application.gateway.mail;

import com.js.microservices.notification.application.error.mail.SendMailError;
import com.js.microservices.notification.domain.dto.SendMailDTO;
import com.js.microservices.notification.domain.gateway.mail.MailGateway;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

@Component
public class MailGatewayImpl implements MailGateway {

    @Autowired
    private JavaMailSender sender;

    @Value("${notification.email}")
    private String ENTERPRISE_EMAIL;

    @Override
    public void send(@NotNull SendMailDTO notification) throws SendMailError {
        try {
            MimeMessagePreparator mail = mimeMessage -> {

                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);

                message.setFrom(ENTERPRISE_EMAIL);
                message.setTo(notification.getTo());
                message.setSubject(notification.getSubject());
                message.setText(notification.getBody(), true);
            };

            sender.send(mail);
        } catch (MailException e) {
            e.printStackTrace();
            throw new SendMailError(notification.getTo());
        }
    }
}
