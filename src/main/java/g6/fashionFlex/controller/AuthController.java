package g6.fashionFlex.controller;

import g6.fashionFlex.dto.AuthResponse;
import g6.fashionFlex.dto.LoginRequest;
import g6.fashionFlex.dto.RegisterRequest;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.security.JwtTokenProvider;
import g6.fashionFlex.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.entity.User;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // Display login page
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                @RequestParam(required = false) String registered,
                                @RequestParam(required = false) String message,
                                HttpSession session,  // THÊM DÒNG NÀY
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out successfully");
        }
        if (registered != null) {
            model.addAttribute("success", "Registration successful! Please login.");
        }
        if (message != null) {
            model.addAttribute("message", message);
        }
        
        // THÊM ĐOẠN NÀY - Lấy và clear loginMessage từ session
        String loginMessage = (String) session.getAttribute("loginMessage");
        if (loginMessage != null) {
            model.addAttribute("loginMessage", loginMessage);
            session.removeAttribute("loginMessage");
        }
        
        model.addAttribute("user", new RegisterRequest());
        return "login";
    }

    // Handle registration from form submission
    @PostMapping("/api/auth/register")
    public String registerUser(@RequestParam(required = false) String fullName,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String password,
                               @RequestParam(required = false) String phone,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        // Create RegisterRequest from form parameters
        RegisterRequest registerRequest = new RegisterRequest();
        if (fullName != null) {
            registerRequest.setFullName(fullName.trim());
        }
        if (email != null) {
            registerRequest.setEmail(email.trim().toLowerCase());
        }
        if (password != null) {
            registerRequest.setPassword(password);
        }
        
        if (phone != null) {
            registerRequest.setPhone(phone.trim());
        }

        // Always add user object back to model for form binding
        model.addAttribute("user", registerRequest);
        model.addAttribute("showRegister", true);
        
        // Validate required fields
        if (registerRequest.getFullName() == null || registerRequest.getFullName().trim().isEmpty()) {
            model.addAttribute("registerError", "Full name is required");
            return "login";
        }
        
        if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
            model.addAttribute("registerError", "Email is required");
            return "login";
        }
        
        if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
            model.addAttribute("registerError", "Password is required");
            return "login";
        }
        
        if (!isStrongPassword(registerRequest.getPassword())) {
            model.addAttribute("registerError", "Password must be at least 8 characters with at least one uppercase letter and one special character.");
            return "login";
        }

        if (registerRequest.getPhone() == null || registerRequest.getPhone().trim().isEmpty()) {
            model.addAttribute("registerError", "Phone number is required");
            return "login";
        }

        String normalizedPhone = registerRequest.getPhone().trim();
        if (!isValidPhone(normalizedPhone)) {
            model.addAttribute("registerError", "Phone number must start with 0 and contain exactly 10 digits.");
            return "login";
        }

        registerRequest.setPhone(normalizedPhone);

        if (customerRepository.existsByPhone(normalizedPhone)) {
            model.addAttribute("registerError", "This phone number has been used, please try another one");
            return "login";
        }

        try {
            // Normalize email to lowercase for consistency
            String emailInput = registerRequest.getEmail();
            if (emailInput == null || emailInput.trim().isEmpty()) {
                model.addAttribute("registerError", "Email is required");
                model.addAttribute("showRegister", true);
                return "login";
            }
            
            String normalizedEmail = emailInput.trim().toLowerCase();
            registerRequest.setEmail(normalizedEmail);
            
            // Check if email already exists in database - query DB directly
            boolean emailExists = userRepository.existsByEmail(normalizedEmail);
            
            // Also check with findByEmail to be absolutely sure
            if (!emailExists) {
                emailExists = userRepository.findByEmail(normalizedEmail).isPresent();
            }
            
            if (emailExists) {
                model.addAttribute("registerError", "This email has been used, please try another one");
                model.addAttribute("showRegister", true);
                model.addAttribute("user", registerRequest); // Keep form data
                return "login";
            }
            
            // Email is unique, proceed with registration
            userService.registerUser(registerRequest);
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            // Check if it's an email already exists error from service layer
            if (errorMessage != null && errorMessage.contains("email has been used")) {
                model.addAttribute("registerError", "This email has been used, please try another one");
            } else {
                model.addAttribute("registerError", errorMessage != null ? errorMessage : "Registration failed. Please try again.");
            }
            model.addAttribute("showRegister", true);
            model.addAttribute("user", registerRequest); // Keep form data
            return "login";
        }
    }

    private boolean isStrongPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.matches("^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$");
    }

    private boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        return phone.matches("^0\\d{9}$");
    }

    // REST API endpoint for registration (for AJAX calls)
    @PostMapping("/api/auth/register-json")
    @ResponseBody
    public ResponseEntity<?> registerUserJson(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            UserDTO userDTO = userService.registerUser(registerRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("user", userDTO);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // REST API endpoint for login (returns JWT token)
    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        UserDTO userDTO = userService.getUserByEmail(loginRequest.getEmail());

        AuthResponse response = new AuthResponse(jwt, userDTO.getId(), userDTO.getEmail(), userDTO.getFullName());

        return ResponseEntity.ok(response);
    }

    // Handle form-based login (traditional form submission)
    @PostMapping("/login")
    public String processLogin(@RequestParam String email,
                          @RequestParam String password,
                          @RequestParam(required = false) String remember,
                          RedirectAttributes redirectAttributes) {
        try {
            // Normalize email to lowercase for consistency
            String normalizedEmail = email.toLowerCase().trim();
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get user details and add to redirect attributes
            UserDTO user = userService.getUserByEmail(normalizedEmail);
            redirectAttributes.addFlashAttribute("displayName", user.getFullName());
            redirectAttributes.addFlashAttribute("isAuthenticated", true);
            
            // Redirect to index instead of user/home
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "true");
            return "redirect:/login";
        }
    }

    @PostMapping("/forgot")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("message", "No account found for that email.");
            return "redirect:/login";
        }
        // Demo: reset to a temporary password. In production, send email with token.
        String temp = "123456";
        user.setPassword(temp);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "Temporary password has been set (demo): " + temp + ". Please sign in and change it.");
        return "redirect:/login";
    }

    // Logout endpoint
    @GetMapping("/logout")
    public String logout(RedirectAttributes redirectAttributes) {
        SecurityContextHolder.clearContext();
        redirectAttributes.addAttribute("logout", "true");
        return "redirect:/login";
    }
}