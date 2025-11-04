package g6.fashionFlex.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("FashionFlex - Password Reset Request");

            String resetUrl = baseUrl + "/reset-password?token=" + token;

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #717fe0; color: white; padding: 20px; text-align: center; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 5px; margin: 20px 0; }
                        .button { display: inline-block; padding: 12px 30px; background: #717fe0; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>FashionFlex</h1>
                        </div>
                        <div class="content">
                            <h2>Password Reset Request</h2>
                            <p>We received a request to reset your password. Click the button below to reset it:</p>
                            <a href="%s" class="button">Reset Password</a>
                            <p>Or copy and paste this link into your browser:</p>
                            <p style="word-break: break-all; color: #717fe0;">%s</p>
                            <p><strong>This link will expire in 24 hours.</strong></p>
                            <p>If you didn't request this, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 FashionFlex. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, resetUrl, resetUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
