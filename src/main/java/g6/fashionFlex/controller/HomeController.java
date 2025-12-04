package g6.fashionFlex.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import g6.fashionFlex.entity.Product;
import g6.fashionFlex.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController extends BaseController {  // CHỈ THÊM extends BaseController

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/")
    public String home(Model model, HttpSession session,  // CHỈ THÊM HttpSession session
                       @RequestParam(value = "category", required = false) String category,
                       @RequestParam(value = "categoryId", required = false) Integer categoryId,
                       @RequestParam(value = "search", required = false) String searchKeyword) {
        
        // CHỈ THÊM DÒNG NÀY
        addAuthenticationToModel(model, session);
        
        // GIỮ NGUYÊN LOGIC CŨ
        String normalizedSearch = (searchKeyword != null && !searchKeyword.trim().isEmpty())
                ? searchKeyword.trim()
                : null;

        Integer topId = null;
        if (normalizedSearch == null && categoryId != null && categoryId > 0) {
            topId = categoryId;
        }
        List<Integer> topIds = null;
        if (normalizedSearch == null && category != null && !category.isBlank()) {
            switch (category.toLowerCase(Locale.ROOT)) {
                case "men":
                    topId = 2;
                    break;
                case "women":
                    topId = 3;
                    break;
                case "kids":
                    topId = 4;
                    break;
                case "top":
                    topIds = List.of(5, 15, 21);
                    break;
                case "bottom":
                    topIds = List.of(6, 14, 21, 22);
                    break;
                case "shoes":
                    topIds = List.of(8, 9, 23);
                    break;
                case "accessory":
                case "accessories":
                    topId = 16;
                    break;
                default:
                    topId = null;
            }
        }

        List<Product> products;
        if (normalizedSearch != null) {
            products = productRepository.searchActiveProducts(normalizedSearch);
        } else if (topIds != null) {
            products = productRepository.findByTopLevelCategories(topIds);
        } else if (topId != null) {
            products = productRepository.findByTopLevelCategory(topId);
        } else {
            products = productRepository.findAllActive();
        }

        model.addAttribute("activeCategory", normalizedSearch == null ? category : null);
        model.addAttribute("activeCategoryId", normalizedSearch == null ? topId : null);
        model.addAttribute("searchKeyword", normalizedSearch);
        model.addAttribute("products", products);

        return "index";
    }

    @GetMapping("/index")
    public String index(Model model, HttpSession session,  // CHỈ THÊM HttpSession session
                        @RequestParam(value = "category", required = false) String category,
                        @RequestParam(value = "categoryId", required = false) Integer categoryId,
                        @RequestParam(value = "search", required = false) String searchKeyword) {
        return home(model, session, category, categoryId, searchKeyword);
    }

    @GetMapping("/home")
    public String homePage(Model model, HttpSession session,  // CHỈ THÊM HttpSession session
                           @RequestParam(value = "category", required = false) String category,
                           @RequestParam(value = "categoryId", required = false) Integer categoryId,
                           @RequestParam(value = "search", required = false) String searchKeyword) {
        return home(model, session, category, categoryId, searchKeyword);
    }

    @GetMapping("/home-02")
    public String home02(Model model, HttpSession session) {  
      
        addAuthenticationToModel(model, session);
        
        getAuthenticatedUser().ifPresent(user -> model.addAttribute("user", user));

        return "home-02";
    }

    @GetMapping("/home-03")
    public String home03(Model model, HttpSession session) {  
        addAuthenticationToModel(model, session);
        
        getAuthenticatedUser().ifPresent(user -> model.addAttribute("user", user));

        return "home-03";
    }

    @GetMapping("/about")
    public String about(Model model, HttpSession session) {  
        
        addAuthenticationToModel(model, session);
        return "about";
    }
}