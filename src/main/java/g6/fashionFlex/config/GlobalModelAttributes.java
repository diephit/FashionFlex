package g6.fashionFlex.config;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.UserRepository;

@ControllerAdvice
@Component
public class GlobalModelAttributes {

    private final UserRepository userRepository;

    public GlobalModelAttributes(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !("anonymousUser".equals(auth.getPrincipal()));
    }

    @ModelAttribute("user")
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Optional<User> user = userRepository.findByEmail(auth.getName());
        return user.orElse(null);
    }

    @ModelAttribute("displayName")
    public String displayName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "User";
        }
        return userRepository.findByEmail(auth.getName())
                .map(u -> u.getName() != null ? u.getName() : auth.getName())
                .orElse(auth.getName());
    }
}
