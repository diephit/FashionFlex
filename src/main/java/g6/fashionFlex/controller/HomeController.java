package g6.fashionFlex.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.service.UserService;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(value = "category", required = false) String category,
                       @RequestParam(value = "categoryId", required = false) Integer categoryId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() 
                && !authentication.getName().equals("anonymousUser")) {
            try {
                String email = authentication.getName();
                UserDTO user = userService.getUserByEmail(email);
                model.addAttribute("isAuthenticated", true);
                model.addAttribute("displayName", user.getFullName());
            } catch (Exception e) {
                model.addAttribute("isAuthenticated", false);
            }
        } else {
            model.addAttribute("isAuthenticated", false);
        }

        // Determine top-level category ID(s) from either categoryId (preferred) or category key
        Integer topId = (categoryId != null && categoryId > 0) ? categoryId : null;
        List<Integer> topIds = null;
        if (category != null && !category.isBlank()) {
            switch (category.toLowerCase(Locale.ROOT)) {
                case "men": // banner
                    topId = 2;
                    break;
                case "women": // banner
                    topId = 3;
                    break;
                case "kids": // reserved
                    topId = 4;
                    break;
                case "top": // filter button: Men Tops (5), Skirts (15), Kids Tops (21)
                    topIds = List.of(5, 15, 21);
                    break;
                case "bottom": // filter button: Men Bottoms (6), Gowns (14), Kids Tops (21), Kids Bottoms (22)
                    topIds = List.of(6, 14, 21, 22);
                    break;
                case "shoes": // filter button: Men, Women, Kids shoes
                    topIds = List.of(8, 9, 23);
                    break;
                case "accessory":
                case "accessories":
                    topId = 16; // Accessories
                    break;
                default:
                    topId = null;
            }
        }

        List<Product> products;
        if (topIds != null) {
            products = productRepository.findByTopLevelCategories(topIds);
        } else if (topId != null) {
            products = productRepository.findByTopLevelCategory(topId);
        } else {
            products = productRepository.findAll();
        }

        model.addAttribute("activeCategory", category);
        model.addAttribute("activeCategoryId", topId);
        model.addAttribute("products", products);

        return "index";
    }

    @GetMapping("/index")
    public String index(Model model,
                        @RequestParam(value = "category", required = false) String category,
                        @RequestParam(value = "categoryId", required = false) Integer categoryId) {
        return home(model, category, categoryId);
    }

    @GetMapping("/home")
    public String homePage(Model model,
                           @RequestParam(value = "category", required = false) String category,
                           @RequestParam(value = "categoryId", required = false) Integer categoryId) {
        return home(model, category, categoryId);
    }

    @GetMapping("/home-02")
    public String home02(Model model) {
        // Check if user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            try {
                String email = authentication.getName();
                UserDTO user = userService.getUserByEmail(email);
                model.addAttribute("user", user);
                model.addAttribute("isAuthenticated", true);
            } catch (Exception e) {
                model.addAttribute("isAuthenticated", false);
            }
        } else {
            model.addAttribute("isAuthenticated", false);
        }

        return "home-02";
    }

    @GetMapping("/home-03")
    public String home03(Model model) {
        // Check if user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            try {
                String email = authentication.getName();
                UserDTO user = userService.getUserByEmail(email);
                model.addAttribute("user", user);
                model.addAttribute("isAuthenticated", true);
            } catch (Exception e) {
                model.addAttribute("isAuthenticated", false);
            }
        } else {
            model.addAttribute("isAuthenticated", false);
        }

        return "home-03";
    }

    @GetMapping("/about")
    public String about(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            try {
                String email = authentication.getName();
                UserDTO user = userService.getUserByEmail(email);
                model.addAttribute("isAuthenticated", true);
                model.addAttribute("displayName", user.getFullName());
            } catch (Exception e) {
                model.addAttribute("isAuthenticated", false);
            }
        } else {
            model.addAttribute("isAuthenticated", false);
        }

        return "about";
    }
}
