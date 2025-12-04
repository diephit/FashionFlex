package g6.fashionFlex.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import g6.fashionFlex.entity.User;
import g6.fashionFlex.service.CheckoutService;
import g6.fashionFlex.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PaymentController extends BaseController {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/submitOrder")
    public String submitOrder(
            @RequestParam("amount") long orderTotal,
            @RequestParam("orderInfo") String orderInfo,
            @RequestParam("email") String email,
            @RequestParam(value = "address", required = false) String address,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        try {
            email = email != null ? email.trim() : "";
            if (email.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Please provide a valid email to continue checkout.");
                return "redirect:/checkout";
            }

            HttpSession session = request.getSession(true);
            String sessionId = (String) session.getAttribute("CART_SESSION_ID");
            if (sessionId == null || sessionId.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Cart is empty");
                return "redirect:/shopping-cart";
            }

            // Get authenticated user if exists
            User authenticatedUser = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String userEmail = auth.getName();
                authenticatedUser = userRepository.findByEmail(userEmail).orElse(null);
            }

            // Create order from cart (address always entered manually)
            CheckoutService.CheckoutResult result = checkoutService.createOrderFromSessionCart(
                    sessionId, email, address, authenticatedUser);

            // Store order ID in session for payment return
            session.setAttribute("PENDING_ORDER_ID", result.getOrder().getOrderID());

            // Create VNPay order with order ID in orderInfo
            String vnpayOrderInfo = "ORDER-" + result.getOrder().getOrderID();
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String vnpayUrl = vnPayService.createOrder(request, orderTotal, vnpayOrderInfo, baseUrl + "/vnpay-payment-return");
            
            return "redirect:" + vnpayUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating order: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/vnpay-payment-return")
    public String paymentReturn(HttpServletRequest request, Model model) {
        int paymentStatus = vnPayService.orderReturn(request);

        String orderInfo = request.getParameter("vnp_OrderInfo");
        String paymentTime = request.getParameter("vnp_PayDate");
        String transactionId = request.getParameter("vnp_TransactionNo");
        String totalPrice = request.getParameter("vnp_Amount");
        long amountVnd = totalPrice != null && !totalPrice.isEmpty() ? Long.parseLong(totalPrice) / 100 : 0;

        // Extract order ID from orderInfo
        Integer orderId = checkoutService.extractOrderId(orderInfo);
        if (orderId == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                orderId = (Integer) session.getAttribute("PENDING_ORDER_ID");
            }
        }

        // Collect all VNPay parameters
        Map<String, String> vnpayParams = new HashMap<>();
        for (var paramName = request.getParameterNames(); paramName.hasMoreElements();) {
            String name = paramName.nextElement();
            vnpayParams.put(name, request.getParameter(name));
        }

        // Update order and payment status
        if (orderId != null) {
            boolean success = (paymentStatus == 1);
            checkoutService.handlePaymentResult(orderId, success, transactionId, amountVnd, vnpayParams);
        }

        model.addAttribute("orderId", orderId != null ? orderId : orderInfo);
        model.addAttribute("totalPrice", amountVnd);
        model.addAttribute("paymentTime", paymentTime);
        model.addAttribute("transactionId", transactionId);

        if (paymentStatus == 1) {
            model.addAttribute("message", "Payment Successful");
            model.addAttribute("status", "success");
        } else if (paymentStatus == 0) {
            model.addAttribute("message", "Payment Failed");
            model.addAttribute("status", "failed");
        } else {
            model.addAttribute("message", "Invalid Signature");
            model.addAttribute("status", "invalid");
        }

        // Clear pending order from session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("PENDING_ORDER_ID");
        }

        return "payment-result";
    }
}