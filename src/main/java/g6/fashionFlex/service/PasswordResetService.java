package g6.fashionFlex.service;

import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordResetService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Constants
    private static final String CODE_PREFIX = "verification:";
    private static final String ATTEMPTS_SUFFIX = ":attempts";
    private static final String LAST_SENT_SUFFIX = ":last_sent";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String RATE_COUNT_SUFFIX = ":count";
    private static final String BLOCKED_SUFFIX = ":blocked_until";

    private static final long CODE_EXPIRY_MINUTES = 5;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RATE_LIMIT_MAX_REQUESTS = 5;
    private static final long RATE_LIMIT_WINDOW_MINUTES = 15;
    private static final long RATE_LIMIT_BLOCK_MINUTES = 15;

    /**
     * Generate random 6-digit verification code
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6 digits: 100000-999999
        return String.valueOf(code);
    }

    /**
     * Check if email is rate limited
     */
    public boolean isRateLimited(String email) {
        String blockedKey = RATE_LIMIT_PREFIX + email + BLOCKED_SUFFIX;
        String blockedUntil = (String) redisTemplate.opsForValue().get(blockedKey);

        if (blockedUntil != null) {
            long blockedTime = Long.parseLong(blockedUntil);
            if (System.currentTimeMillis() < blockedTime) {
                return true;
            } else {
                // Block time expired, remove key
                redisTemplate.delete(blockedKey);
            }
        }

        return false;
    }

    /**
     * Get remaining block time in minutes
     */
    public long getRemainingBlockTime(String email) {
        String blockedKey = RATE_LIMIT_PREFIX + email + BLOCKED_SUFFIX;
        String blockedUntil = (String) redisTemplate.opsForValue().get(blockedKey);

        if (blockedUntil != null) {
            long blockedTime = Long.parseLong(blockedUntil);
            long remaining = blockedTime - System.currentTimeMillis();
            return remaining > 0 ? TimeUnit.MILLISECONDS.toMinutes(remaining) + 1 : 0;
        }

        return 0;
    }

    /**
     * Check and increment rate limit counter
     */
    private void incrementRateLimitCounter(String email) {
        String countKey = RATE_LIMIT_PREFIX + email + RATE_COUNT_SUFFIX;
        String blockedKey = RATE_LIMIT_PREFIX + email + BLOCKED_SUFFIX;

        // Get current count
        String countStr = (String) redisTemplate.opsForValue().get(countKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        count++;

        // If exceeded limit, block the email
        if (count > RATE_LIMIT_MAX_REQUESTS) {
            long blockUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(RATE_LIMIT_BLOCK_MINUTES);
            redisTemplate.opsForValue().set(blockedKey, String.valueOf(blockUntil),
                    Duration.ofMinutes(RATE_LIMIT_BLOCK_MINUTES));
            // Reset counter
            redisTemplate.delete(countKey);
        } else {
            // Increment counter
            redisTemplate.opsForValue().set(countKey, String.valueOf(count),
                    Duration.ofMinutes(RATE_LIMIT_WINDOW_MINUTES));
        }
    }

    /**
     * Check if can resend code (cooldown period)
     */
    public boolean canResendCode(String email) {
        String lastSentKey = CODE_PREFIX + email + LAST_SENT_SUFFIX;
        String lastSentStr = (String) redisTemplate.opsForValue().get(lastSentKey);

        if (lastSentStr != null) {
            long lastSent = Long.parseLong(lastSentStr);
            long elapsed = System.currentTimeMillis() - lastSent;
            return elapsed >= TimeUnit.SECONDS.toMillis(RESEND_COOLDOWN_SECONDS);
        }

        return true;
    }

    /**
     * Get remaining cooldown time in seconds
     */
    public long getRemainingCooldown(String email) {
        String lastSentKey = CODE_PREFIX + email + LAST_SENT_SUFFIX;
        String lastSentStr = (String) redisTemplate.opsForValue().get(lastSentKey);

        if (lastSentStr != null) {
            long lastSent = Long.parseLong(lastSentStr);
            long elapsed = System.currentTimeMillis() - lastSent;
            long remaining = TimeUnit.SECONDS.toMillis(RESEND_COOLDOWN_SECONDS) - elapsed;
            return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) : 0;
        }

        return 0;
    }

    /**
     * Send verification code to email
     */
    public void sendVerificationCode(String email) throws Exception {
        System.out.println("=== PASSWORD RESET SERVICE - START ===");
        System.out.println("Email: " + email);

        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));
        System.out.println("User found: " + user.getEmail());

        // Check rate limit
        if (isRateLimited(email)) {
            long remainingMinutes = getRemainingBlockTime(email);
            throw new IllegalStateException("Too many requests. Please try again after " + remainingMinutes + " minutes.");
        }

        // Check cooldown
        if (!canResendCode(email)) {
            long remainingSeconds = getRemainingCooldown(email);
            throw new IllegalStateException("Please wait " + remainingSeconds + " seconds before requesting a new code.");
        }

        // Generate code
        String code = generateVerificationCode();
        System.out.println("Generated code: " + code);

        // Save to Redis
        String codeKey = CODE_PREFIX + email + ":code";
        String attemptsKey = CODE_PREFIX + email + ATTEMPTS_SUFFIX;
        String lastSentKey = CODE_PREFIX + email + LAST_SENT_SUFFIX;

        System.out.println("Saving to Redis:");
        System.out.println("  Code key: " + codeKey);
        System.out.println("  Code value: " + code);
        System.out.println("  Expiry: " + CODE_EXPIRY_MINUTES + " minutes");

        try {
            redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(CODE_EXPIRY_MINUTES));
            redisTemplate.opsForValue().set(attemptsKey, "0", Duration.ofMinutes(CODE_EXPIRY_MINUTES));
            redisTemplate.opsForValue().set(lastSentKey, String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(RESEND_COOLDOWN_SECONDS));
            System.out.println("Redis save completed!");

            // Verify save
            String savedCode = (String) redisTemplate.opsForValue().get(codeKey);
            System.out.println("Verification - Retrieved code from Redis: " + savedCode);

            if (savedCode == null || !savedCode.equals(code)) {
                throw new Exception("Failed to save code to Redis! Expected: " + code + ", Got: " + savedCode);
            }
        } catch (Exception e) {
            System.out.println("ERROR saving to Redis: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        // Increment rate limit counter
        incrementRateLimitCounter(email);

        // Send email
        try {
            emailService.sendPasswordResetVerificationCode(email, code);
        } catch (MessagingException e) {
            throw new Exception("Failed to send email: " + e.getMessage());
        }

        System.out.println("=== PASSWORD RESET SERVICE - END ===");
    }

    /**
     * Verify code
     */
    public boolean verifyCode(String email, String code) throws Exception {
        System.out.println("=== VERIFY CODE - START ===");
        System.out.println("Email: " + email);
        System.out.println("Code entered: " + code);

        String codeKey = CODE_PREFIX + email + ":code";
        String attemptsKey = CODE_PREFIX + email + ATTEMPTS_SUFFIX;

        System.out.println("Looking for Redis key: " + codeKey);

        // Get stored code
        String storedCode = (String) redisTemplate.opsForValue().get(codeKey);
        System.out.println("Retrieved from Redis: " + storedCode);

        if (storedCode == null) {
            System.out.println("ERROR: Code not found in Redis!");

            // Debug: List all keys in Redis
            System.out.println("DEBUG: Checking all Redis keys with pattern 'verification:*'");
            throw new IllegalArgumentException("Verification code expired or not found. Please request a new code.");
        }

        // Get attempts
        String attemptsStr = (String) redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        System.out.println("Current attempts: " + attempts);

        // Check max attempts
        if (attempts >= MAX_ATTEMPTS) {
            System.out.println("Max attempts exceeded!");
            // Delete code
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            throw new IllegalStateException("Maximum verification attempts exceeded. Please request a new code.");
        }

        // Verify code
        System.out.println("Comparing codes:");
        System.out.println("  Stored: " + storedCode);
        System.out.println("  Entered: " + code);
        System.out.println("  Match: " + storedCode.equals(code));

        if (!storedCode.equals(code)) {
            // Increment attempts
            attempts++;
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts),
                    Duration.ofMinutes(CODE_EXPIRY_MINUTES));

            int remainingAttempts = MAX_ATTEMPTS - attempts;
            if (remainingAttempts > 0) {
                System.out.println("Invalid code, " + remainingAttempts + " attempts remaining");
                throw new IllegalArgumentException("Invalid verification code. You have " + remainingAttempts + " attempt(s) remaining.");
            } else {
                // Delete code after max attempts
                redisTemplate.delete(codeKey);
                redisTemplate.delete(attemptsKey);
                throw new IllegalStateException("Maximum verification attempts exceeded. Please request a new code.");
            }
        }

        // Code is correct - keep it in Redis for final password reset
        System.out.println("Code verified successfully!");
        System.out.println("=== VERIFY CODE - END ===");
        return true;
    }

    /**
     * Reset password
     */
    public void resetPassword(String email, String code, String newPassword) throws Exception {
        String codeKey = CODE_PREFIX + email + ":code";

        // Verify code one more time
        String storedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Clear all Redis keys for this email
        clearVerificationData(email);
    }

    /**
     * Clear all verification data for email
     */
    public void clearVerificationData(String email) {
        redisTemplate.delete(CODE_PREFIX + email + ":code");
        redisTemplate.delete(CODE_PREFIX + email + ATTEMPTS_SUFFIX);
        redisTemplate.delete(CODE_PREFIX + email + LAST_SENT_SUFFIX);
    }

    /**
     * Get remaining attempts
     */
    public int getRemainingAttempts(String email) {
        String attemptsKey = CODE_PREFIX + email + ATTEMPTS_SUFFIX;
        String attemptsStr = (String) redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
}
