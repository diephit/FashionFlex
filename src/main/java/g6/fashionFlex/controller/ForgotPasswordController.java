package g6.fashionFlex.controller;

import g6.fashionFlex.service.ForgotPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    /**
     * Show forgot password page (enter email)
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    /**
     * Send verification code to email
     */
    @PostMapping("/forgot-password/send-code")
    public String sendVerificationCode(@RequestParam String email, 
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        boolean sent = forgotPasswordService.sendVerificationCode(email);
        
        if (sent) {
            model.addAttribute("email", email);
            model.addAttribute("message", "We have sent a verification code to your email: " + email);
            return "verify-code";
        } else {
            redirectAttributes.addFlashAttribute("error", "Email not found in our system.");
            return "redirect:/forgot-password";
        }
    }

    /**
     * Show verification code page
     */
    @GetMapping("/verify-code")
    public String showVerifyCodePage(@RequestParam(required = false) String email, Model model) {
        if (email == null || email.isEmpty()) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", email);
        return "verify-code";
    }

    /**
     * Verify code and redirect to reset password page
     */
    @PostMapping("/verify-code")
    public String verifyCode(@RequestParam String email,
                            @RequestParam String code,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        boolean valid = forgotPasswordService.verifyCode(email, code);
        
        if (valid) {
            model.addAttribute("email", email);
            model.addAttribute("code", code);
            return "reset-password";
        } else {
            model.addAttribute("email", email);
            model.addAttribute("error", "Invalid or expired verification code. Please try again.");
            return "verify-code";
        }
    }

    /**
     * Show reset password page
     */
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam(required = false) String email,
                                       @RequestParam(required = false) String code,
                                       Model model) {
        if (email == null || code == null) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", email);
        model.addAttribute("code", code);
        return "reset-password";
    }

    /**
     * Reset password
     */
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                               @RequestParam String code,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("email", email);
            model.addAttribute("code", code);
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }

        // Validate password length
        if (newPassword.length() < 6) {
            model.addAttribute("email", email);
            model.addAttribute("code", code);
            model.addAttribute("error", "Password must be at least 6 characters.");
            return "reset-password";
        }

        // Reset password
        boolean success = forgotPasswordService.resetPassword(email, code, newPassword);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Password reset successfully! You can now login with your new password.");
            return "redirect:/login";
        } else {
            model.addAttribute("email", email);
            model.addAttribute("code", code);
            model.addAttribute("error", "Failed to reset password. Please try again.");
            return "reset-password";
        }
    }
}