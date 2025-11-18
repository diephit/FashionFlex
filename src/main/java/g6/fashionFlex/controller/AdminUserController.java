package g6.fashionFlex.controller;

import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserDTO> userPage;

        // Use advanced search if filters are provided
        if (keyword != null || role != null || enabled != null) {
            userPage = userService.searchUsersAdvanced(keyword, role, enabled, pageable);
        } else {
            userPage = userService.getAllUsers(pageable);
        }

        // Add statistics
        AdminUserService.UserStatistics statistics = userService.getStatistics();
        model.addAttribute("statistics", statistics);

        // Add filter options (common roles)
        model.addAttribute("availableRoles", new String[]{"ROLE_USER", "ROLE_ADMIN"});

        // Add current filters to model for persistence
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("enabled", enabled);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", userPage.getNumber());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "admin/users/list";
    }

    @GetMapping("/form/{id}")
    public String userForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            UserDTO user = userService.getUserById(id);
            model.addAttribute("user", user);
            return "admin/users/form";
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
        return "redirect:/admin/users/form/" + id;
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
        return "redirect:/admin/users/form/" + userId;
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

    // Bulk Actions
    @PostMapping("/bulk/activate")
    @ResponseBody
    public ResponseEntity<?> bulkActivate(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = userService.bulkActivate(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " users activated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk/deactivate")
    @ResponseBody
    public ResponseEntity<?> bulkDeactivate(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = userService.bulkDeactivate(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " users deactivated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk/delete")
    @ResponseBody
    public ResponseEntity<?> bulkDelete(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = userService.bulkDelete(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " users deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
