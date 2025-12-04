package g6.fashionFlex.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.service.EmailService;
import g6.fashionFlex.service.FileStorageService;
import g6.fashionFlex.service.PasswordSetupService;
import jakarta.servlet.http.HttpSession;

/**
 * AccountController handles user account-related operations such as:
 * - Profile management (view and edit)
 * - Order history viewing
 * - Password management and verification
 * - Email verification for password setup
 */
@Controller
public class AccountController extends BaseController {

    // ===== AUTOWIRED DEPENDENCIES =====
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordSetupService passwordSetupService;

    @Autowired
    private FileStorageService fileStorageService;

    // ===== SESSION KEYS =====
    private static final String SESSION_VERIFIED_KEY = "changePasswordVerifiedUserId";
    private static final String SESSION_SUGGESTION_KEY = "changePasswordSuggestion";

    // ===== ACCOUNT ENDPOINTS =====

    /**
     * Redirects /my-account requests to /profile
     * Checks if user is authenticated before redirecting
     */
    @GetMapping("/my-account")
    public String myAccount(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        addAuthenticationToModel(model, session);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = getAuthenticatedUser(auth);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "You must sign in first to view your profile");
            return "redirect:/login";
        }
        return "redirect:/profile";
    }

    /**
     * Displays the user's profile page
     * Loads user and customer information
     * Auto-creates Customer profile for OAuth users if not exists
     */
    @GetMapping("/profile")
    public String profile(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        addAuthenticationToModel(model, session);
        
        // Check if user is authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        
        Optional<User> userOpt = getAuthenticatedUser(auth);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        
        // Store user info in session if not already present
        if (session.getAttribute("loggedInUser") == null) {
            session.setAttribute("loggedInUser", user);
            session.setAttribute("userId", user.getUserID());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getName());
        }

        // Fetch or create Customer profile
        Optional<Customer> customerOpt = customerRepository.findByUser(user);
        Customer customer;
        if (customerOpt.isEmpty()) {
            // Auto-create Customer for OAuth users if not exists
            customer = new Customer();
            customer.setUser(user);
            customer.setLoyaltyPoints(0);
            customer.setTotalSpent(BigDecimal.ZERO);
            customer = customerRepository.save(customer);
        } else {
            customer = customerOpt.get();
        }
        
        model.addAttribute("customer", customer);
        model.addAttribute("user", user);
        return "profile-edit";
    }
    
    /**
     * Legacy endpoint - redirects /profile-edit to /profile
     */
    @GetMapping("/profile-edit")
    public String profileEdit() {
        return "redirect:/profile";
    }
    
    // ===== ORDER HISTORY ENDPOINT =====

    /**
     * Displays user's order history with filtering capability
     * Supports filtering by order status (processing, shipped, completed, cancelled)
     * Aggregates orders from multiple sources for OAuth users
     */
    @GetMapping("/order-history")
    public String orderHistory(@RequestParam(value = "status", required = false) String statusParam,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        addAuthenticationToModel(model, session);

        // Check authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }

        Optional<User> userOpt = getAuthenticatedUser(auth);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        
        User user = userOpt.get();

        // Fetch or create Customer profile
        Optional<Customer> customerOpt = customerRepository.findByUser(user);
        Customer customer;
        if (customerOpt.isEmpty()) {
            // Auto-create Customer for OAuth users if not exists
            customer = new Customer();
            customer.setUser(user);
            customer.setLoyaltyPoints(0);
            customer.setTotalSpent(BigDecimal.ZERO);
            customer = customerRepository.save(customer);
        } else {
            customer = customerOpt.get();
        }

        // Retrieve orders from multiple sources to ensure complete order history
        List<Order> ordersByCustomer = new ArrayList<>();
        if (customer != null && customer.getCustomerID() != null) {
            ordersByCustomer = orderRepository.findOrdersWithItemsByCustomer(customer.getCustomerID());
        }
        
        List<Order> ordersByUser = new ArrayList<>();
        if (user != null && user.getUserID() != null) {
            ordersByUser = orderRepository.findByUserOrderByOrderDateDesc(user.getUserID());
        }
        
        List<Order> ordersByEmail = new ArrayList<>();
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            ordersByEmail = orderRepository.findByContactEmailWithItems(user.getEmail());
        }
        
        // Merge and deduplicate orders from all sources
        Set<Integer> orderIds = new HashSet<>();
        List<Order> orders = new ArrayList<>();
        
        for (Order order : ordersByCustomer) {
            if (order != null && order.getOrderID() != null && orderIds.add(order.getOrderID())) {
                orders.add(order);
            }
        }
        for (Order order : ordersByUser) {
            if (order != null && order.getOrderID() != null && orderIds.add(order.getOrderID())) {
                orders.add(order);
            }
        }
        for (Order order : ordersByEmail) {
            if (order != null && order.getOrderID() != null && orderIds.add(order.getOrderID())) {
                orders.add(order);
            }
        }
        
        // Sort orders by date descending (newest first)
        orders.sort((o1, o2) -> {
            if (o1.getOrderDate() == null && o2.getOrderDate() == null) return 0;
            if (o1.getOrderDate() == null) return 1;
            if (o2.getOrderDate() == null) return -1;
            return o2.getOrderDate().compareTo(o1.getOrderDate());
        });

        // Count orders by status
        Map<Order.OrderStatus, Long> statusCounts = new EnumMap<>(Order.OrderStatus.class);
        for (Order.OrderStatus orderStatus : Order.OrderStatus.values()) {
            long count = orders.stream()
                    .filter(order -> order.getStatus() == orderStatus)
                    .count();
            statusCounts.put(orderStatus, count);
        }

        // Parse and apply status filter if provided
        String selectedStatusKey = null;
        Set<Order.OrderStatus> filterStatuses = null;
        if (statusParam != null && !statusParam.isBlank()) {
            selectedStatusKey = statusParam.trim().toLowerCase();
            filterStatuses = switch (selectedStatusKey) {
                case "processing" -> EnumSet.of(Order.OrderStatus.pending, Order.OrderStatus.paid);
                case "shipped" -> EnumSet.of(Order.OrderStatus.shipped);
                case "completed" -> EnumSet.of(Order.OrderStatus.completed);
                case "cancelled", "canceled" -> EnumSet.of(Order.OrderStatus.canceled);
                default -> {
                    try {
                        Order.OrderStatus enumStatus = Order.OrderStatus.valueOf(selectedStatusKey);
                        yield EnumSet.of(enumStatus);
                    } catch (IllegalArgumentException ex) {
                        yield null;
                    }
                }
            };
        }

        final Set<Order.OrderStatus> activeStatuses = filterStatuses;

        // Filter orders based on selected status
        List<Order> filteredOrders = activeStatuses == null
                ? orders
                : orders.stream()
                        .filter(order -> activeStatuses.contains(order.getStatus()))
                        .collect(Collectors.toList());

        // Calculate total amount spent
        BigDecimal totalSpent = orders.stream()
                .map(order -> BigDecimal.valueOf(order.getTotalAmount() == null ? 0.0 : order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get count for each status group
        long processingCount = statusCounts.getOrDefault(Order.OrderStatus.pending, 0L)
                + statusCounts.getOrDefault(Order.OrderStatus.paid, 0L);
        long shippedCount = statusCounts.getOrDefault(Order.OrderStatus.shipped, 0L);
        long completedCount = statusCounts.getOrDefault(Order.OrderStatus.completed, 0L);
        long canceledCount = statusCounts.getOrDefault(Order.OrderStatus.canceled, 0L);

        // Add data to model
        model.addAttribute("orders", filteredOrders);
        model.addAttribute("selectedStatus", selectedStatusKey);
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("processingCount", processingCount);
        model.addAttribute("shippedCount", shippedCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("canceledCount", canceledCount);

        return "order-history";
    }
    
    // ===== PROFILE UPDATE ENDPOINT =====

    /**
     * Updates user profile information including:
     * - Full name
     * - Phone number
     * - Date of birth
     * - Profile picture/avatar
     * 
     * Validates file upload and handles errors appropriately
     */
    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String name,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String dateOfBirth,
                                @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                                RedirectAttributes redirectAttributes) {
        
        // Check if user is authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        
        Optional<User> userOpt = getAuthenticatedUser(auth);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        
        // Fetch or create Customer profile
        Optional<Customer> customerOpt = customerRepository.findByUser(user);
        Customer customer;
        if (customerOpt.isEmpty()) {
            // Auto-create Customer for OAuth users if not exists
            customer = new Customer();
            customer.setUser(user);
            customer.setLoyaltyPoints(0);
            customer.setTotalSpent(BigDecimal.ZERO);
            customer = customerRepository.save(customer);
        } else {
            customer = customerOpt.get();
        }
        
        // Update user name if provided
        if (name != null && !name.trim().isEmpty()) {
            user.setName(name.trim());
        }
        
        // Update phone number if provided
        if (phone != null) {
            String normalizedPhone = phone.trim();
            if (normalizedPhone.isEmpty()) {
                customer.setPhone(null);
            } else {
                if (!normalizedPhone.matches("^0\\d{9}$")) {
                    redirectAttributes.addFlashAttribute("error", "Phone number must start with 0 and contain exactly 10 digits.");
                    return "redirect:/profile";
                }
                boolean phoneTaken = customerRepository.existsByPhoneAndCustomerIDNot(normalizedPhone, customer.getCustomerID());
                if (phoneTaken) {
                    redirectAttributes.addFlashAttribute("error", "This phone number has been used, please try another one.");
                    return "redirect:/profile";
                }
                customer.setPhone(normalizedPhone);
            }
        }
        
        // Update date of birth if provided
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            try {
                customer.setDateOfBirth(java.time.LocalDate.parse(dateOfBirth));
            } catch (Exception e) {
                // Log parsing error if needed
            }
        }

        // Handle profile image upload
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // Validate file size (2MB limit = 2097152 bytes)
                if (profileImage.getSize() > 2097152) {
                    redirectAttributes.addFlashAttribute("error", "File size exceeds 2MB limit.");
                    return "redirect:/profile";
                }
                
                // Validate file type (must be image)
                String contentType = profileImage.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Please upload a valid image file.");
                    return "redirect:/profile";
                }
                
                // Store the image file and get the path
                String avatarPath = fileStorageService.storeCustomerImage(profileImage);
                
                // Verify that file was saved successfully
                if (avatarPath != null && !avatarPath.isEmpty()) {
                    customer.setCustomerImg(avatarPath);
                } else {
                    redirectAttributes.addFlashAttribute("error", "Failed to save image. Please try again.");
                    return "redirect:/profile";
                }
            } catch (Exception ex) {
                // Log exception for debugging
                ex.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Unable to upload profile image: " + ex.getMessage());
                return "redirect:/profile";
            }
        }
        
        // Save updated user and customer records to database
        userRepository.save(user);
        customerRepository.save(customer);
        
        redirectAttributes.addFlashAttribute("message", "Profile updated successfully.");
        return "redirect:/profile";
    }

    // ===== PASSWORD MANAGEMENT ENDPOINTS =====

    /**
     * Legacy endpoint - redirects /profile/change-password to /change-password
     */
    @GetMapping("/profile/change-password")
    public String redirectLegacyChangePassword() {
        return "redirect:/change-password";
    }

    /**
     * Legacy POST endpoint - redirects /profile/change-password to /change-password
     */
    @PostMapping("/profile/change-password")
    public String redirectLegacyChangePasswordPost() {
        return "redirect:/change-password";
    }

    /**
     * Displays the change password page
     * Shows different UI based on:
     * - Whether user has an existing password (OAuth users might not)
     * - Whether email has been verified
     * - Provides suggested password for passwordless users
     */
    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpSession session,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        addAuthenticationToModel(model, session);

        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to continue.");
            return "redirect:/login";
        }
        
        User user = userOpt.get();

        // Check if user has an existing password
        boolean hasPassword = hasExistingPassword(user);
        
        // Check if email has been verified in current session
        boolean emailVerified = isEmailVerifiedForUser(session, user);

        // Clear verification if user already has a password
        if (hasPassword && emailVerified) {
            session.removeAttribute(SESSION_VERIFIED_KEY);
            session.removeAttribute(SESSION_SUGGESTION_KEY);
            emailVerified = false;
        }

        // Generate or retrieve suggested password if email is verified
        if (emailVerified) {
            String suggestion = (String) session.getAttribute(SESSION_SUGGESTION_KEY);
            if (suggestion == null || suggestion.isBlank()) {
                suggestion = passwordSetupService.generateSuggestedPassword();
                session.setAttribute(SESSION_SUGGESTION_KEY, suggestion);
            }
            model.addAttribute("suggestedPassword", suggestion);
        }

        // Set flags for template to render appropriate UI
        model.addAttribute("requiresVerification", !hasPassword && !emailVerified);
        model.addAttribute("showPasswordForm", hasPassword || emailVerified);
        model.addAttribute("passwordlessUser", !hasPassword);

        return "change-password";
    }

    /**
     * Processes password change request
     * Validates:
     * - Password length (minimum 6 characters)
     * - Password confirmation matches
     * - User verification (for passwordless users, email must be verified first)
     */
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please sign in to continue.");
            return "redirect:/login";
        }
        
        User user = userOpt.get();

        // Validate password length
        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters.");
            return "redirect:/change-password";
        }
        
        // Validate password confirmation matches
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Password confirmation does not match.");
            return "redirect:/change-password";
        }

        // For passwordless users, require email verification first
        boolean hasPassword = hasExistingPassword(user);
        if (!hasPassword && !isEmailVerifiedForUser(session, user)) {
            redirectAttributes.addFlashAttribute("error", "Please verify via the email link before setting a password.");
            return "redirect:/change-password";
        }

        // Encode and save new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Clear verification session attributes
        session.removeAttribute(SESSION_VERIFIED_KEY);
        session.removeAttribute(SESSION_SUGGESTION_KEY);

        redirectAttributes.addFlashAttribute("message", "Password updated successfully.");
        return "redirect:/change-password";
    }

    /**
     * Sends password setup verification link via email
     * Only for users without existing password (OAuth users)
     */
    @PostMapping("/change-password/send-link")
    public String sendPasswordSetupLink(HttpSession session, RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please sign in to continue.");
            return "redirect:/login";
        }
        
        User user = userOpt.get();

        // Check if user already has a password
        if (hasExistingPassword(user)) {
            redirectAttributes.addFlashAttribute("error", "You already have a password set for this account.");
            return "redirect:/change-password";
        }

        // Generate verification token
        String token = passwordSetupService.generateToken(user.getEmail());
        
        // Build verification link
        String verificationLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/change-password/verify")
                .queryParam("token", token)
                .build()
                .toUriString();

        // Compose email
        String subject = "Verify your email to set a Fashion Flex password";
        String body = "Hi " + (user.getName() != null ? user.getName() : "there") + ",\n\n"
                + "You requested to set a password for your Fashion Flex account.\n"
                + "Please click the secure link below to verify your email and continue:\n\n"
                + verificationLink + "\n\n"
                + "The link will expire in 30 minutes.\n"
                + "If you did not make this request, you can ignore this email.\n\n"
                + "Fashion Flex Team";

        try {
            // Send verification email
            emailService.sendSimpleMessage(user.getEmail(), subject, body);
            redirectAttributes.addFlashAttribute("message", "We sent a verification link to " + user.getEmail() + ". Please check your inbox.");
        } catch (MailException e) {
            redirectAttributes.addFlashAttribute("error", "Unable to send verification email right now. Please try again later.");
        }

        return "redirect:/change-password";
    }

    /**
     * Verifies the email verification link sent to user
     * On successful verification, sets session flag allowing user to set password
     */
    @GetMapping("/change-password/verify")
    public String verifyPasswordSetup(@RequestParam("token") String token,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        
        // Validate and consume token (one-time use)
        Optional<String> emailOpt = passwordSetupService.consumeToken(token);
        if (emailOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "The verification link is invalid or has expired.");
            return "redirect:/change-password";
        }

        // Find user by email from token
        Optional<User> tokenUserOpt = userRepository.findByEmail(emailOpt.get());
        if (tokenUserOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Account not found for the verification link.");
            return "redirect:/change-password";
        }

        // Ensure logged-in user matches token user (security check)
        Optional<User> loggedUserOpt = getAuthenticatedUser();
        if (loggedUserOpt.isEmpty() || !loggedUserOpt.get().getUserID().equals(tokenUserOpt.get().getUserID())) {
            redirectAttributes.addFlashAttribute("error", "Please login with the same account before verifying the link.");
            return "redirect:/login";
        }

        // Set verification flag in session
        User user = tokenUserOpt.get();
        session.setAttribute(SESSION_VERIFIED_KEY, user.getUserID());
        session.setAttribute(SESSION_SUGGESTION_KEY, passwordSetupService.generateSuggestedPassword());

        redirectAttributes.addFlashAttribute("message", "Email verified successfully. You can now set a password.");
        return "redirect:/change-password";
    }

    // ===== HELPER METHODS =====

    /**
     * Checks if user has an existing password (not placeholder)
     * Used to differentiate between OAuth users and regular users
     * 
     * @param user the user to check
     * @return true if user has a valid password, false otherwise
     */
    private boolean hasExistingPassword(User user) {
        if (user == null) {
            return false;
        }
        String password = user.getPassword();
        if (password == null || password.isBlank()) {
            return false;
        }
        // Check if password is not a placeholder (used for OAuth users)
        return !PasswordSetupService.isPlaceholderPassword(password);
    }

    /**
     * Checks if user's email has been verified in current session
     * Used for password setup verification flow
     * 
     * @param session the HTTP session
     * @param user the user to check
     * @return true if email is verified for this user in session
     */
    private boolean isEmailVerifiedForUser(HttpSession session, User user) {
        if (session == null || user == null || user.getUserID() == null) {
            return false;
        }
        Integer verifiedUserId = (Integer) session.getAttribute(SESSION_VERIFIED_KEY);
        return verifiedUserId != null && verifiedUserId.equals(user.getUserID());
    }
}