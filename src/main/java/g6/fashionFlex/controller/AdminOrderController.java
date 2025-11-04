package g6.fashionFlex.controller;

import g6.fashionFlex.dto.OrderDTO;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.service.AdminOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<OrderDTO> orderPage;
        if (keyword != null && !keyword.isEmpty()) {
            orderPage = orderService.searchOrders(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else if (status != null && !status.isEmpty()) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                orderPage = orderService.getOrdersByStatus(orderStatus, pageable);
                model.addAttribute("filterStatus", status);
            } catch (IllegalArgumentException e) {
                orderPage = orderService.getAllOrders(pageable);
            }
        } else {
            orderPage = orderService.getAllOrders(pageable);
        }

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", orderPage.getNumber());
        model.addAttribute("totalItems", orderPage.getTotalElements());
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("orderStatuses", Order.OrderStatus.values());

        return "admin/orders/list";
    }

    @GetMapping("/view/{id}")
    public String viewOrder(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            OrderDTO order = orderService.getOrderById(id);
            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", Order.OrderStatus.values());
            model.addAttribute("paymentStatuses", Order.PaymentStatus.values());
            return "admin/orders/view";
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
        return "redirect:/admin/orders/view/" + id;
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
        return "redirect:/admin/orders/view/" + id;
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
        return "redirect:/admin/orders/view/" + id;
    }
}
