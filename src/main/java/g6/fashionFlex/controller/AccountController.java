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

    @GetMapping("/my-account")
    public String myAccount(RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("message", "You must sign in first to view your profile");
            return "redirect:/login";
        }
        // User is authenticated, redirect to profile
        return "redirect:/profile";
    }

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
        return "profile-edit";
    }
    
    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String name,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String dateOfBirth,
                                @RequestParam(required = false) String address,
                                RedirectAttributes redirectAttributes) {
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
        
        // Update user name
        if (name != null && !name.trim().isEmpty()) {
            user.setName(name.trim());
        }
        
        // Update customer info
        if (phone != null) {
            customer.setPhone(phone.trim().isEmpty() ? null : phone.trim());
        }
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            try {
                customer.setDateOfBirth(java.time.LocalDate.parse(dateOfBirth));
            } catch (Exception e) {
                // Invalid date format, ignore
            }
        }
        if (address != null) {
            customer.setAddress(address.trim().isEmpty() ? null : address.trim());
        }
        
        userRepository.save(user);
        customerRepository.save(customer);
        redirectAttributes.addFlashAttribute("message", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordPage(Model model, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        return "change-password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("error", "Please sign in to continue.");
            return "redirect:/login";
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Please sign in to continue.");
            return "redirect:/login";
        }
        if (currentPassword == null || !currentPassword.equals(user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect.");
            return "redirect:/profile/change-password";
        }
        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters.");
            return "redirect:/profile/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Password confirmation does not match.");
            return "redirect:/profile/change-password";
        }
        user.setPassword(newPassword);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "Password updated successfully.");
        return "redirect:/profile/change-password";
    }
}
