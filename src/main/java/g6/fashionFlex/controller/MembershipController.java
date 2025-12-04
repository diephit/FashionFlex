package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.MembershipHistory;
import g6.fashionFlex.entity.MembershipLevel;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.MembershipHistoryRepository;
import g6.fashionFlex.repository.MembershipLevelRepository;
import g6.fashionFlex.service.MembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class MembershipController extends BaseController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private MembershipHistoryRepository membershipHistoryRepository;

    @Autowired
    private MembershipLevelRepository membershipLevelRepository;

    @GetMapping("/membership")
    public String membershipPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        Optional<User> userOpt = getAuthenticatedUser(auth);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        Optional<Customer> customerOpt = customerRepository.findByUser(user);
        if (customerOpt.isEmpty()) {
            return "redirect:/my-account";
        }

        Customer customer = customerOpt.get();
        
        // Get membership info
        MembershipLevel currentLevel = customer.getLevel();
        if (currentLevel == null) {
            // Get default Bronze level
            currentLevel = membershipLevelRepository.findByLevelName("Bronze")
                    .orElse(null);
        }

        MembershipLevel nextLevel = membershipService.getNextLevel(customer);
        double progress = membershipService.getProgressToNextLevel(customer);
        List<MembershipHistory> history = membershipHistoryRepository
                .findByCustomerOrderByChangedAtDesc(customer.getCustomerID());

        model.addAttribute("customer", customer);
        model.addAttribute("currentLevel", currentLevel);
        model.addAttribute("nextLevel", nextLevel);
        model.addAttribute("progress", progress);
        model.addAttribute("history", history);
        model.addAttribute("discountRate", currentLevel != null ? currentLevel.getDiscountRate() : 0);

        return "membership";
    }
}

