package g6.fashionFlex.controller;

import g6.fashionFlex.dto.CategoryDTO;
import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.service.AdminCategoryService;
import g6.fashionFlex.service.ProductService;
import g6.fashionFlex.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String IS_AUTHENTICATED = "isAuthenticated";

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private AdminCategoryService adminCategoryService;

    @GetMapping("/")
    public String home(Model model) {
        addUserAuthenticationData(model);

        // Get products for homepage
        List<Product> featuredProducts = productService.getFeaturedProducts();
        List<Product> latestProducts = productService.getLatestProducts(16);

        // Get active categories
        List<CategoryDTO> categories = adminCategoryService.findAllByActiveTrue();

        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("products", latestProducts);
        model.addAttribute("categories", categories);

        return "index";
    }

    @GetMapping("/index")
    public String index(Model model) {
        return home(model);
    }

    @GetMapping("/home")
    public String homePage(Model model) {
        return home(model);
    }

    @GetMapping("/home-02")
    public String home02(Model model) {
        addUserAuthenticationData(model);
        return "home-02";
    }

    @GetMapping("/home-03")
    public String home03(Model model) {
        addUserAuthenticationData(model);
        return "home-03";
    }

    @GetMapping("/about")
    public String about(Model model) {
        addUserAuthenticationData(model);
        return "about";
    }

    @GetMapping("/blog")
    public String blog(Model model) {
        addUserAuthenticationData(model);
        return "blog";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        addUserAuthenticationData(model);
        model.addAttribute("contactDTO", new g6.fashionFlex.dto.ContactDTO());
        return "contact";
    }

    @GetMapping("/blog-detail")
    public String blogDetail(Model model) {
        addUserAuthenticationData(model);
        return "blog-detail";
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
