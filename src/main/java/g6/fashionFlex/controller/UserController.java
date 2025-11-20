package g6.fashionFlex.controller;

import g6.fashionFlex.dto.*;
import g6.fashionFlex.service.FileUploadService;
import g6.fashionFlex.service.UserService;
import g6.fashionFlex.service.WishlistService;
import g6.fashionFlex.service.OrderService;
import g6.fashionFlex.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AddressService addressService;

    /**
     * Get the current authenticated user
     */
    private UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            String email = authentication.getName();
            return userService.getUserByEmail(email);
        }
        return null;
    }

    /**
     * My Account page - Overview of user account
     */
    @GetMapping("/my-account")
    public String myAccount(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);

        try {
            // Get real counts for user statistics
            long orderCount = orderService.getUserOrderCount(user.getId());
            long wishlistCount = wishlistService.getWishlistCount(user.getId());
            long addressCount = addressService.getUserAddressCount(user.getId());

            model.addAttribute("orderCount", orderCount);
            model.addAttribute("wishlistCount", wishlistCount);
            model.addAttribute("addressCount", addressCount);
        } catch (Exception e) {
            // Fallback to 0 if services are not available
            model.addAttribute("orderCount", 0);
            model.addAttribute("wishlistCount", 0);
            model.addAttribute("addressCount", 0);
        }

        return "user/my-account";
    }

    /**
     * Profile Edit page
     */
    @GetMapping("/profile-edit")
    public String profileEdit(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        return "user/profile-edit";
    }

    /**
     * Update user profile
     */
    @PostMapping("/profile/update")
    public String profileUpdate(@ModelAttribute UpdateProfileRequest request,
                                @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                                RedirectAttributes redirectAttributes) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return "redirect:/login";
            }

            // Handle profile image upload if provided
            if (profileImage != null && !profileImage.isEmpty()) {
                try {
                    String imageUrl = fileUploadService.uploadFile(profileImage, "profiles");
                    request.setProfileImageUrl(imageUrl);

                    // Delete old profile image if exists
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        fileUploadService.deleteFile(user.getProfileImageUrl());
                    }
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Error uploading profile image: " + e.getMessage());
                    return "redirect:/user/profile-edit";
                }
            }

            userService.updateProfile(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
            return "redirect:/user/my-account";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
            return "redirect:/user/profile-edit";
        }
    }

    /**
     * Change password
     */
    @PostMapping("/password/change")
    public String changePassword(@ModelAttribute ChangePasswordRequest request, RedirectAttributes redirectAttributes) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return "redirect:/login";
            }

            userService.changePassword(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully");
            return "redirect:/user/profile-edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/profile-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error changing password: " + e.getMessage());
            return "redirect:/user/profile-edit";
        }
    }

    /**
     * Order History page
     */
    @GetMapping("/order-history")
    public String orderHistory(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        // TODO: Add orders list when OrderService is implemented
        // List<Order> orders = orderService.getOrdersByUserId(user.getId());
        // model.addAttribute("orders", orders);
        return "user/order-history";
    }

    /**
     * Order Tracking page
     */
    @GetMapping("/order-tracking")
    public String orderTracking(@RequestParam(required = false) String orderNumber, Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);

        if (orderNumber != null) {
            // TODO: Fetch order details when OrderService is implemented
            // Order order = orderService.getOrderByNumber(orderNumber);
            // model.addAttribute("order", order);
            model.addAttribute("orderNumber", orderNumber);
        }

        return "user/order-tracking";
    }

    /**
     * Address Book page
     */
    @GetMapping("/address-book")
    public String addressBook(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        // TODO: Add addresses list when Address entity is implemented
        // List<Address> addresses = addressService.getAddressesByUserId(user.getId());
        // model.addAttribute("addresses", addresses);
        return "user/address-book";
    }

    /**
     * Wishlist page
     */
    @GetMapping("/wishlist")
    public String wishlist(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);

        // Load wishlist items
        List<WishlistDTO> wishlistItems = wishlistService.getWishlistByUserId(user.getId());
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("wishlistCount", wishlistItems.size());

        return "user/wishlist";
    }

    // ==================== Wishlist REST API Endpoints ====================

    /**
     * Get all wishlist items (AJAX)
     */
    @GetMapping("/api/wishlist/items")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getWishlistItems() {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
            }

            List<WishlistDTO> wishlistItems = wishlistService.getWishlistByUserId(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("wishlistItems", wishlistItems);
            response.put("count", wishlistItems.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Add product to wishlist (AJAX)
     */
    @PostMapping("/api/wishlist/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToWishlist(@Valid @RequestBody AddToWishlistRequest request) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
            }

            WishlistDTO wishlistItem = wishlistService.addToWishlist(user.getId(), request.getProductId());
            long count = wishlistService.getWishlistCount(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product added to wishlist");
            response.put("wishlistItem", wishlistItem);
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Error adding to wishlist: " + e.getMessage()));
        }
    }

    /**
     * Remove product from wishlist (AJAX)
     */
    @DeleteMapping("/api/wishlist/remove/{wishlistId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromWishlist(@PathVariable Long wishlistId) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
            }

            wishlistService.removeFromWishlist(user.getId(), wishlistId);
            long count = wishlistService.getWishlistCount(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product removed from wishlist");
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Remove product from wishlist by product ID (AJAX)
     */
    @DeleteMapping("/api/wishlist/remove-by-product/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromWishlistByProductId(@PathVariable Long productId) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
            }

            wishlistService.removeFromWishlistByProductId(user.getId(), productId);
            long count = wishlistService.getWishlistCount(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product removed from wishlist");
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Check if product is in wishlist (AJAX)
     */
    @GetMapping("/api/wishlist/check/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkInWishlist(@PathVariable Long productId) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("inWishlist", false);
                return ResponseEntity.ok(response);
            }

            boolean inWishlist = wishlistService.isInWishlist(user.getId(), productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inWishlist", inWishlist);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get wishlist count (AJAX)
     */
    @GetMapping("/api/wishlist/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getWishlistCount() {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("count", 0);
                return ResponseEntity.ok(response);
            }

            long count = wishlistService.getWishlistCount(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Move wishlist item to cart (AJAX)
     */
    @PostMapping("/api/wishlist/move-to-cart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> moveToCart(@Valid @RequestBody MoveToCartRequest request) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
            }

            CartDTO cart = wishlistService.moveToCart(user.getId(), request);
            long wishlistCount = wishlistService.getWishlistCount(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product moved to cart successfully");
            response.put("cart", cart);
            response.put("wishlistCount", wishlistCount);
            response.put("cartItemCount", cart.getTotalItems());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Error moving to cart: " + e.getMessage()));
        }
    }

    /**
     * Clear all wishlist items (AJAX)
     */
    @DeleteMapping("/api/wishlist/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearWishlist() {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
            }

            wishlistService.clearWishlist(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wishlist cleared successfully");
            response.put("count", 0);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
