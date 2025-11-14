package g6.fashionFlex.controller;

import g6.fashionFlex.service.AdminStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminStatsController {

    @Autowired
    private AdminStatsService statsService;

    @GetMapping("/stats")
    public String stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Integer year,
            Model model) {
        
        if (startDate != null && endDate != null) {
            Map<String, Object> stats = statsService.getStatsByDateRange(startDate, endDate);
            model.addAllAttributes(stats);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        } else if (year != null) {
            Map<String, java.math.BigDecimal> revenueByMonth = statsService.getRevenueByMonth(year);
            model.addAttribute("revenueByMonth", revenueByMonth);
            model.addAttribute("year", year);
        } else {
            // Default to last 30 days
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(30);
            Map<String, Object> stats = statsService.getStatsByDateRange(start, end);
            model.addAllAttributes(stats);
            model.addAttribute("startDate", start);
            model.addAttribute("endDate", end);
        }
        
        return "admin/stats";
    }

    @GetMapping("/revenue/daily")
    public String revenueByDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Model model) {
        Map<String, java.math.BigDecimal> revenueByDay = statsService.getRevenueByDay(startDate, endDate);
        model.addAttribute("revenueByDay", revenueByDay);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/revenue-daily";
    }

    @GetMapping("/revenue/monthly")
    public String revenueByMonth(@RequestParam Integer year, Model model) {
        Map<String, java.math.BigDecimal> revenueByMonth = statsService.getRevenueByMonth(year);
        model.addAttribute("revenueByMonth", revenueByMonth);
        model.addAttribute("year", year);
        return "admin/revenue-monthly";
    }
}

