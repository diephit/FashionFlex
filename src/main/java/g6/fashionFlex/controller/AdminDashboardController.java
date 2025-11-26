package g6.fashionFlex.controller;

import g6.fashionFlex.service.AdminStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    @Autowired
    private AdminStatsService statsService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", statsService.getAdminStatistics());
        return "admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("stats", statsService.getAdminStatistics());
        return "admin/dashboard";
    }
}
