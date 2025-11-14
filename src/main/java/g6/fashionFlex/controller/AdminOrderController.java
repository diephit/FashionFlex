package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.Order.OrderStatus;
import g6.fashionFlex.entity.Payment;
import g6.fashionFlex.entity.Payment.PaymentStatus;
import g6.fashionFlex.service.AdminOrderService;
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
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private AdminOrderService orderService;

    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderID") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Order> orders;
        if (keyword != null && !keyword.trim().isEmpty()) {
            orders = orderService.searchOrders(keyword, pageable);
            if (status != null && !status.isEmpty()) {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                // Filter in memory for status
                List<Order> filtered = orders.getContent().stream()
                    .filter(o -> o.getStatus() == orderStatus)
                    .toList();
                orders = new PageImpl<>(filtered, pageable, filtered.size());
            }
        } else if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            orders = orderService.getOrdersByStatus(orderStatus, pageable);
        } else {
            orders = orderService.getAllOrders(pageable);
        }
        
        model.addAttribute("orders", orders);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Integer id, Model model) {
        Optional<Order> order = orderService.getOrderById(id);
        if (order.isEmpty()) {
            return "redirect:/admin/orders";
        }
        
        List<Payment> payments = orderService.getOrderPayments(id);
        model.addAttribute("order", order.get());
        model.addAttribute("payments", payments);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        
        return "admin/order-detail";
    }

    @PostMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable Integer id,
                                   @RequestParam OrderStatus status,
                                   RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Order status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }
        
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/tracking")
    public String updateTrackingNumber(@PathVariable Integer id,
                                      @RequestParam String trackingNumber,
                                      RedirectAttributes redirectAttributes) {
        try {
            orderService.updateTrackingNumber(id, trackingNumber);
            redirectAttributes.addFlashAttribute("success", "Tracking number updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating tracking: " + e.getMessage());
        }
        
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/payments/{paymentId}/status")
    public String updatePaymentStatus(@PathVariable Integer paymentId,
                                     @RequestParam PaymentStatus status,
                                     @RequestParam Integer orderId,
                                     RedirectAttributes redirectAttributes) {
        try {
            orderService.updatePaymentStatus(paymentId, status);
            redirectAttributes.addFlashAttribute("success", "Payment status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating payment status: " + e.getMessage());
        }
        
        return "redirect:/admin/orders/" + orderId;
    }
}

