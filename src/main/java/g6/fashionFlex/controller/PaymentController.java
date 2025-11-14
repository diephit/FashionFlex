package g6.fashionFlex.controller;

import g6.fashionFlex.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PaymentController {

    @Autowired
    private VNPayService vnPayService;

    @PostMapping("/submitOrder")
    public String submitOrder(
            @RequestParam("amount") long orderTotal,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {
        
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String vnpayUrl = vnPayService.createOrder(request, orderTotal, orderInfo, baseUrl + "/vnpay-payment-return");
        
        return "redirect:" + vnpayUrl;
    }

    @GetMapping("/vnpay-payment-return")
    public String paymentReturn(HttpServletRequest request, Model model) {
        int paymentStatus = vnPayService.orderReturn(request);

        String orderInfo = request.getParameter("vnp_OrderInfo");
        String paymentTime = request.getParameter("vnp_PayDate");
        String transactionId = request.getParameter("vnp_TransactionNo");
        String totalPrice = request.getParameter("vnp_Amount");

        model.addAttribute("orderId", orderInfo);
        model.addAttribute("totalPrice", Long.parseLong(totalPrice) / 100);
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

        return "payment-result";
    }
}