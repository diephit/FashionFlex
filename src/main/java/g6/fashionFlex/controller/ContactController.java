package g6.fashionFlex.controller;

import g6.fashionFlex.dto.ContactDTO;
import g6.fashionFlex.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    @Autowired
    private EmailService emailService;

    private static final String ADMIN_EMAIL = "antruong070204@gmail.com";

    @PostMapping("/contact/submit")
    public String submitContact(@ModelAttribute ContactDTO contactDTO, RedirectAttributes redirectAttributes) {
        try {
            // Validate input
            if (contactDTO.getName() == null || contactDTO.getName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please enter your name");
                return "redirect:/contact";
            }

            if (contactDTO.getEmail() == null || contactDTO.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please enter your email");
                return "redirect:/contact";
            }

            if (contactDTO.getMessage() == null || contactDTO.getMessage().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please enter your message");
                return "redirect:/contact";
            }

            // Send email
            emailService.sendContactEmail(
                    contactDTO.getName(),
                    contactDTO.getEmail(),
                    contactDTO.getMessage(),
                    ADMIN_EMAIL
            );

            redirectAttributes.addFlashAttribute("success", "Thank you for contacting us! We will get back to you soon.");
            return "redirect:/contact";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Sorry, there was an error sending your message. Please try again later.");
            return "redirect:/contact";
        }
    }
}
