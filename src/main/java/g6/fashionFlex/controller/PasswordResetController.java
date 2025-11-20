package g6.fashionFlex.controller;

import g6.fashionFlex.dto.ForgotPasswordRequest;
import g6.fashionFlex.dto.ResetPasswordRequest;
import g6.fashionFlex.dto.VerifyCodeRequest;
import g6.fashionFlex.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    // ==================== FORGOT PASSWORD ====================

    /**
     * Show forgot password page
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    /**
     * Handle forgot password form submission
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute ForgotPasswordRequest request,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        System.out.println("=== FORGOT PASSWORD - START ===");
        System.out.println("Email received: " + request.getEmail());

        if (bindingResult.hasErrors()) {
            System.out.println("Validation errors: " + bindingResult.getAllErrors());
            model.addAttribute("error", "Please enter a valid email address");
            return "auth/forgot-password";
        }

        try {
            System.out.println("Calling sendVerificationCode...");
            passwordResetService.sendVerificationCode(request.getEmail());
            System.out.println("Verification code sent successfully!");

            redirectAttributes.addFlashAttribute("email", request.getEmail());
            redirectAttributes.addFlashAttribute("message",
                    "A verification code has been sent to your email address.");

            String redirectUrl = "redirect:/verify-token?email=" + request.getEmail();
            System.out.println("Redirecting to: " + redirectUrl);
            return redirectUrl;
        } catch (IllegalArgumentException e) {
            // Email not found
            System.out.println("ERROR - IllegalArgumentException: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "auth/forgot-password";
        } catch (IllegalStateException e) {
            // Rate limited or cooldown
            System.out.println("ERROR - IllegalStateException: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "auth/forgot-password";
        } catch (Exception e) {
            // Log the actual error for debugging
            System.out.println("ERROR - Exception: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error: " + e.getMessage());
            return "auth/forgot-password";
        }
    }

    // ==================== VERIFY TOKEN ====================

    /**
     * Show verify token page
     */
    @GetMapping("/verify-token")
    public String showVerifyTokenPage(@RequestParam(required = false) String email,
                                     Model model,
                                     HttpSession session) {
        if (email == null || email.isEmpty()) {
            return "redirect:/forgot-password";
        }

        model.addAttribute("email", email);
        model.addAttribute("verifyCodeRequest", new VerifyCodeRequest());
        model.addAttribute("remainingAttempts", passwordResetService.getRemainingAttempts(email));

        return "auth/verify-token";
    }

    /**
     * Handle verify token form submission
     */
    @PostMapping("/verify-token")
    public String processVerifyToken(@RequestParam String email,
                                    @RequestParam String token,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession session,
                                    Model model) {
        try {
            boolean verified = passwordResetService.verifyCode(email, token);

            if (verified) {
                // Store email and code in session for final password reset
                session.setAttribute("resetEmail", email);
                session.setAttribute("resetCode", token);
                redirectAttributes.addFlashAttribute("message", "Verification successful! Please enter your new password.");
                return "redirect:/reset-password";
            } else {
                model.addAttribute("error", "Invalid verification code");
                model.addAttribute("email", email);
                model.addAttribute("remainingAttempts", passwordResetService.getRemainingAttempts(email));
                return "auth/verify-token";
            }
        } catch (IllegalArgumentException e) {
            // Invalid code or attempts remaining
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("remainingAttempts", passwordResetService.getRemainingAttempts(email));
            return "auth/verify-token";
        } catch (IllegalStateException e) {
            // Max attempts exceeded
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/verify-token";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred. Please try again.");
            model.addAttribute("email", email);
            model.addAttribute("remainingAttempts", passwordResetService.getRemainingAttempts(email));
            return "auth/verify-token";
        }
    }

    // ==================== RESET PASSWORD ====================

    /**
     * Show reset password page
     */
    @GetMapping("/reset-password")
    public String showResetPasswordPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("resetEmail");
        String code = (String) session.getAttribute("resetCode");

        if (email == null || code == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid session. Please start the password reset process again.");
            return "redirect:/forgot-password";
        }

        model.addAttribute("email", email);
        model.addAttribute("resetPasswordRequest", new ResetPasswordRequest());
        return "auth/reset-password";
    }

    /**
     * Handle reset password form submission
     */
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        String email = (String) session.getAttribute("resetEmail");
        String code = (String) session.getAttribute("resetCode");

        if (email == null || code == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid session. Please start the password reset process again.");
            return "redirect:/forgot-password";
        }

        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("email", email);
            return "auth/reset-password";
        }

        // Validate password length
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters");
            model.addAttribute("email", email);
            return "auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(email, code, password);

            // Clear session
            session.removeAttribute("resetEmail");
            session.removeAttribute("resetCode");

            redirectAttributes.addFlashAttribute("success", "Password reset successful! Please login with your new password.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/reset-password";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred. Please try again.");
            model.addAttribute("email", email);
            return "auth/reset-password";
        }
    }

    // ==================== AJAX ENDPOINTS ====================

    /**
     * Resend verification code (AJAX)
     */
    @PostMapping("/resend-code")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resendCode(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            passwordResetService.sendVerificationCode(email);
            response.put("success", true);
            response.put("message", "Verification code sent successfully");
            response.put("cooldown", 60); // Cooldown in seconds
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Email not found
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            // Rate limited or cooldown
            response.put("success", false);
            response.put("message", e.getMessage());

            // Check if it's cooldown or rate limit
            if (e.getMessage().contains("wait")) {
                long remaining = passwordResetService.getRemainingCooldown(email);
                response.put("remainingSeconds", remaining);
            } else if (e.getMessage().contains("Too many")) {
                long remaining = passwordResetService.getRemainingBlockTime(email);
                response.put("remainingMinutes", remaining);
            }

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send verification code");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Check remaining cooldown time (AJAX)
     */
    @GetMapping("/check-cooldown")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkCooldown(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        long remaining = passwordResetService.getRemainingCooldown(email);
        boolean canResend = passwordResetService.canResendCode(email);

        response.put("success", true);
        response.put("canResend", canResend);
        response.put("remainingSeconds", remaining);

        return ResponseEntity.ok(response);
    }
}
