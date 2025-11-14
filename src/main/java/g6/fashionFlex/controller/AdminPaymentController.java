package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Payment;
import g6.fashionFlex.entity.Payment.PaymentStatus;
import g6.fashionFlex.entity.PaymentHistory;
import g6.fashionFlex.service.AdminPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    @Autowired
    private AdminPaymentService paymentService;

    @GetMapping
    public String listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentID") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Payment> payments;
        if (keyword != null && !keyword.trim().isEmpty()) {
            payments = paymentService.searchPayments(keyword, pageable);
            if (status != null && !status.isEmpty()) {
                PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
                List<Payment> filtered = payments.getContent().stream()
                    .filter(p -> p.getStatus() == paymentStatus)
                    .toList();
                payments = new PageImpl<>(filtered, pageable, filtered.size());
            }
        } else if (status != null && !status.isEmpty()) {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
            List<Payment> paymentsList = paymentService.getPaymentsByStatus(paymentStatus);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), paymentsList.size());
            List<Payment> pageContent = paymentsList.subList(start, end);
            payments = new PageImpl<>(pageContent, pageable, paymentsList.size());
        } else {
            payments = paymentService.getAllPayments(pageable);
        }
        
        model.addAttribute("payments", payments);
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", payments.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        return "admin/payments";
    }

    @GetMapping("/{id}")
    public String viewPayment(@PathVariable Integer id, Model model) {
        Optional<Payment> payment = paymentService.getPaymentById(id);
        if (payment.isEmpty()) {
            return "redirect:/admin/payments";
        }
        
        List<PaymentHistory> history = paymentService.getPaymentHistory(id);
        model.addAttribute("payment", payment.get());
        model.addAttribute("history", history);
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        
        return "admin/payment-detail";
    }

    @PostMapping("/{id}/status")
    public String updatePaymentStatus(@PathVariable Integer id,
                                     @RequestParam PaymentStatus status,
                                     @RequestParam(required = false) String gatewayResponse,
                                     RedirectAttributes redirectAttributes) {
        try {
            paymentService.updatePaymentStatus(id, status, gatewayResponse);
            redirectAttributes.addFlashAttribute("success", "Payment status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }
        
        return "redirect:/admin/payments/" + id;
    }

    @PostMapping("/{id}/refund")
    public String processRefund(@PathVariable Integer id,
                                @RequestParam(required = false) String note,
                                RedirectAttributes redirectAttributes) {
        try {
            paymentService.processRefund(id, note);
            redirectAttributes.addFlashAttribute("success", "Refund processed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing refund: " + e.getMessage());
        }
        
        return "redirect:/admin/payments/" + id;
    }

    @GetMapping("/order/{orderId}")
    public String getOrderPayments(@PathVariable Integer orderId, Model model) {
        List<Payment> payments = paymentService.getOrderPayments(orderId);
        model.addAttribute("payments", payments);
        model.addAttribute("orderId", orderId);
        return "admin/order-payments";
    }
}

