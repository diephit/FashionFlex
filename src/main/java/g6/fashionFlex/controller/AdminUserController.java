package g6.fashionFlex.controller;

import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.service.AdminUserService;
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
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private AdminUserService userService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserDTO> userPage = userService.getAllUsers(pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", userPage.getNumber());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "admin/users/list";
    }

    @GetMapping("/view/{id}")
    public String viewUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            UserDTO user = userService.getUserById(id);
            model.addAttribute("user", user);
            return "admin/users/view";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "User not found: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/toggle/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("success", "User status updated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user status: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/assign-role/{id}")
    public String assignRole(
            @PathVariable Long id,
            @RequestParam String roleName,
            RedirectAttributes redirectAttributes) {
        try {
            userService.assignRole(id, roleName);
            redirectAttributes.addFlashAttribute("success", "Role assigned successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error assigning role: " + e.getMessage());
        }
        return "redirect:/admin/users/view/" + id;
    }

    @GetMapping("/remove-role/{userId}/{roleName}")
    public String removeRole(
            @PathVariable Long userId,
            @PathVariable String roleName,
            RedirectAttributes redirectAttributes) {
        try {
            userService.removeRole(userId, roleName);
            redirectAttributes.addFlashAttribute("success", "Role removed successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error removing role: " + e.getMessage());
        }
        return "redirect:/admin/users/view/" + userId;
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
