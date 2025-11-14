package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private AdminUserService userService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userID") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> users = userService.getAllUsers(pageable);
        List<Role> roles = userService.getAllRoles();
        
        model.addAttribute("users", users);
        model.addAttribute("roles", roles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        return "admin/customers";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Integer id,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("success", "User status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String assignRole(@PathVariable Integer id,
                            @RequestParam Integer roleID,
                            RedirectAttributes redirectAttributes) {
        try {
            userService.assignRole(id, roleID);
            redirectAttributes.addFlashAttribute("success", "Role assigned successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error assigning role: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/remove-role")
    public String removeRole(@PathVariable Integer id,
                            RedirectAttributes redirectAttributes) {
        try {
            userService.removeRole(id);
            redirectAttributes.addFlashAttribute("success", "Role removed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error removing role: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Integer id,
                            RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
}

