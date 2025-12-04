package g6.fashionFlex.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpSession;

@Controller
public class BlogController extends BaseController {  

    @GetMapping("/blog")
    public String blog(Model model, HttpSession session) {  
        addAuthenticationToModel(model, session);  
        return "blog";
    }

    @GetMapping("/blog-detail")
    public String blogDetail(Model model, HttpSession session) {  
        addAuthenticationToModel(model, session);  
        return "blog-detail";
    }

    @GetMapping("/blog-detail/{id}")
    public String blogDetailById(@PathVariable(required = false) String id, Model model, HttpSession session) {  // CHỈ THÊM HttpSession session
        addAuthenticationToModel(model, session);  
        return "blog-detail";
    }
}