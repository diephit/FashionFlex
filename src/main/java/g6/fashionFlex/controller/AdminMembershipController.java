package g6.fashionFlex.controller;

import g6.fashionFlex.entity.MembershipLevel;
import g6.fashionFlex.repository.MembershipLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/membership")
public class AdminMembershipController {

    @Autowired
    private MembershipLevelRepository membershipLevelRepository;

    @GetMapping
    public String listMembershipLevels(@RequestParam(required = false) Integer editId, Model model) {
        List<MembershipLevel> levels = membershipLevelRepository.findAllOrderByMinSpentAsc();
        model.addAttribute("levels", levels);
        model.addAttribute("newLevel", new MembershipLevel());
        
        if (editId != null) {
            Optional<MembershipLevel> editLevel = membershipLevelRepository.findById(editId);
            editLevel.ifPresent(level -> model.addAttribute("editLevel", level));
        }
        
        return "admin/membership";
    }

    @PostMapping
    public String createMembershipLevel(@ModelAttribute MembershipLevel level,
                                       @RequestParam("minSpent") String minSpentStr,
                                       @RequestParam("discountRate") String discountRateStr,
                                       @RequestParam("bonusRate") String bonusRateStr,
                                       RedirectAttributes redirectAttributes) {
        try {
            level.setMinSpent(new BigDecimal(minSpentStr));
            level.setDiscountRate(new BigDecimal(discountRateStr));
            level.setBonusRate(new BigDecimal(bonusRateStr));
            membershipLevelRepository.save(level);
            redirectAttributes.addFlashAttribute("success", "Membership level created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating membership level: " + e.getMessage());
        }
        
        return "redirect:/admin/membership";
    }

    @PostMapping("/{id}")
    public String updateMembershipLevel(@PathVariable Integer id,
                                       @ModelAttribute MembershipLevel level,
                                       @RequestParam("minSpent") String minSpentStr,
                                       @RequestParam("discountRate") String discountRateStr,
                                       @RequestParam("bonusRate") String bonusRateStr,
                                       RedirectAttributes redirectAttributes) {
        try {
            Optional<MembershipLevel> existingLevelOpt = membershipLevelRepository.findById(id);
            if (existingLevelOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Membership level not found");
                return "redirect:/admin/membership";
            }
            
            MembershipLevel existingLevel = existingLevelOpt.get();
            existingLevel.setLevelName(level.getLevelName());
            existingLevel.setMinSpent(new BigDecimal(minSpentStr));
            existingLevel.setDiscountRate(new BigDecimal(discountRateStr));
            existingLevel.setBonusRate(new BigDecimal(bonusRateStr));
            existingLevel.setDescription(level.getDescription());
            
            membershipLevelRepository.save(existingLevel);
            redirectAttributes.addFlashAttribute("success", "Membership level updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating membership level: " + e.getMessage());
        }
        
        return "redirect:/admin/membership";
    }

    @PostMapping("/{id}/delete")
    public String deleteMembershipLevel(@PathVariable Integer id,
                                        RedirectAttributes redirectAttributes) {
        try {
            Optional<MembershipLevel> levelOpt = membershipLevelRepository.findById(id);
            if (levelOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Membership level not found");
                return "redirect:/admin/membership";
            }
            
            // Check if any customers are using this level
            MembershipLevel level = levelOpt.get();
            if (level.getCustomers() != null && !level.getCustomers().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete membership level that is assigned to customers");
                return "redirect:/admin/membership";
            }
            
            membershipLevelRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Membership level deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting membership level: " + e.getMessage());
        }
        
        return "redirect:/admin/membership";
    }
}

