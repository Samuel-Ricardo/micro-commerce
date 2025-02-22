package com.js.microservices.notification.application.usecase;

import com.js.microservices.notification.domain.dto.FormatMailBodyDTO;
import com.js.microservices.notification.domain.usecase.FormatMailBodyUseCase;
import org.springframework.stereotype.Component;

@Component
public class FormatMailBodyUseCaseImpl implements FormatMailBodyUseCase {

    @Override
    public String execute(FormatMailBodyDTO mail) {
        return String.format("""
                                <!DOCTYPE html>
                                             <html lang="en">
                                             <head>
                                                 <meta charset="UTF-8">
                                                 <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                                 <title>Order Confirmation</title>
                                                 <style>
                                                     body {
                                                         font-family: Arial, sans-serif;
                                                         line-height: 1.6;
                                                         margin: 0;
                                                         padding: 0;
                                                         background-color: #f4f4f9;
                                                     }
                                                     .email-container {
                                                         max-width: 600px;
                                                         margin: 20px auto;
                                                         background: #ffffff;
                                                         border-radius: 8px;
                                                         box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                                                         overflow: hidden;
                                                     }
                                                     .header {
                                                         background: #6c63ff;
                                                         color: #ffffff;
                                                         padding: 20px;
                                                         text-align: center;
                                                     }
                                                     .header h1 {
                                                         margin: 0;
                                                         font-size: 24px;
                                                     }
                                                     .content {
                                                         padding: 20px;
                                                         color: #333333;
                                                     }
                                                     .content p {
                                                         margin: 10px 0;
                                                     }
                                                     .footer {
                                                         background: #f4f4f9;
                                                         color: #888888;
                                                         text-align: center;
                                                         padding: 10px 20px;
                                                         font-size: 12px;
                                                     }
                                                     .footer a {
                                                         color: #6c63ff;
                                                         text-decoration: none;
                                                     }
                                                     .footer a:hover {
                                                         text-decoration: underline;
                                                     }
                                                 </style>
                                             </head>
                                             <body>
                                                 <div class="email-container">
                                                     <div class="header">
                                                         <h1>Java Shop ♨</h1>
                                                     </div>
                                                     <div class="content">
                                                         <p>Hi <strong>%s</strong> <strong>%s</strong> 🌱,</p>
                                
                                                         <p>Your order of number <strong>%s</strong> is now placed successfully! :D</p>
                                
                                                         <p>Thank you for shopping with us. We hope you enjoy your order!</p>
                                
                                                         <p>Best Regards ☕,<br>- Java Shop ♨</p>
                                                     </div>
                                                     <div class="footer">
                                                         <p>&copy; 2025 Java Shop. All rights reserved.</p>
                                                         <p><a href="#">Visit our website</a> | <a href="#">Unsubscribe</a></p>
                                                     </div>
                                                 </div>
                                             </body>
                                             </html>
                            """,
                mail.getFirstName(),
                mail.getLastName(),
                mail.getOrderNumber()
        );
    }
}
