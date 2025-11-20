package g6.fashionFlex.controller;

import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/info")
public class InfoController {

    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String IS_AUTHENTICATED = "isAuthenticated";

    @Autowired
    private UserService userService;

    @GetMapping("/faq")
    public String faq(Model model) {
        addUserAuthenticationData(model);
        return "info/faq";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        addUserAuthenticationData(model);
        return "info/privacy-policy";
    }

    @GetMapping("/terms-conditions")
    public String termsConditions(Model model) {
        addUserAuthenticationData(model);
        return "info/terms-conditions";
    }

    @GetMapping("/shipping-policy")
    public String shippingPolicy(Model model) {
        addUserAuthenticationData(model);
        return "info/shipping-policy";
    }

    @GetMapping("/return-policy")
    public String returnPolicy(Model model) {
        addUserAuthenticationData(model);
        return "info/return-policy";
    }

    @GetMapping("/size-guide")
    public String sizeGuide(Model model) {
        addUserAuthenticationData(model);
        return "info/size-guide";
    }

    /**
     * Helper method to add user authentication data to model
     * @param model the model to add data to
     */
    private void addUserAuthenticationData(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals(ANONYMOUS_USER)) {
            try {
                String email = authentication.getName();
                UserDTO user = userService.getUserByEmail(email);
                model.addAttribute("user", user);
                model.addAttribute(IS_AUTHENTICATED, true);
            } catch (Exception e) {
                model.addAttribute(IS_AUTHENTICATED, false);
            }
        } else {
            model.addAttribute(IS_AUTHENTICATED, false);
        }
    }
}