package g6.fashionFlex.controller;

import g6.fashionFlex.entity.MembershipHistory;
import g6.fashionFlex.entity.MembershipLevel;
import g6.fashionFlex.service.AdminMembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/membership")
public class AdminMembershipController {

    @Autowired
    private AdminMembershipService membershipService;

    @GetMapping("/levels")
    public String listLevels(Model model) {
        List<MembershipLevel> levels = membershipService.getAllMembershipLevels();
        model.addAttribute("levels", levels);
        return "admin/membership-levels";
    }

    @PostMapping("/levels")
    public String createLevel(@ModelAttribute MembershipLevel level,
                              RedirectAttributes redirectAttributes) {
        try {
            membershipService.createMembershipLevel(level);
            redirectAttributes.addFlashAttribute("success", "Membership level created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating level: " + e.getMessage());
        }
        
        return "redirect:/admin/membership/levels";
    }

    @PostMapping("/levels/{id}")
    public String updateLevel(@PathVariable Integer id,
                             @ModelAttribute MembershipLevel level,
                             RedirectAttributes redirectAttributes) {
        try {
            membershipService.updateMembershipLevel(id, level);
            redirectAttributes.addFlashAttribute("success", "Membership level updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating level: " + e.getMessage());
        }
        
        return "redirect:/admin/membership/levels";
    }

    @PostMapping("/levels/{id}/delete")
    public String deleteLevel(@PathVariable Integer id,
                             RedirectAttributes redirectAttributes) {
        try {
            membershipService.deleteMembershipLevel(id);
            redirectAttributes.addFlashAttribute("success", "Membership level deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting level: " + e.getMessage());
        }
        
        return "redirect:/admin/membership/levels";
    }

    @GetMapping("/history")
    public String history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer customerID,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        if (customerID != null) {
            List<MembershipHistory> history = membershipService.getMembershipHistory(customerID);
            model.addAttribute("history", history);
            model.addAttribute("customerID", customerID);
        } else {
            Page<MembershipHistory> historyPage = membershipService.getAllMembershipHistory(pageable);
            model.addAttribute("history", historyPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", historyPage.getTotalPages());
        }
        
        return "admin/membership-history";
    }

    @PostMapping("/customers/{customerId}/points")
    public String updatePoints(@PathVariable Integer customerId,
                              @RequestParam Integer points,
                              RedirectAttributes redirectAttributes) {
        try {
            membershipService.updateLoyaltyPoints(customerId, points);
            redirectAttributes.addFlashAttribute("success", "Loyalty points updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating points: " + e.getMessage());
        }
        
        return "redirect:/admin/customers/" + customerId;
    }

    @PostMapping("/customers/{customerId}/points/add")
    public String addPoints(@PathVariable Integer customerId,
                           @RequestParam Integer points,
                           RedirectAttributes redirectAttributes) {
        try {
            membershipService.addLoyaltyPoints(customerId, points);
            redirectAttributes.addFlashAttribute("success", "Points added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding points: " + e.getMessage());
        }
        
        return "redirect:/admin/customers/" + customerId;
    }

    @PostMapping("/customers/{customerId}/points/deduct")
    public String deductPoints(@PathVariable Integer customerId,
                              @RequestParam Integer points,
                              RedirectAttributes redirectAttributes) {
        try {
            membershipService.deductLoyaltyPoints(customerId, points);
            redirectAttributes.addFlashAttribute("success", "Points deducted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deducting points: " + e.getMessage());
        }
        
        return "redirect:/admin/customers/" + customerId;
    }

    @PostMapping("/customers/{customerId}/level-change")
    public String recordLevelChange(@PathVariable Integer customerId,
                                   @RequestParam(required = false) Integer oldLevelID,
                                   @RequestParam(required = false) Integer newLevelID,
                                   @RequestParam(required = false) String note,
                                   RedirectAttributes redirectAttributes) {
        try {
            membershipService.recordLevelChange(customerId, oldLevelID, newLevelID, note);
            redirectAttributes.addFlashAttribute("success", "Level change recorded");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error recording level change: " + e.getMessage());
        }
        
        return "redirect:/admin/membership/history?customerID=" + customerId;
    }
}

