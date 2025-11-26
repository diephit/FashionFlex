package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.service.OrderService;
import g6.fashionFlex.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * VNPay Payment Controller
 * Handles VNPay payment requests and callbacks
 */
@Controller
@RequestMapping("/payment/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    /**
     * Initiate VNPay payment for an order
     * Redirects user to VNPay payment gateway
     */
    @GetMapping("/pay/{orderId}")
    public String initiatePayment(@PathVariable Long orderId,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            log.info("Initiating VNPay payment for order ID: {}", orderId);

            // Get order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            // Validate order status
            if (order.getStatus() != Order.OrderStatus.PENDING) {
                log.warn("Invalid order status for payment: {} - {}", orderId, order.getStatus());
                redirectAttributes.addFlashAttribute("error", "Order is not in valid state for payment");
                return "redirect:/user/orders/" + orderId;
            }

            // Validate payment method
            if (order.getPaymentMethod() != Order.PaymentMethod.VNPAY) {
                log.warn("Invalid payment method for VNPay: {} - {}", orderId, order.getPaymentMethod());
                redirectAttributes.addFlashAttribute("error", "Order payment method is not VNPay");
                return "redirect:/user/orders/" + orderId;
            }

            // Create VNPay payment URL
            String paymentUrl = vnPayService.createPaymentUrl(order, request);

            log.info("Redirecting to VNPay payment gateway for order: {}", order.getOrderNumber());

            // Redirect to VNPay
            return "redirect:" + paymentUrl;

        } catch (IllegalArgumentException e) {
            log.error("Order validation error for VNPay payment", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/orders";
        } catch (Exception e) {
            log.error("Error initiating VNPay payment for order: {}", orderId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to initiate payment: " + e.getMessage());
            return "redirect:/user/orders/" + orderId;
        }
    }

    /**
     * Handle VNPay callback after payment
     * This endpoint is called by VNPay after user completes payment
     */
    @GetMapping("/callback")
    public String handleCallback(HttpServletRequest request, Model model) {
        try {
            log.info("Received VNPay payment callback");

            // Verify callback signature
            boolean isValidSignature = vnPayService.verifyCallback(request);
            if (!isValidSignature) {
                log.error("Invalid VNPay callback signature");
                model.addAttribute("paymentSuccess", false);
                model.addAttribute("message", "Invalid payment verification. Please contact support.");
                return "payment/payment-result";
            }

            // Extract payment result
            Map<String, String> paymentResult = vnPayService.getPaymentResult(request);
            String orderIdStr = paymentResult.get("orderId");
            String responseCode = paymentResult.get("responseCode");
            String transactionStatus = paymentResult.get("transactionStatus");
            String transactionNo = paymentResult.get("transactionNo");
            String amountStr = paymentResult.get("amount");
            long paidAmount = 0L;
            if (amountStr != null && !amountStr.isEmpty()) {
                paidAmount = Long.parseLong(amountStr);
            }

            log.info("Payment callback - OrderID: {}, ResponseCode: {}, TransactionStatus: {}, TransactionNo: {}",
                    orderIdStr, responseCode, transactionStatus, transactionNo);

            // Get order
            Long orderId = Long.parseLong(orderIdStr);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            // Check payment success
            boolean isPaymentSuccess = vnPayService.isPaymentSuccess(responseCode, transactionStatus);

            if (isPaymentSuccess) {
                // Payment successful - update order
                log.info("Payment successful for order: {}", order.getOrderNumber());

                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setStatus(Order.OrderStatus.CONFIRMED);
                order.setTrackingNumber(transactionNo);
                orderRepository.save(order);

                model.addAttribute("paymentSuccess", true);
                model.addAttribute("message", "Payment successful! Your order has been confirmed.");
                model.addAttribute("orderNumber", order.getOrderNumber());
                model.addAttribute("orderId", order.getId());
                model.addAttribute("transactionNo", transactionNo);

            } else {
                // Payment failed - update order
                log.warn("Payment failed for order: {} - ResponseCode: {}", order.getOrderNumber(), responseCode);

                order.setPaymentStatus(Order.PaymentStatus.FAILED);
                orderRepository.save(order);

                String errorMessage = vnPayService.getResponseDescription(responseCode);
                model.addAttribute("paymentSuccess", false);
                model.addAttribute("message", errorMessage);
                model.addAttribute("orderNumber", order.getOrderNumber());
                model.addAttribute("orderId", order.getId());
            }

            // Add additional payment info
            model.addAttribute("paymentResult", paymentResult);
            model.addAttribute("order", order);
            model.addAttribute("paidAmount", paidAmount);

            return "payment/payment-result";

        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            model.addAttribute("paymentSuccess", false);
            model.addAttribute("message", "Error processing payment. Please contact support.");
            model.addAttribute("paymentResult", new java.util.HashMap<>());
            model.addAttribute("order", new Order());
            model.addAttribute("paidAmount", 0L);
            return "payment/payment-result";
        }
    }

    /**
     * Show payment result page (for direct access with order ID)
     */
    @GetMapping("/result/{orderId}")
    public String showPaymentResult(@PathVariable Long orderId,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));

            model.addAttribute("order", order);
            model.addAttribute("paymentSuccess", order.getPaymentStatus() == Order.PaymentStatus.PAID);
            model.addAttribute("orderNumber", order.getOrderNumber());
            model.addAttribute("orderId", order.getId());

            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                model.addAttribute("message", "Payment successful! Your order has been confirmed.");
            } else {
                model.addAttribute("message", "Payment status: " + order.getPaymentStatus());
            }

            return "payment/payment-result";

        } catch (Exception e) {
            log.error("Error showing payment result for order: {}", orderId, e);
redirectAttributes.addFlashAttribute("error", "Order not found");
return "redirect:/user/orders";
}
}
}
