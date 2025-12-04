package g6.fashionFlex.controller;

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

@Controller
@RequestMapping("/admin/customers")
public class AdminUserController {

    @Autowired
    private AdminUserService userService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userID") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> users = (keyword != null && !keyword.trim().isEmpty())
                ? userService.searchUsers(keyword, pageable)
                : userService.getAllUsers(pageable);
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalUsers", userService.countAllUsers());
        model.addAttribute("activeUsers", userService.countActiveUsers());
        model.addAttribute("inactiveUsers", userService.countInactiveUsers());
        model.addAttribute("newUsersThisMonth", userService.countNewUsersThisMonth());
        
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
        
        return "redirect:/admin/customers";
    }

    @PostMapping("/create-admin")
    public String createAdmin(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              RedirectAttributes redirectAttributes) {
        try {
            userService.createAdmin(email, password);
            redirectAttributes.addFlashAttribute("success", "Admin account created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating admin: " + e.getMessage());
        }
        return "redirect:/admin/customers";
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
        
        return "redirect:/admin/customers";
    }
}

