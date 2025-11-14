package g6.fashionFlex.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.AdminRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.service.AdminStatsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AdminController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private AdminStatsService statsService;
    
    @GetMapping("/admin/admin-login")
    public String adminPage(Model model) {
        return "redirect:/admin/dashboard";
    }
    
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        try {
            Map<String, Object> stats = statsService.getDashboardStats();
            model.addAllAttributes(stats);
        } catch (Exception e) {
            // If stats service fails, provide default values
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalProducts", 0);
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalCategories", 0);
            model.addAttribute("totalRevenue", 0);
        }
        return "admin/dashboard";
    }
    
    @GetMapping("/admin-login")
    public String showAdminLoginPage(@RequestParam(required = false) String error,
                                    @RequestParam(required = false) String message,
                                    Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password, or you don't have admin privileges");
        }
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "admin/admin-login";
    }
    
    @PostMapping("/admin-login")
    public String processAdminLogin(@RequestParam String email,
                                   @RequestParam String password,
                                   @RequestParam(required = false) String remember,
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            
            // Check if user has admin role BEFORE setting authentication
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getRole() == null || 
                !user.getRole().getRoleName().equals(Role.RoleName.admin)) {
                redirectAttributes.addAttribute("error", "true");
                return "redirect:/admin-login";
            }
            
            // Check if user has Admin record
            if (adminRepository.findByUserUserID(user.getUserID()).isEmpty()) {
                redirectAttributes.addAttribute("error", "true");
                return "redirect:/admin-login";
            }
            
            // Set authentication in SecurityContext
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            
            // Save authentication to session using SecurityContextRepository
            SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
            securityContextRepository.saveContext(context, request, response);
            
            // Success - redirect to admin dashboard
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            redirectAttributes.addAttribute("error", "true");
            return "redirect:/admin-login";
        }
    }
}
