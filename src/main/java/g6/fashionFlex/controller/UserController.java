package g6.fashionFlex.controller;

import g6.fashionFlex.dto.ChangePasswordRequest;
import g6.fashionFlex.dto.UpdateProfileRequest;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.service.FileUploadService;
import g6.fashionFlex.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * Get the current authenticated user
     */
    private UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            String email = authentication.getName();
            return userService.getUserByEmail(email);
        }
        return null;
    }

    /**
     * My Account page - Overview of user account
     */
    @GetMapping("/my-account")
    public String myAccount(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        return "user/my-account";
    }

    /**
     * Profile Edit page
     */
    @GetMapping("/profile-edit")
    public String profileEdit(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        return "user/profile-edit";
    }

    /**
     * Update user profile
     */
    @PostMapping("/profile/update")
    public String profileUpdate(@ModelAttribute UpdateProfileRequest request,
                                @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                                RedirectAttributes redirectAttributes) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return "redirect:/login";
            }

            // Handle profile image upload if provided
            if (profileImage != null && !profileImage.isEmpty()) {
                try {
                    String imageUrl = fileUploadService.uploadFile(profileImage, "profiles");
                    request.setProfileImageUrl(imageUrl);

                    // Delete old profile image if exists
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        fileUploadService.deleteFile(user.getProfileImageUrl());
                    }
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Error uploading profile image: " + e.getMessage());
                    return "redirect:/user/profile-edit";
                }
            }

            userService.updateProfile(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
            return "redirect:/user/my-account";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
            return "redirect:/user/profile-edit";
        }
    }

    /**
     * Change password
     */
    @PostMapping("/password/change")
    public String changePassword(@ModelAttribute ChangePasswordRequest request, RedirectAttributes redirectAttributes) {
        try {
            UserDTO user = getCurrentUser();
            if (user == null) {
                return "redirect:/login";
            }

            userService.changePassword(user.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully");
            return "redirect:/user/profile-edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/profile-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error changing password: " + e.getMessage());
            return "redirect:/user/profile-edit";
        }
    }

    /**
     * Order History page
     */
    @GetMapping("/order-history")
    public String orderHistory(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        // TODO: Add orders list when OrderService is implemented
        // List<Order> orders = orderService.getOrdersByUserId(user.getId());
        // model.addAttribute("orders", orders);
        return "user/order-history";
    }

    /**
     * Order Tracking page
     */
    @GetMapping("/order-tracking")
    public String orderTracking(@RequestParam(required = false) String orderNumber, Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);

        if (orderNumber != null) {
            // TODO: Fetch order details when OrderService is implemented
            // Order order = orderService.getOrderByNumber(orderNumber);
            // model.addAttribute("order", order);
            model.addAttribute("orderNumber", orderNumber);
        }

        return "user/order-tracking";
    }

    /**
     * Address Book page
     */
    @GetMapping("/address-book")
    public String addressBook(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        // TODO: Add addresses list when Address entity is implemented
        // List<Address> addresses = addressService.getAddressesByUserId(user.getId());
        // model.addAttribute("addresses", addresses);
        return "user/address-book";
    }

    /**
     * Wishlist page
     */
    @GetMapping("/wishlist")
    public String wishlist(Model model) {
        UserDTO user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        // TODO: Add wishlist items when Wishlist entity is implemented
        // List<Product> wishlistItems = wishlistService.getWishlistByUserId(user.getId());
        // model.addAttribute("wishlistItems", wishlistItems);
        return "user/wishlist";
    }
}
