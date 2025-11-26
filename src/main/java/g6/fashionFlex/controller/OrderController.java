package g6.fashionFlex.controller;

import g6.fashionFlex.dto.OrderDTO;
import g6.fashionFlex.dto.OrderTrackingDTO;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.service.OrderService;
import g6.fashionFlex.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/user/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * Display order history page
     */
    @GetMapping
    public String orderHistory(@RequestParam(required = false, defaultValue = "ALL") String status,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        // Get user from authentication (GlobalControllerAdvice already adds user to model)
        UserDTO currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to view orders");
            return "redirect:/login";
        }

        try {
            List<OrderDTO> orders;

            if ("ALL".equalsIgnoreCase(status)) {
                orders = orderService.getOrdersByUserId(currentUser.getId());
                model.addAttribute("filterStatus", "ALL");
            } else {
                try {
                    Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                    orders = orderService.getOrdersByUserIdAndStatus(currentUser.getId(), orderStatus);
                    model.addAttribute("filterStatus", status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid order status: " + status);
                    return "redirect:/user/orders";
                }
            }

            model.addAttribute("orders", orders);
            model.addAttribute("orderStatuses", Order.OrderStatus.values());

            return "user/order-history";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to load orders: " + e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * Display order details page
     */
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        UserDTO currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to view order");
            return "redirect:/login";
        }

        try {
            OrderDTO order = orderService.getOrderDetails(orderId, currentUser.getId());
            model.addAttribute("order", order);
            return "user/order-details";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "You don't have permission to view this order");
            return "redirect:/user/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Order not found");
            return "redirect:/user/orders";
        }
    }

    /**
     * Display order tracking page (with search form)
     */
    @GetMapping("/track")
    public String trackOrderPage() {
        // User is already added to model by GlobalControllerAdvice
        return "user/order-tracking";
    }

    /**
     * Track order by order number (for logged-in users)
     */
    @GetMapping("/track/{orderNumber}")
    public String trackOrderByNumber(@PathVariable String orderNumber,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        UserDTO currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login or use order number and email to track");
            return "redirect:/user/orders/track";
        }

        try {
            OrderTrackingDTO tracking = orderService.trackOrder(orderNumber, currentUser.getId());
            model.addAttribute("tracking", tracking);
            return "user/order-tracking";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Order not found or doesn't belong to you");
            return "redirect:/user/orders/track";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to track order");
            return "redirect:/user/orders/track";
        }
    }

    /**
     * Track order by order number and email (POST - for form submission)
     */
    @PostMapping("/track")
    public String trackOrderByEmailPost(@RequestParam String orderNumber,
            @RequestParam String email,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            OrderTrackingDTO tracking = orderService.trackOrderByEmail(orderNumber, email);
            model.addAttribute("tracking", tracking);
            return "user/order-tracking";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Order not found or email doesn't match");
            return "redirect:/user/orders/track";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to track order");
            return "redirect:/user/orders/track";
        }
    }

    /**
     * Cancel order (only allowed for PENDING orders)
     */
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Long orderId,
            @RequestParam(required = false, defaultValue = "") String reason,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        UserDTO currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to cancel order");
            return "redirect:/login";
        }

        try {
            orderService.cancelOrder(orderId, currentUser.getId(), reason);
            redirectAttributes.addFlashAttribute("success", "Order cancelled successfully");
            return "redirect:/user/orders";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/orders";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "You don't have permission to cancel this order");
            return "redirect:/user/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel order");
            return "redirect:/user/orders";
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Get current logged-in user from Authentication object
     */
    private UserDTO getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            String email = authentication.getName();
            return userService.getUserByEmail(email);
        }
        return null;
    }
}
