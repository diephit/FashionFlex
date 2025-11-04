package g6.fashionFlex.controller;

import g6.fashionFlex.dto.AdminStatsDTO;
import g6.fashionFlex.service.AdminStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    @Autowired
    private AdminStatsService statsService;

    @GetMapping
    public ResponseEntity<AdminStatsDTO> getAdminStatistics() {
        return ResponseEntity.ok(statsService.getAdminStatistics());
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenue() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalRevenue", statsService.getTotalRevenue());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue/range")
    public ResponseEntity<Map<String, Object>> getRevenueByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        BigDecimal revenue = statsService.getRevenueByDateRange(startDate, endDate);
        long orderCount = statsService.getOrderCountByDateRange(startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("revenue", revenue);
        response.put("orderCount", orderCount);
        response.put("startDate", startDate);
        response.put("endDate", endDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Object>> getCounts() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", statsService.getTotalUsers());
        response.put("totalProducts", statsService.getTotalProducts());
        response.put("totalOrders", statsService.getTotalOrders());
        response.put("totalCategories", statsService.getTotalCategories());
        return ResponseEntity.ok(response);
    }
}
