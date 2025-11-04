package g6.fashionFlex.controller;

import g6.fashionFlex.dto.AuthResponse;
import g6.fashionFlex.dto.LoginRequest;
import g6.fashionFlex.dto.RegisterRequest;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.security.JwtTokenProvider;
import g6.fashionFlex.service.UserService;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // Display login page
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                @RequestParam(required = false) String registered,
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
        model.addAttribute("user", new RegisterRequest());
        return "login";
    }

    // Handle registration from form submission
    @PostMapping("/api/auth/register")
    public String registerUser(@Valid @ModelAttribute("user") RegisterRequest registerRequest,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("registerError", "Please fix the errors in the form");
            return "login";
        }

        try {
            UserDTO userDTO = userService.registerUser(registerRequest);
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("registerError", e.getMessage());
            return "login";
        }
    }

    // REST API endpoint for registration (for AJAX calls)
    @PostMapping("/api/auth/register-json")
    @ResponseBody
    public ResponseEntity<?> registerUserJson(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            UserDTO userDTO = userService.registerUser(registerRequest);
            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "Registration successful",
                    "user", userDTO
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
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
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Redirect to user home after successful login
            return "redirect:/user/home";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "true");
            return "redirect:/login";
        }
    }

    @GetMapping("/user/home")
    public String userHome(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            try {
                UserDTO user = userService.getUserByEmail(email);
                model.addAttribute("user", user);
            } catch (Exception e) {
                // User not found, continue without user details
            }
        }
        return "user_home";
    }

    // Logout endpoint
    @GetMapping("/logout")
    public String logout(RedirectAttributes redirectAttributes) {
        SecurityContextHolder.clearContext();
        redirectAttributes.addAttribute("logout", "true");
        return "redirect:/login";
    }

    // Helper method to create response map
    private static class Map<K, V> {
        private final java.util.Map<K, V> map = new java.util.HashMap<>();

        public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2) {
            java.util.Map<K, V> map = new java.util.HashMap<>();
            map.put(k1, v1);
            map.put(k2, v2);
            return map;
        }

        public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
            java.util.Map<K, V> map = new java.util.HashMap<>();
            map.put(k1, v1);
            map.put(k2, v2);
            map.put(k3, v3);
            return map;
        }
    }
}
