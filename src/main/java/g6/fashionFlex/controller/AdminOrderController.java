package g6.fashionFlex.controller;

import g6.fashionFlex.dto.OrderDTO;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.service.AdminOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    @Autowired
    private AdminOrderService orderService;

    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderDTO> orderPage;

        // Parse enum values
        Order.OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }

        Order.PaymentStatus paymentStatusEnum = null;
        if (paymentStatus != null && !paymentStatus.isEmpty()) {
            try {
                paymentStatusEnum = Order.PaymentStatus.valueOf(paymentStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid payment status, ignore
            }
        }

        // Use advanced search if any filters are provided
        if (keyword != null || orderStatus != null || paymentStatusEnum != null || startDate != null || endDate != null) {
            orderPage = orderService.searchOrdersAdvanced(keyword, orderStatus, paymentStatusEnum, startDate, endDate, pageable);
        } else {
            orderPage = orderService.getAllOrders(pageable);
        }

        // Add statistics
        AdminOrderService.OrderStatistics statistics = orderService.getStatistics();
        model.addAttribute("statistics", statistics);

        // Add filter options
        model.addAttribute("orderStatuses", Order.OrderStatus.values());
        model.addAttribute("paymentStatuses", Order.PaymentStatus.values());

        // Add current filters to model for persistence
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", orderPage.getNumber());
        model.addAttribute("totalItems", orderPage.getTotalElements());
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "admin/orders/list";
    }

    @GetMapping("/form/{id}")
    public String orderForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            OrderDTO order = orderService.getOrderById(id);
            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", Order.OrderStatus.values());
            model.addAttribute("paymentStatuses", Order.PaymentStatus.values());
            return "admin/orders/form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Order not found: " + e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    @PostMapping("/update-status/{id}")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            orderService.updateOrderStatus(id, orderStatus);
            redirectAttributes.addFlashAttribute("success", "Order status updated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid order status");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error updating order status: " + e.getMessage());
        }
        return "redirect:/admin/orders/form/" + id;
    }

    @PostMapping("/update-payment/{id}")
    public String updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String paymentStatus,
            RedirectAttributes redirectAttributes) {
        try {
            Order.PaymentStatus status = Order.PaymentStatus.valueOf(paymentStatus.toUpperCase());
            orderService.updatePaymentStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Payment status updated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid payment status");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error updating payment status: " + e.getMessage());
        }
        return "redirect:/admin/orders/form/" + id;
    }

    @PostMapping("/update-tracking/{id}")
    public String updateTrackingNumber(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.updateTrackingNumber(id, trackingNumber);
            redirectAttributes.addFlashAttribute("success", "Tracking number updated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error updating tracking number: " + e.getMessage());
        }
        return "redirect:/admin/orders/form/" + id;
    }

    // Bulk Actions
    @PostMapping("/bulk/cancel")
    @ResponseBody
    public ResponseEntity<?> bulkCancel(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) payload.get("ids");
            String reason = (String) payload.get("reason");
            int count = orderService.bulkCancel(ids, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " orders cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk/mark-shipped")
    @ResponseBody
    public ResponseEntity<?> bulkMarkShipped(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = orderService.bulkMarkShipped(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " orders marked as shipped"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk/mark-delivered")
    @ResponseBody
    public ResponseEntity<?> bulkMarkDelivered(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = orderService.bulkMarkDelivered(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " orders marked as delivered"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
