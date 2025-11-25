package g6.fashionFlex.controller;

import g6.fashionFlex.dto.*;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.security.CustomUserDetails;
import g6.fashionFlex.security.CustomOAuth2User;
import g6.fashionFlex.service.CartService;
import g6.fashionFlex.service.CouponService;
import g6.fashionFlex.service.ShippingService;
import g6.fashionFlex.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CartController {

      @Autowired
    private UserService userService;

    private final CartService cartService;
    private final CouponService couponService;
    private final ShippingService shippingService;


    @GetMapping("/shopping-cart")
    public String viewCart(Model model) {
        UserDTO user = getCurrentUser();
        CartDTO cart = createEmptyCart(); // Default to empty cart
        boolean isAuthenticated = false;

        if (user != null) {
            // User is authenticated
            isAuthenticated = true;
            model.addAttribute("user", user);

            try {
                Long userId = user.getId();
                // Try to get existing cart (without auto-creating)
                cart = cartService.getCartByUserId(userId);
            } catch (Exception e) {
                // Error loading cart or cart doesn't exist - use empty cart
                // This prevents "Connection is read-only" errors
                System.out.println("Could not load cart for user " + user.getId() + ": " + e.getMessage());
            }
        }

        model.addAttribute("cart", cart);
        model.addAttribute("isAuthenticated", isAuthenticated);

        try {
            model.addAttribute("supportedCountries", shippingService.getSupportedCountries());
        } catch (Exception e) {
            model.addAttribute("supportedCountries", new java.util.ArrayList<>());
        }

        return "shopping-cart";
    }

    /**
     * Create empty cart DTO for guest users
     */
    private CartDTO createEmptyCart() {
        CartDTO emptyCart = new CartDTO();
        emptyCart.setItems(new java.util.ArrayList<>());
        emptyCart.setSubtotal(java.math.BigDecimal.ZERO);
        emptyCart.setTotal(java.math.BigDecimal.ZERO);
        emptyCart.setTotalItems(0);
        emptyCart.setDiscountAmount(java.math.BigDecimal.ZERO);
        emptyCart.setHasOutOfStockItems(false);
        emptyCart.setStockWarnings(new java.util.ArrayList<>());
        return emptyCart;
    }

    /**
     * Add item to cart (AJAX)
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<?> addToCart(@Valid @RequestBody AddToCartRequest request) {
        try {
           UserDTO user = getCurrentUser();
            Long userId = user.getId();
            CartDTO cart = cartService.addToCart(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item added to cart successfully");
            response.put("cart", cart);
            response.put("cartItemCount", cart.getTotalItems());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred while adding item to cart");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update cart item quantity (AJAX)
     */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/api/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateCartItem(@Valid @RequestBody UpdateCartItemRequest request) {
        try {
           UserDTO user = getCurrentUser();
            Long userId = user.getId();
            CartDTO cart = cartService.updateCartItem(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart updated successfully");
            response.put("cart", cart);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred while updating cart");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Remove item from cart (AJAX)
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/api/cart/remove/{cartItemId}")
    @ResponseBody
    public ResponseEntity<?> removeCartItem(@PathVariable Long cartItemId) {
        try {
           UserDTO user = getCurrentUser();
            Long userId = user.getId();
            CartDTO cart = cartService.removeCartItem(userId, cartItemId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item removed from cart");
            response.put("cart", cart);
            response.put("cartItemCount", cart.getTotalItems());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred while removing item");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Clear cart
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/cart/clear")
    public String clearCart(RedirectAttributes redirectAttributes) {
        try {
           UserDTO user = getCurrentUser();
            Long userId = user.getId();
            cartService.clearCart(userId);
            redirectAttributes.addFlashAttribute("success", "Cart cleared successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to clear cart");
        }
        return "redirect:/cart";
    }

    /**
     * Apply coupon code (AJAX)
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/api/cart/coupon/apply")
    @ResponseBody
    public ResponseEntity<?> applyCoupon(@Valid @RequestBody ApplyCouponRequest request) {
        try {
            UserDTO user = getCurrentUser();
            Long userId = user.getId();

            // The service now handles calculation and saving
            CartDTO cart = cartService.applyCoupon(userId, request.getCouponCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Coupon applied successfully");
            response.put("cart", cart);
            response.put("discount", cart.getDiscountAmount()); // Get discount from the returned cart

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred while applying coupon");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Remove coupon (AJAX)
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/api/cart/coupon/remove")
    @ResponseBody
    public ResponseEntity<?> removeCoupon() {
        try {
           UserDTO user = getCurrentUser();
            Long userId = user.getId();
            CartDTO cart = cartService.removeCoupon(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Coupon removed");
            response.put("cart", cart);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred while removing coupon");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Shipping calculation moved to CheckoutController for better user experience

    // Shipping method selection moved to CheckoutController for better user experience

    // Shipping selection clearing moved to CheckoutController for better user experience

    /**
     * Validate cart stock before checkout (AJAX)
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/api/cart/validate")
    @ResponseBody
    public ResponseEntity<?> validateCart() {
        try {
           UserDTO user = getCurrentUser();
            Long userId = user.getId();
            List<String> warnings = cartService.validateCartStock(userId);
            CartDTO cart = cartService.getCartByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warnings", warnings);
            response.put("cart", cart);
            response.put("hasWarnings", !warnings.isEmpty());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred while validating cart");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Validate cart before proceeding to checkout
     * Checks: cart not empty, shipping selected, no out-of-stock items
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/api/cart/validate-checkout")
    @ResponseBody
    public ResponseEntity<?> validateCheckout() {
        try {
            UserDTO user = getCurrentUser();
            Long userId = user.getId();
            CartDTO cart = cartService.getCartByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            List<String> errors = new ArrayList<>();

            // Check if cart is empty
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                errors.add("Your cart is empty");
                response.put("success", false);
                response.put("valid", false);
                response.put("errors", errors);
                response.put("message", "Please add items to your cart before checkout");
                return ResponseEntity.ok(response);
            }

            // Note: Shipping method validation moved to checkout page
            // No longer require pre-selected shipping method for cart validation

            // Validate stock availability
            List<String> stockWarnings = cartService.validateCartStock(userId);
            if (!stockWarnings.isEmpty()) {
                response.put("success", false);
                response.put("valid", false);
                response.put("errors", stockWarnings);
                response.put("message", "Some items in your cart are out of stock or have insufficient quantity");
                return ResponseEntity.ok(response);
            }

            // All validations passed
            response.put("success", true);
            response.put("valid", true);
            response.put("message", "Cart is ready for checkout");
            response.put("cart", cart);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("valid", false);
            response.put("message", "An error occurred while validating checkout: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get cart item count (for header badge)
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/cart/count")
    @ResponseBody
    public ResponseEntity<?> getCartItemCount() {
        try {
           UserDTO user = getCurrentUser();
            Long userId = user.getId();
            int count = cartService.getCartItemCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("count", 0);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get current cart data (AJAX)
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/cart/data")
    @ResponseBody
    public ResponseEntity<?> getCartData() {
        try {

            UserDTO user = getCurrentUser();
            Long userId = user.getId();
            CartDTO cart = cartService.getCartByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cart", cart);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred while fetching cart data");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to get current user
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
}
