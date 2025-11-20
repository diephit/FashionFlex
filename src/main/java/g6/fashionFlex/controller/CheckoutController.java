package g6.fashionFlex.controller;

import g6.fashionFlex.dto.*;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.service.AddressService;
import g6.fashionFlex.service.CartService;
import g6.fashionFlex.service.OrderService;
import g6.fashionFlex.service.UserService;
import g6.fashionFlex.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Slf4j
public class CheckoutController {

    private final CartService cartService;
    private final AddressService addressService;
    private final OrderService orderService;
    private final UserService userService;
    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;

    /**
     * Display checkout page
     */
    @GetMapping
    public String showCheckoutPage(Model model, RedirectAttributes redirectAttributes) {
        try {
            log.info("Accessing checkout page");

            UserDTO currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("User not logged in, redirecting to login");
                redirectAttributes.addFlashAttribute("error", "Please login to checkout");
                return "redirect:/login";
            }

            log.info("User {} accessing checkout", currentUser.getEmail());

            // Get cart
            CartDTO cart = cartService.getCartByUserId(currentUser.getId());
            log.info("Cart retrieved: items={}, shipping={}, outOfStock={}",
                    cart != null ? cart.getTotalItems() : "null",
                    cart != null ? cart.getHasShippingMethod() : "null",
                    cart != null ? cart.getHasOutOfStockItems() : "null");

        // Validate cart is not empty
        if (cart == null || cart.getTotalItems() == null || cart.getTotalItems() == 0) {
            redirectAttributes.addFlashAttribute("error", "Your cart is empty");
            return "redirect:/shopping-cart";
        }

        // Validate shipping method is selected
        if (cart.getHasShippingMethod() == null || !cart.getHasShippingMethod()) {
            redirectAttributes.addFlashAttribute("error", "Please select a shipping method first");
            return "redirect:/shopping-cart";
        }

        // Validate no out of stock items
        if (cart.getHasOutOfStockItems() != null && cart.getHasOutOfStockItems()) {
            redirectAttributes.addFlashAttribute("error", "Some items in your cart are out of stock");
            return "redirect:/shopping-cart";
        }

            // Get user's saved addresses
            List<AddressDTO> addresses = addressService.findByUserId(currentUser.getId());
            log.info("Found {} saved addresses", addresses.size());

            // Add data to model
            model.addAttribute("cart", cart);
            model.addAttribute("addresses", addresses);
            model.addAttribute("user", currentUser);
            model.addAttribute("checkoutRequest", new CheckoutRequest());
            model.addAttribute("paymentMethods", Order.PaymentMethod.values());

            log.info("Rendering checkout page");
            return "checkout/checkout";

        } catch (Exception e) {
            log.error("Error loading checkout page", e);
            redirectAttributes.addFlashAttribute("error", "Failed to load checkout: " + e.getMessage());
            return "redirect:/shopping-cart";
        }
    }

    /**
     * Process checkout and place order
     */
    @PostMapping("/place-order")
    public String placeOrder(@Valid @ModelAttribute("checkoutRequest") CheckoutRequest request,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest httpRequest) {
        UserDTO currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to place order");
            return "redirect:/login";
        }

        // Validation errors
        if (bindingResult.hasErrors()) {
            // Reload page with errors
            CartDTO cart = cartService.getCartByUserId(currentUser.getId());
            List<AddressDTO> addresses = addressService.findByUserId(currentUser.getId());

            model.addAttribute("cart", cart);
            model.addAttribute("addresses", addresses);
            model.addAttribute("user", currentUser);
            model.addAttribute("paymentMethods", Order.PaymentMethod.values());

            return "checkout/checkout";
        }

        OrderConfirmationDTO confirmation = null;
        try {
            log.info("Starting place order for user: {}", currentUser.getEmail());

            // Get cart
            CartDTO cart = cartService.getCartByUserId(currentUser.getId());
            log.info("Cart retrieved: items={}, total={}", cart.getTotalItems(), cart.getTotal());

            // Build place order request
            PlaceOrderRequest placeOrderRequest = buildPlaceOrderRequest(request, cart, currentUser);
            log.info("Place order request built: shipping={}, payment={}, total={}",
                    placeOrderRequest.getShippingAddress(),
                    placeOrderRequest.getPaymentMethod(),
                    placeOrderRequest.getTotalAmount());

            // Step 1: Place the order (core transactional operation)
            confirmation = orderService.placeOrder(placeOrderRequest);
            log.info("Order placed successfully: orderId={}, orderNumber={}",
                    confirmation.getOrderId(), confirmation.getOrderNumber());

        } catch (IllegalStateException e) {
            log.error("Illegal state when placing order", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/shopping-cart";
        } catch (Exception e) {
            log.error("Failed to place order - Full exception:", e);
            redirectAttributes.addFlashAttribute("error", "Failed to place order: " + e.getMessage());
            return "redirect:/checkout";
        }

        // --- Post-Order Operations (outside the main transaction) ---

        // Step 2: Save address if requested (non-critical)
        if (request.isSaveAddress()) {
            try {
                AddressDTO newAddress = new AddressDTO();
                newAddress.setFullName(request.getFullName());
                newAddress.setPhoneNumber(request.getPhoneNumber());
                newAddress.setStreet(request.getStreet());
                newAddress.setCity(request.getCity());
                newAddress.setState(request.getState());
                newAddress.setZipCode(request.getZipCode());
                newAddress.setCountry(request.getCountry());
                addressService.save(newAddress, currentUser.getId());
                log.info("Saved new address for user {}", currentUser.getEmail());
            } catch (Exception e) {
                log.error("Non-critical error: Failed to save new address for user {} after order placement.", currentUser.getEmail(), e);
            }
        }

        // Step 3: Clear the cart (non-critical, but important)
        try {
            cartService.clearCart(currentUser.getId());
            log.info("Cart cleared for user {}", currentUser.getEmail());
        } catch (Exception e) {
            log.error("Non-critical error: Failed to clear cart for user {} after order placement.", currentUser.getEmail(), e);
        }

        // Step 4: Check payment method and redirect accordingly
        if ("VNPAY".equals(request.getPaymentMethod())) {
            // For VNPay payment, redirect to VNPay payment gateway
            try {
                log.info("Redirecting to VNPay payment for order: {}", confirmation.getOrderId());
                Order order = orderRepository.findById(confirmation.getOrderId())
                        .orElseThrow(() -> new IllegalArgumentException("Order not found"));
                String paymentUrl = vnPayService.createPaymentUrl(order, httpRequest);
                return "redirect:" + paymentUrl;
            } catch (Exception e) {
                log.error("Failed to create VNPay payment URL", e);
                redirectAttributes.addFlashAttribute("error", "Failed to initiate VNPay payment: " + e.getMessage());
                return "redirect:/checkout/confirmation/" + confirmation.getOrderId();
            }
        } else {
            // For other payment methods (COD, etc.), redirect to confirmation page
            redirectAttributes.addFlashAttribute("orderConfirmation", confirmation);
            return "redirect:/checkout/confirmation/" + confirmation.getOrderId();
        }
    }

    /**
     * Show order confirmation page
     */
    @GetMapping("/confirmation/{orderId}")
    public String showConfirmation(@PathVariable Long orderId, Model model, RedirectAttributes redirectAttributes) {
        UserDTO currentUser = getCurrentUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to view order");
            return "redirect:/login";
        }

        try {
            OrderConfirmationDTO confirmation = orderService.getOrderConfirmation(orderId, currentUser.getId());
            model.addAttribute("orderConfirmation", confirmation);
            return "checkout/order-confirmation";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Order not found");
            return "redirect:/user/orders";
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Build PlaceOrderRequest from CheckoutRequest and Cart
     */
    private PlaceOrderRequest buildPlaceOrderRequest(CheckoutRequest request, CartDTO cart, UserDTO user) {
        PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();

        placeOrderRequest.setUserId(user.getId());
        placeOrderRequest.setCartId(cart.getId());

        // Shipping information
        if (request.getSavedAddressId() != null) {
            // Use saved address
            AddressDTO savedAddress = addressService.findById(request.getSavedAddressId());
            placeOrderRequest.setShippingName(savedAddress.getFullName());
            placeOrderRequest.setShippingPhone(savedAddress.getPhoneNumber());
            placeOrderRequest.setShippingAddress(savedAddress.getStreet());
            placeOrderRequest.setShippingCity(savedAddress.getCity());
            placeOrderRequest.setShippingState(savedAddress.getState());
            placeOrderRequest.setShippingZipCode(savedAddress.getZipCode());
            placeOrderRequest.setShippingCountry(savedAddress.getCountry());
        } else {
            // Use new address from form
            placeOrderRequest.setShippingName(request.getFullName());
            placeOrderRequest.setShippingPhone(request.getPhoneNumber());
            placeOrderRequest.setShippingAddress(request.getStreet());
            placeOrderRequest.setShippingCity(request.getCity());
            placeOrderRequest.setShippingState(request.getState());
            placeOrderRequest.setShippingZipCode(request.getZipCode());
            placeOrderRequest.setShippingCountry(request.getCountry());

            // Save address if requested
            if (request.isSaveAddress()) {
                placeOrderRequest.setSaveAddress(true);
                placeOrderRequest.setFullName(request.getFullName());
                placeOrderRequest.setPhoneNumber(request.getPhoneNumber());
                placeOrderRequest.setStreet(request.getStreet());
                placeOrderRequest.setCity(request.getCity());
                placeOrderRequest.setState(request.getState());
                placeOrderRequest.setZipCode(request.getZipCode());
                placeOrderRequest.setCountry(request.getCountry());
            }
        }

        // Payment method
        placeOrderRequest.setPaymentMethod(request.getPaymentMethod());

        // Order totals from cart
        placeOrderRequest.setSubtotal(cart.getSubtotal());
        placeOrderRequest.setShippingCost(cart.getShippingCost() != null ? cart.getShippingCost() : java.math.BigDecimal.ZERO);
        placeOrderRequest.setTax(cart.getTaxAmount() != null ? cart.getTaxAmount() : java.math.BigDecimal.ZERO);
        placeOrderRequest.setDiscount(cart.getDiscountAmount() != null ? cart.getDiscountAmount() : java.math.BigDecimal.ZERO);
        placeOrderRequest.setTotalAmount(cart.getFinalTotal() != null ? cart.getFinalTotal() : cart.getTotal());

        // Coupon
        placeOrderRequest.setCouponCode(cart.getCouponCode());

        // Notes
        placeOrderRequest.setNotes(request.getNotes());

        return placeOrderRequest;
    }

    /**
     * Get current logged-in user
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
