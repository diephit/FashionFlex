package g6.fashionFlex.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class PasswordSetupService {

    private static final int TOKEN_TTL_MINUTES = 30;
    private static final String SUGGESTION_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@$%";
    public static final String PASSWORD_PLACEHOLDER_PREFIX = "{noop}OAUTH2_PENDING";

    private final Map<String, TokenData> tokenStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private static class TokenData {
        private final String email;
        private final LocalDateTime expiresAt;

        private TokenData(String email, LocalDateTime expiresAt) {
            this.email = email;
            this.expiresAt = expiresAt;
        }
    }

    public String generateToken(String email) {
        String token = UUID.randomUUID().toString().replace("-", "") + Long.toString(System.nanoTime(), 36);
        tokenStore.put(token, new TokenData(email.toLowerCase(), LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES)));
        return token;
    }

    public Optional<String> consumeToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        TokenData data = tokenStore.remove(token);
        if (data == null) {
            return Optional.empty();
        }
        if (LocalDateTime.now().isAfter(data.expiresAt)) {
            return Optional.empty();
        }
        return Optional.of(data.email);
    }

    public String generateSuggestedPassword() {
        StringBuilder builder = new StringBuilder();
        int length = 12;
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(SUGGESTION_CHARSET.length());
            builder.append(SUGGESTION_CHARSET.charAt(index));
        }
        return builder.toString();
    }

    public static String buildPlaceholderPassword() {
        return PASSWORD_PLACEHOLDER_PREFIX + "_" + UUID.randomUUID();
    }

    public static boolean isPlaceholderPassword(String password) {
        return password != null && password.startsWith(PASSWORD_PLACEHOLDER_PREFIX);
    }
}

