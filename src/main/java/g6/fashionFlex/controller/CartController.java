package g6.fashionFlex.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import g6.fashionFlex.dto.CartSummaryDTO;
import g6.fashionFlex.entity.Cart;
import g6.fashionFlex.entity.CartItem;
import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.Order.OrderStatus;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.service.CartService;
import g6.fashionFlex.service.CheckoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class CartController extends BaseController { 

    @Autowired
    private CartService cartService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping(value = "/cart/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addToCart(@RequestParam("variantId") Integer variantId,
                                       @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                       @RequestParam(value = "selectedSize", required = false) String selectedSize,
                                       HttpServletRequest request) {
        // GIỮ NGUYÊN - KHÔNG SỬA GÌ
        HttpSession session = request.getSession(true);
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        session.setAttribute("CART_SESSION_ID", cart.getSessionID());

        CartSummaryDTO summary = cartService.addItemAndGetSummary(cart, variantId, quantity == null ? 1 : quantity, selectedSize);

        if (isAjaxRequest(request)) {
            return ResponseEntity.ok(summary);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(org.springframework.web.util.UriComponentsBuilder.fromPath("/shopping-cart").build().toUri());
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @GetMapping(value = "/cart/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CartSummaryDTO> getCartSummary(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        session.setAttribute("CART_SESSION_ID", cart.getSessionID());

        CartSummaryDTO summary = cartService.getCartSummary(cart);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/shopping-cart")
    public String showCart(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(true);
        addAuthenticationToModel(model, session);  // CHỈ THÊM DÒNG NÀY
        
        // GIỮ NGUYÊN LOGIC CŨ
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        session.setAttribute("CART_SESSION_ID", cart.getSessionID());
        List<CartItem> items = cartService.getItems(cart);
        Map<Integer, Integer> variantStockMap = new HashMap<>();
        for (CartItem item : items) {
            if (item.getVariant() == null || item.getVariant().getVariantID() == null) {
                continue;
            }
            Integer variantId = item.getVariant().getVariantID();
            Product product = item.getVariant().getProduct();
            if (product != null && product.getStockQuantity() != null) {
                variantStockMap.put(variantId, product.getStockQuantity());
            }
        }
        model.addAttribute("cart", cart);
        model.addAttribute("items", items);
        model.addAttribute("variantStockMap", variantStockMap);
        return "shopping-cart";
    }

    @GetMapping("/checkout")
    public String checkout(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(true);
        addAuthenticationToModel(model, session);
        
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null || sessionId.isBlank()) {
            return "redirect:/shopping-cart";
        }
        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        List<CartItem> items = cartService.getItems(cart);
        if (items == null || items.isEmpty()) {
            return "redirect:/shopping-cart";
        }
        
        Customer checkoutCustomer = null;

        // Get customer info for auto-fill if authenticated (email + membership discount)
        String customerEmail = null;
        Boolean isAuthenticated = (Boolean) model.getAttribute("isAuthenticated");
        if (isAuthenticated != null && isAuthenticated) {
            String userEmail = (String) model.getAttribute("userEmail");
            if (userEmail != null) {
                customerEmail = userEmail;
                User user = userRepository.findByEmail(userEmail).orElse(null);
                if (user != null) {
                    checkoutCustomer = customerRepository.findByUser(user).orElse(null);
                }
            }
        }

        CheckoutService.CheckoutTotals totals = checkoutService.calculateTotals(cart, checkoutCustomer);
        
        model.addAttribute("cart", cart);
        model.addAttribute("items", items);
        model.addAttribute("subtotalUSD", totals.getSubtotalUSD());
        model.addAttribute("discountAmountUSD", totals.getDiscountAmountUSD());
        model.addAttribute("discountedSubtotalUSD", totals.getDiscountedSubtotalUSD());
        model.addAttribute("discountRate", totals.getDiscountRate());
        model.addAttribute("shippingUSD", totals.getShippingUSD());
        model.addAttribute("totalUSD", totals.getTotalUSD());
        model.addAttribute("totalVND", totals.getTotalVND());
        model.addAttribute("hasMembershipDiscount", totals.hasDiscount());
        if (customerEmail != null) {
            model.addAttribute("customerEmail", customerEmail);
        }
        
        return "checkout";
    }

    @GetMapping("/checkout/continue/{orderId}")
    public String continueCheckoutFromOrder(@PathVariable Integer orderId,
                                            HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        session.setAttribute("CART_SESSION_ID", cart.getSessionID());

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return "redirect:/order-history";
        }

        // Chỉ cho phép continue với đơn đang pending hoặc đã paid
        if (order.getStatus() != OrderStatus.pending && order.getStatus() != OrderStatus.paid) {
            return "redirect:/order-history";
        }

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return "redirect:/order-history";
        }

        // Build lại giỏ hàng từ order items
        cartService.rebuildCartFromOrder(cart, order);
        return "redirect:/checkout";
    }

    @DeleteMapping(value = "/cart/remove/{cartItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeFromCart(@PathVariable Integer cartItemId,
                                            HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        cartService.removeItem(cart, cartItemId);
        CartSummaryDTO summary = cartService.getCartSummary(cart);
        
        return ResponseEntity.ok(summary);
    }

    @PostMapping(value = "/cart/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CartSummaryDTO> updateCart(@RequestBody CartUpdatePayload payload,
                                                     HttpServletRequest request) {
        if (payload == null || payload.getItems() == null || payload.getItems().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        HttpSession session = request.getSession(true);
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        session.setAttribute("CART_SESSION_ID", cart.getSessionID());

        payload.getItems().forEach(item -> {
            if (item.getCartItemId() != null && item.getQuantity() != null) {
                cartService.updateItemQuantity(cart, item.getCartItemId(), item.getQuantity());
            }
        });

        CartSummaryDTO summary = cartService.getCartSummary(cart);
        return ResponseEntity.ok(summary);
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if (requestedWith != null && requestedWith.equalsIgnoreCase("XMLHttpRequest")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    public static class CartUpdatePayload {
        private List<CartUpdateItem> items;

        public List<CartUpdateItem> getItems() {
            return items;
        }

        public void setItems(List<CartUpdateItem> items) {
            this.items = items;
        }
    }

    public static class CartUpdateItem {
        private Integer cartItemId;
        private Integer quantity;

        public Integer getCartItemId() {
            return cartItemId;
        }

        public void setCartItemId(Integer cartItemId) {
            this.cartItemId = cartItemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}