package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Admin;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.entity.Stock;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.AdminRepository;
import g6.fashionFlex.repository.ProductVariantRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.service.AdminInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

    @Autowired
    private AdminInventoryService inventoryService;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    private Admin getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return null;
        }
        return adminRepository.findByUserUserID(user.getUserID()).orElse(null);
    }

    @GetMapping
    public String listStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer variantID,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        if (variantID != null) {
            List<Stock> stockHistory = inventoryService.getStockHistory(variantID);
            Integer currentQuantity = inventoryService.getCurrentStockQuantity(variantID);
            ProductVariant variant = variantRepository.findById(variantID).orElse(null);
            
            model.addAttribute("stockHistory", stockHistory);
            model.addAttribute("currentQuantity", currentQuantity);
            model.addAttribute("variant", variant);
            model.addAttribute("variantID", variantID);
        } else {
            Page<Stock> stockPage = inventoryService.getAllStockHistory(pageable);
            model.addAttribute("stockHistory", stockPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", stockPage.getTotalPages());
        }
        
        model.addAttribute("variants", variantRepository.findAll());
        
        return "admin/inventory";
    }

    @PostMapping("/import")
    public String importStock(@RequestParam Integer variantID,
                            @RequestParam Integer quantity,
                            @RequestParam(required = false) String note,
                            RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/inventory";
        }
        
        try {
            inventoryService.importStock(variantID, quantity, admin.getAdminID(), note);
            redirectAttributes.addFlashAttribute("success", "Stock imported successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error importing stock: " + e.getMessage());
        }
        
        return "redirect:/admin/inventory?variantID=" + variantID;
    }

    @PostMapping("/export")
    public String exportStock(@RequestParam Integer variantID,
                            @RequestParam Integer quantity,
                            @RequestParam(required = false) String note,
                            RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/inventory";
        }
        
        try {
            inventoryService.exportStock(variantID, quantity, admin.getAdminID(), note);
            redirectAttributes.addFlashAttribute("success", "Stock exported successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error exporting stock: " + e.getMessage());
        }
        
        return "redirect:/admin/inventory?variantID=" + variantID;
    }

    @PostMapping("/adjust")
    public String adjustStock(@RequestParam Integer variantID,
                             @RequestParam Integer newQuantity,
                             @RequestParam(required = false) String note,
                             RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/inventory";
        }
        
        try {
            inventoryService.adjustStock(variantID, newQuantity, admin.getAdminID(), note);
            redirectAttributes.addFlashAttribute("success", "Stock adjusted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adjusting stock: " + e.getMessage());
        }
        
        return "redirect:/admin/inventory?variantID=" + variantID;
    }
}

