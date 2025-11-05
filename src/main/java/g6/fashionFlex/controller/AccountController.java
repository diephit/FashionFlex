package g6.fashionFlex.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.UserRepository;

import java.util.Optional;

@Controller
public class AccountController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/profile")
    public String profile(Model model, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        User user = userOpt.get();
        Optional<Customer> customerOpt = customerRepository.findByUser(user);
        if (customerOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "You need a registered customer account to access My Account.");
            return "redirect:/login";
        }
        Customer customer = customerOpt.get();
        model.addAttribute("customer", customer);
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        if (currentPassword == null || !currentPassword.equals(user.getPassword())) {
            redirectAttributes.addFlashAttribute("message", "Current password is incorrect.");
            return "redirect:/profile";
        }
        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("message", "New password must be at least 6 characters.");
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("message", "Password confirmation does not match.");
            return "redirect:/profile";
        }
        user.setPassword(newPassword);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "Password updated successfully.");
        return "redirect:/profile";
    }
}
