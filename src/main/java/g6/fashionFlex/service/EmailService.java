package g6.fashionFlex.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send contact form email to admin
     */
    public void sendContactEmail(String senderName, String senderEmail, String message, String recipientEmail) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(recipientEmail);
        helper.setSubject("New Contact Form Submission from " + senderName);
        helper.setReplyTo(senderEmail);

        String htmlContent = buildContactEmailTemplate(senderName, senderEmail, message);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
    }

    /**
     * Build HTML email template for contact form
     */
    private String buildContactEmailTemplate(String senderName, String senderEmail, String message) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }" +
                ".header { background-color: #667eea; color: white; padding: 20px; text-align: center; }" +
                ".content { background-color: white; padding: 30px; margin-top: 20px; border-radius: 5px; }" +
                ".info-row { margin-bottom: 15px; }" +
                ".label { font-weight: bold; color: #667eea; }" +
                ".message-box { background-color: #f5f5f5; padding: 15px; border-left: 4px solid #667eea; margin-top: 20px; }" +
                ".footer { text-align: center; margin-top: 20px; color: #999; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>= New Contact Form Submission</h2>" +
                "</div>" +
                "<div class='content'>" +
                "<div class='info-row'>" +
                "<span class='label'>From:</span> " + senderName +
                "</div>" +
                "<div class='info-row'>" +
                "<span class='label'>Email:</span> <a href='mailto:" + senderEmail + "'>" + senderEmail + "</a>" +
                "</div>" +
                "<div class='message-box'>" +
                "<div class='label'>Message:</div>" +
                "<p>" + message.replace("\n", "<br>") + "</p>" +
                "</div>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>This email was sent from FashionFlex contact form</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String recipientEmail, String resetLink) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(recipientEmail);
        helper.setSubject("Password Reset Request - FashionFlex");

        String htmlContent = buildPasswordResetTemplate(resetLink);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
    }

    public void sendPasswordResetVerificationCode(String recipientEmail, String verificationCode) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(recipientEmail);
        helper.setSubject("Password Reset Verification Code - FashionFlex");

        String htmlContent = buildPasswordResetVerificationCodeTemplate(verificationCode);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
    }

    /**
     * Build HTML email template for password reset
     */
    private String buildPasswordResetTemplate(String resetLink) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }" +
                ".header { background-color: #667eea; color: white; padding: 20px; text-align: center; }" +
                ".content { background-color: white; padding: 30px; margin-top: 20px; border-radius: 5px; }" +
                ".button { display: inline-block; padding: 12px 30px; background-color: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".footer { text-align: center; margin-top: 20px; color: #999; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>=► Password Reset Request</h2>" +
                "</div>" +
                "<div class='content'>" +
                "<p>You have requested to reset your password. Click the button below to reset your password:</p>" +
                "<div style='text-align: center;'>" +
                "<a href='" + resetLink + "' class='button'>Reset Password</a>" +
                "</div>" +
                "<p>If you didn't request this, please ignore this email.</p>" +
                "<p><small>This link will expire in 1 hour.</small></p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>FashionFlex - Your Fashion Destination</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildPasswordResetVerificationCodeTemplate(String verificationCode) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }" +
                ".header { background-color: #667eea; color: white; padding: 20px; text-align: center; }" +
                ".content { background-color: white; padding: 30px; margin-top: 20px; border-radius: 5px; }" +
                ".verification-code { font-size: 24px; font-weight: bold; text-align: center; margin: 20px 0; }" +
                ".footer { text-align: center; margin-top: 20px; color: #999; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>=► Password Reset Verification Code</h2>" +
                "</div>" +
                "<div class='content'>" +
                "<p>You have requested to reset your password. Here is your verification code:</p>" +
                "<div class='verification-code'>" + verificationCode + "</div>" +
                "<p>If you didn't request this, please ignore this email.</p>" +
                "<p><small>This code will expire in 1 hour.</small></p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>FashionFlex - Your Fashion Destination</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}