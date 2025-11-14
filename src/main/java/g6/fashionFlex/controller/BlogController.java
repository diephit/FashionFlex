package g6.fashionFlex.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.service.UserService;

@Controller
public class BlogController {

    @Autowired
    private UserService userService;

    private void addUserInfoToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            try {
                UserDTO user = userService.getUserByEmail(authentication.getName());
                model.addAttribute("isAuthenticated", true);
                model.addAttribute("displayName", user.getFullName());
            } catch (Exception e) {
                model.addAttribute("isAuthenticated", false);
            }
        } else {
            model.addAttribute("isAuthenticated", false);
        }
    }

    @GetMapping("/blog")
    public String blog(Model model) {
        addUserInfoToModel(model);
        return "blog";
    }

    @GetMapping("/blog-detail")
    public String blogDetail(Model model) {
        addUserInfoToModel(model);
        return "blog-detail";
    }

    @GetMapping("/blog-detail/{id}")
    public String blogDetailById(@PathVariable(required = false) String id, Model model) {
        addUserInfoToModel(model);
        return "blog-detail";
    }
}

