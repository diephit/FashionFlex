package g6.fashionFlex.controller;

import g6.fashionFlex.dto.CartDTO;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.service.CartService;
import g6.fashionFlex.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

/**
 * Global controller advice to add common attributes to all views
 * This includes user authentication status, cart data, and wishlist count
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserService userService;
    private final CartService cartService;

    /**
     * Add user authentication data to all pages
     * This adds:
     * - isAuthenticated: boolean flag for user authentication status
     * - user: UserDTO object with user information (if authenticated)
     */
    @ModelAttribute
    public void addUserAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            try {
                String email = authentication.getName();
                UserDTO user = userService.getUserByEmail(email);

                if (user != null) {
                    model.addAttribute("isAuthenticated", true);
                    model.addAttribute("user", user);
                } else {
                    model.addAttribute("isAuthenticated", false);
                    model.addAttribute("user", null);
                }
            } catch (Exception e) {
                model.addAttribute("isAuthenticated", false);
                model.addAttribute("user", null);
            }
        } else {
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("user", null);
        }
    }

    /**
     * Add cart data to all pages for authenticated users
     * Cart sidebar will display:
     * - Top 5 most recent items
     * - Total item count
     * - Subtotal
     * - Links to view full cart and checkout
     */
    @ModelAttribute
    public void addCartAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            try {
                String email = authentication.getName();
                UserDTO user = userService.getUserByEmail(email);

                if (user != null) {
                    // Get full cart data
                    CartDTO cart = cartService.getCartByUserId(user.getId());

                    // Create a limited cart view for the sidebar (top 5 items)
                    CartDTO sidebarCart = new CartDTO();
                    sidebarCart.setId(cart.getId());
                    sidebarCart.setUserId(cart.getUserId());
                    sidebarCart.setSubtotal(cart.getSubtotal());
                    sidebarCart.setTotal(cart.getTotal());
                    sidebarCart.setTotalItems(cart.getTotalItems());

                    // Limit to 5 most recent items for sidebar display
                    List<g6.fashionFlex.dto.CartItemDTO> limitedItems = new ArrayList<>();
                    int itemsToShow = Math.min(5, cart.getItems().size());
                    for (int i = 0; i < itemsToShow; i++) {
                        limitedItems.add(cart.getItems().get(i));
                    }
                    sidebarCart.setItems(limitedItems);
                    sidebarCart.setHasMoreItems(cart.getItems().size() > 5);

                    // Add cart data to model
                    model.addAttribute("headerCart", sidebarCart);
                    model.addAttribute("cartItemCount", cart.getTotalItems());
                }
            } catch (Exception e) {
                // If error loading cart, set empty values
                model.addAttribute("headerCart", null);
                model.addAttribute("cartItemCount", 0);
            }
        } else {
            // Guest user - no cart data
            model.addAttribute("headerCart", null);
            model.addAttribute("cartItemCount", 0);
        }
    }
}
