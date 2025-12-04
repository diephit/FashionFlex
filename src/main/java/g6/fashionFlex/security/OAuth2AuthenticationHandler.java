package g6.fashionFlex.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2AuthenticationHandler {

    @Component
    public static class SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        
        @Autowired
        private UserRepository userRepository;
        
        @Autowired
        private CustomerRepository customerRepository;
        
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) throws IOException, ServletException {
            
            if (authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                String email = oauth2User.getAttribute("email");
                logger.info("OAuth2 login successful for user: " + email);
                
                // Lưu user vào session
                if (email != null) {
                    email = email.toLowerCase().trim();
                    User user = userRepository.findByEmail(email).orElse(null);
                    
                    if (user != null) {
                        // Tạo Customer record nếu chưa có (cho Google OAuth users)
                        customerRepository.findByUser(user).orElseGet(() -> {
                            Customer newCustomer = new Customer();
                            newCustomer.setUser(user);
                            newCustomer.setLoyaltyPoints(0);
                            newCustomer.setTotalSpent(java.math.BigDecimal.ZERO);
                            Customer saved = customerRepository.save(newCustomer);
                            logger.info("Created Customer record for OAuth user: " + user.getEmail());
                            return saved;
                        });
                        
                        HttpSession session = request.getSession(true); // Đảm bảo tạo session mới nếu chưa có
                        session.setAttribute("loggedInUser", user);
                        session.setAttribute("userId", user.getUserID());
                        session.setAttribute("userEmail", user.getEmail());
                        session.setAttribute("userName", user.getName());
                        session.setAttribute("isAuthenticated", true);
                        session.setAttribute("displayName", user.getName());
                        
                        logger.info("User saved to session: " + user.getEmail() + " with username: " + user.getName());
                    } else {
                        logger.warn("User not found in database after OAuth2 login: " + email);
                    }
                }
            }
            
            setDefaultTargetUrl("/");
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    @Component
    public static class FailureHandler extends SimpleUrlAuthenticationFailureHandler {
        
        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                AuthenticationException exception) throws IOException, ServletException {
            
            logger.error("OAuth2 authentication failed: " + exception.getMessage());
            
            String targetUrl = UriComponentsBuilder.fromUriString("/login")
                    .queryParam("error", "oauth2_failed")
                    .queryParam("message", "Google sign-in failed. Please try again.")
                    .build().toUriString();
            
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}