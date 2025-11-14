package g6.fashionFlex.service;

import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ForgotPasswordService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Store verification codes in memory (in production, use Redis or database)
    private Map<String, VerificationData> verificationCodes = new HashMap<>();

    private static class VerificationData {
        String email;
        String code;
        LocalDateTime expiry;

        public VerificationData(String email, String code, LocalDateTime expiry) {
            this.email = email;
            this.code = code;
            this.expiry = expiry;
        }
    }

    /**
     * Send verification code to user's email
     */
    public boolean sendVerificationCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false; // User not found
        }

        // Generate 6-digit verification code
        String code = generateVerificationCode();
        
        // Store code with 15 minutes expiry
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
        verificationCodes.put(email, new VerificationData(email, code, expiry));

        // Send email
        try {
            String subject = "Password Reset Verification Code - Fashion Flex";
            String text = "Hello,\n\n" +
                    "You have requested to reset your password.\n\n" +
                    "Your verification code is: " + code + "\n\n" +
                    "This code will expire in 15 minutes.\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "Fashion Flex Team";
            
            emailService.sendSimpleMessage(email, subject, text);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verify the code entered by user
     */
    public boolean verifyCode(String email, String code) {
        VerificationData data = verificationCodes.get(email);
        
        if (data == null) {
            return false; // No verification code found
        }

        // Check if code has expired
        if (LocalDateTime.now().isAfter(data.expiry)) {
            verificationCodes.remove(email);
            return false; // Code expired
        }

        // Verify code
        return data.code.equals(code);
    }

    /**
     * Reset password after successful verification
     */
    public boolean resetPassword(String email, String code, String newPassword) {
        // Verify code first
        if (!verifyCode(email, code)) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        // Sử dụng setPassword thay vì setPasswordHash
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Remove used verification code
        verificationCodes.remove(email);

        return true;
    }

    /**
     * Generate random 6-digit verification code
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Clear expired codes (call this periodically)
     */
    public void clearExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationCodes.entrySet().removeIf(entry -> 
            now.isAfter(entry.getValue().expiry)
        );
    }
}