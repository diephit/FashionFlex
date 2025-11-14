package g6.fashionFlex.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public void sendSimpleMessage(String to, String subject, String text) throws MailException {
        // Validate email configuration
        if (fromEmail == null || fromEmail.isEmpty()) {
            throw new MailAuthenticationException("Email configuration error: MAIL_USERNAME is not set. Please configure it in .env file.");
        }
        if (mailPassword == null || mailPassword.isEmpty()) {
            throw new MailAuthenticationException("Email configuration error: MAIL_PASSWORD is not set. Please configure it in .env file.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
