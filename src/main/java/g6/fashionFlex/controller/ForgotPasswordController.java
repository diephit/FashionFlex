package g6.fashionFlex.controller;

import g6.fashionFlex.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.createPasswordResetToken(email);
            redirectAttributes.addFlashAttribute("message",
                    "Password reset link has been sent to your email. Please check your inbox.");
            return "redirect:/forgot-password?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error: " + e.getMessage());
            return "redirect:/forgot-password?error";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        boolean valid = passwordResetService.validatePasswordResetToken(token);

        if (!valid) {
            model.addAttribute("error", "Invalid or expired password reset token");
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/reset-password?token=" + token;
        }

        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters");
            return "redirect:/reset-password?token=" + token;
        }

        try {
            passwordResetService.resetPassword(token, password);
            redirectAttributes.addAttribute("reset", "success");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
}
