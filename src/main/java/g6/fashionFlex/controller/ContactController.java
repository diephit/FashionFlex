package g6.fashionFlex.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import g6.fashionFlex.dto.ContactForm;
import g6.fashionFlex.service.EmailService;
import jakarta.servlet.http.HttpSession;

@Controller
public class ContactController extends BaseController {  

    @Autowired
    private EmailService emailService;

    @Value("${contact.recipient.email:nnguyenkiet051205@gmail.com}")
    private String recipientEmail;

    @GetMapping("/contact")
    public String contact(Model model, HttpSession session) {  
        addAuthenticationToModel(model, session);  

        model.addAttribute("contactForm", new ContactForm());
        return "contact";
    }

    @PostMapping("/contact")
    public String handleContact(@ModelAttribute ContactForm contactForm, RedirectAttributes ra) {
        try {
            String subject = "Contact form message";
            StringBuilder body = new StringBuilder();
            body.append("Email: ").append(contactForm.getEmail()).append("\n\n");
            body.append("Message:\n").append(contactForm.getMessage());

            emailService.sendSimpleMessage(recipientEmail, subject, body.toString());
            ra.addFlashAttribute("success", "Message sent successfully.");
        } catch (org.springframework.mail.MailAuthenticationException ex) {
            String errorMsg = ex.getMessage();
            if (errorMsg != null && (errorMsg.contains("MAIL_USERNAME") || errorMsg.contains("MAIL_PASSWORD"))) {
                ra.addFlashAttribute("error", "Email configuration error. Please check your .env file and ensure MAIL_USERNAME and MAIL_PASSWORD are set correctly.");
            } else {
                ra.addFlashAttribute("error", "Authentication failed. Please check your Gmail App Password in .env file.");
            }
        } catch (org.springframework.mail.MailException ex) {
            ra.addFlashAttribute("error", "Failed to send message: " + ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to send message: " + ex.getMessage());
        }
        return "redirect:/contact";
    }
}