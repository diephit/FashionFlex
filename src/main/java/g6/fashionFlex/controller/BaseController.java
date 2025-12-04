package g6.fashionFlex.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.ui.Model;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.repository.WishlistItemRepository;
import g6.fashionFlex.repository.WishlistRepository;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

public abstract class BaseController {
    
    @Autowired
    protected UserRepository userRepository;
    
    @Autowired
    protected CustomerRepository customerRepository;
    
    @Autowired
    protected WishlistRepository wishlistRepository;
    
    @Autowired
    protected WishlistItemRepository wishlistItemRepository;
    
    protected void addAuthenticationToModel(Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        String email = getAuthenticatedEmail(authentication);
        boolean isAuthenticated = email != null;

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("wishlistCount", 0);
        
        if (isAuthenticated) {
            User user = (User) session.getAttribute("loggedInUser");
            
            if (user == null || (user.getEmail() != null && email != null && !user.getEmail().equalsIgnoreCase(email))) {
                user = getAuthenticatedUser(authentication).orElse(null);
                if (user != null) {
                    session.setAttribute("loggedInUser", user);
                    session.setAttribute("userId", user.getUserID());
                    session.setAttribute("userEmail", user.getEmail());
                    session.setAttribute("userName", user.getName());
                }
            }

            if (user != null) {
                model.addAttribute("displayName", user.getName());
                model.addAttribute("userEmail", user.getEmail());
                model.addAttribute("userId", user.getUserID());
                model.addAttribute("wishlistCount", getWishlistCount(user));
            } else if (email != null) {
                model.addAttribute("displayName", email.split("@")[0]);
                model.addAttribute("userEmail", email);
            }
        }
    }

    protected Optional<User> getAuthenticatedUser(Authentication authentication) {
        String email = getAuthenticatedEmail(authentication);
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.toLowerCase());
    }

    protected Optional<User> getAuthenticatedUser() {
        return getAuthenticatedUser(SecurityContextHolder.getContext().getAuthentication());
    }

    protected Optional<Customer> getCustomerFromAuth(Authentication authentication) {
        return getAuthenticatedUser(authentication).flatMap(customerRepository::findByUser);
    }

    protected Optional<Customer> getAuthenticatedCustomer() {
        return getCustomerFromAuth(SecurityContextHolder.getContext().getAuthentication());
    }

    protected String getAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String email = null;
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            Object emailAttr = oauth2Token.getPrincipal().getAttributes().get("email");
            if (emailAttr != null) {
                email = emailAttr.toString();
            }
        }

        if (email == null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
            } else if (principal instanceof String str && !"anonymousUser".equals(str)) {
                email = str;
            } else {
                email = authentication.getName();
            }
        }

        return email != null ? email.toLowerCase() : null;
    }
    
    private int getWishlistCount(User user) {
        if (user == null) {
            return 0;
        }
        return customerRepository.findByUser(user)
                .flatMap(wishlistRepository::findByCustomer)
                .map(wishlistItemRepository::countByWishlist)
                .orElse(0);
    }
}