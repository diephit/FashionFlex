package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.Order.OrderStatus;
import g6.fashionFlex.entity.Payment;
import g6.fashionFlex.service.AdminOrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
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
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Order> orders;

        boolean hasDateRange = startDate != null && !startDate.isBlank();
        if (hasDateRange && (endDate == null || endDate.isBlank())) {
            endDate = startDate;
        }

        if (hasDateRange) {
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            java.time.LocalDateTime startDt = start.atStartOfDay();
            java.time.LocalDateTime endDt = end.plusDays(1).atStartOfDay().minusNanos(1);

            if (status != null && !status.isEmpty()) {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                orders = orderService.getOrdersByStatusAndDateRange(orderStatus, startDt, endDt, pageable);
            } else {
                orders = orderService.getOrdersByDateRange(startDt, endDt, pageable);
            }
        } else if (keyword != null && !keyword.trim().isEmpty()) {
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
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        // Add statistics
        model.addAttribute("totalOrders", orderService.countAllOrders());
        model.addAttribute("pendingOrders", orderService.countPendingOrders());
        model.addAttribute("paidOrders", orderService.countPaidOrders());
        model.addAttribute("completedOrders", orderService.countCompletedOrders());
        model.addAttribute("canceledOrders", orderService.countCanceledOrders());
        
        return "admin/orders";
    }

    @GetMapping("/export")
    public void exportOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("orderID").descending());
        Page<Order> ordersPage;

        boolean hasDateRange = startDate != null && !startDate.isBlank();
        if (hasDateRange && (endDate == null || endDate.isBlank())) {
            endDate = startDate;
        }

        if (hasDateRange) {
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            java.time.LocalDateTime startDt = start.atStartOfDay();
            java.time.LocalDateTime endDt = end.plusDays(1).atStartOfDay().minusNanos(1);

            if (status != null && !status.isEmpty()) {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                ordersPage = orderService.getOrdersByStatusAndDateRange(orderStatus, startDt, endDt, pageable);
            } else {
                ordersPage = orderService.getOrdersByDateRange(startDt, endDt, pageable);
            }
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            ordersPage = orderService.searchOrders(keyword, pageable);
            if (status != null && !status.isEmpty()) {
                OrderStatus orderStatus = OrderStatus.valueOf(status);
                List<Order> filtered = ordersPage.getContent().stream()
                        .filter(o -> o.getStatus() == orderStatus)
                        .toList();
                ordersPage = new PageImpl<>(filtered, pageable, filtered.size());
            }
        } else if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            ordersPage = orderService.getOrdersByStatus(orderStatus, pageable);
        } else {
            ordersPage = orderService.getAllOrders(pageable);
        }

        List<Order> orders = ordersPage.getContent();

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"orders-export.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("OrderID,CustomerName,Email,Status,OrderDate,TotalAmount,ItemsCount");
        for (Order o : orders) {
            String name = o.getCustomerName() != null ? o.getCustomerName() : "";
            String email = o.getContactEmail() != null ? o.getContactEmail() : "";
            String statusName = o.getStatus() != null ? o.getStatus().name() : "";
            String date = o.getOrderDate() != null ? o.getOrderDate().toString() : "";
            String total = o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0";
            int itemsCount = (o.getOrderItems() != null) ? o.getOrderItems().size() : 0;

            writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",%s,%d%n",
                    o.getOrderID(),
                    name.replace("\"", "\"\""),
                    email.replace("\"", "\"\""),
                    statusName,
                    date,
                    total,
                    itemsCount);
        }
        writer.flush();
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
        
        return "admin/order-detail";
    }
}