package g6.fashionFlex.controller;

import g6.fashionFlex.dto.CartSummaryDTO;
import g6.fashionFlex.entity.Cart;
import g6.fashionFlex.entity.CartItem;
import g6.fashionFlex.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    // Tỷ giá VND/USD
    private static final double USD_TO_VND_RATE = 25000.0;

    @PostMapping(value = "/cart/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addToCart(@RequestParam("variantId") Integer variantId,
                                       @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                       HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        session.setAttribute("CART_SESSION_ID", cart.getSessionID());

        CartSummaryDTO summary = cartService.addItemAndGetSummary(cart, variantId, quantity == null ? 1 : quantity);

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
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        session.setAttribute("CART_SESSION_ID", cart.getSessionID());
        List<CartItem> items = cartService.getItems(cart);
        model.addAttribute("cart", cart);
        model.addAttribute("items", items);
        return "shopping-cart";
    }

    @GetMapping("/checkout")
    public String checkout(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(true);
        String sessionId = (String) session.getAttribute("CART_SESSION_ID");
        if (sessionId == null || sessionId.isBlank()) {
            return "redirect:/shopping-cart";
        }
        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        List<CartItem> items = cartService.getItems(cart);
        if (items == null || items.isEmpty()) {
            return "redirect:/shopping-cart";
        }
        
        // Tính tổng giá trị đơn hàng (USD) - Xử lý BigDecimal đúng cách
        BigDecimal subtotalUSD = BigDecimal.ZERO;
        for (CartItem item : items) {
            BigDecimal itemPrice = item.getVariant().getPrice();
            BigDecimal itemQuantity = BigDecimal.valueOf(item.getQuantity());
            BigDecimal itemTotal = itemPrice.multiply(itemQuantity);
            subtotalUSD = subtotalUSD.add(itemTotal);
        }
        
        // Shipping = 0 trong trường hợp này
        BigDecimal shippingUSD = BigDecimal.ZERO;
        BigDecimal totalUSD = subtotalUSD.add(shippingUSD);
        
        // Quy đổi sang VND và làm tròn
        double totalUSDDouble = totalUSD.doubleValue();
        long totalVND = Math.round(totalUSDDouble * USD_TO_VND_RATE);
        
        // Đảm bảo số tiền tối thiểu (VNPay yêu cầu tối thiểu 5,000 VND)
        if (totalVND < 5000) {
            totalVND = 5000;
        }
        
        model.addAttribute("cart", cart);
        model.addAttribute("items", items);
        model.addAttribute("subtotalUSD", subtotalUSD);
        model.addAttribute("shippingUSD", shippingUSD);
        model.addAttribute("totalUSD", totalUSD);
        model.addAttribute("totalVND", totalVND);
        
        return "checkout";
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if (requestedWith != null && requestedWith.equalsIgnoreCase("XMLHttpRequest")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }
}